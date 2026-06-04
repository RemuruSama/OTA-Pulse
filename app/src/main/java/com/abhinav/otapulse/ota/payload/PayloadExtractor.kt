package com.abhinav.otapulse.ota.payload

import chromeos_update_engine.UpdateMetadata.DeltaArchiveManifest
import chromeos_update_engine.UpdateMetadata.InstallOperation
import com.abhinav.otapulse.ota.network.RangeHttpClient
import com.abhinav.otapulse.ota.resume.ExtractionState
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.tukaani.xz.XZInputStream
import java.io.ByteArrayInputStream
import java.io.OutputStream
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * The core engine for decompressing and writing partition data.
 * Supports REPLACE, REPLACE_BZ, and REPLACE_XZ operations.
 */
class PayloadExtractor(
    private val url: String,
    private val http: RangeHttpClient,
    private val header: PayloadHeader,
    private val blockSize: Long = 4096L
) {
    /**
     * Extract [partitionName] into [output].
     *
     * Supports all full OTA operation types:
     *   REPLACE, REPLACE_BZ, REPLACE_XZ
     *
     * Pass a non-null [resumeState] to skip already-written operations.
     * [onProgress] is called after every operation with (done, total, opIndex).
     */
    suspend fun extract(
        partitionName: String,
        manifest: DeltaArchiveManifest,
        output: OutputStream,
        resumeState: ExtractionState? = null,
        onProgress: suspend (done: Long, total: Long, opIndex: Int) -> Unit = { _, _, _ -> }
    ): ExtractionState {
        val partition = manifest.partitionsList
            .firstOrNull { it.partitionName == partitionName }
            ?: error("Partition '$partitionName' not found in manifest.")

        val totalBytes   = partition.newPartitionInfo.size
        val startOpIdx   = resumeState?.lastCompletedOpIndex?.plus(1) ?: 0
        var writtenBytes = resumeState?.bytesWritten ?: 0L

        val ops = partition.operationsList
        var i = startOpIdx
        while (i < ops.size) {
            currentCoroutineContext().ensureActive()
            val firstOp = ops[i]

            // Handle non-blob operations (ZERO, DISCARD, MOVE)
            if (firstOp.type == InstallOperation.Type.ZERO) {
                val zeroBytes = firstOp.dstExtentsList.sumOf { it.numBlocks } * blockSize
                if (zeroBytes > 0) {
                    val buffer = ByteArray(blockSize.toInt())
                    var remaining = zeroBytes
                    while (remaining > 0) {
                        currentCoroutineContext().ensureActive()
                        val toWrite = minOf(remaining, blockSize).toInt()
                        output.write(buffer, 0, toWrite)
                        remaining -= toWrite
                    }
                    writtenBytes += zeroBytes
                }
                onProgress(writtenBytes, totalBytes, i)
                i++
                continue
            }
            if (firstOp.type == InstallOperation.Type.DISCARD || 
                firstOp.type == InstallOperation.Type.MOVE || 
                firstOp.dataLength == 0L) {
                onProgress(writtenBytes, totalBytes, i)
                i++
                continue
            }

            // Coalesce contiguous operations into a bundle
            val bundleOps = mutableListOf<InstallOperation>()
            bundleOps.add(firstOp)
            
            val bundleStart = header.blobOffset + firstOp.dataOffset
            var bundleLength = firstOp.dataLength
            val maxBundleSize = 4 * 1024 * 1024L // 4MB

            var nextIdx = i + 1
            while (nextIdx < ops.size) {
                currentCoroutineContext().ensureActive()
                val nextOp = ops[nextIdx]
                
                // Only coalesce if contiguous AND it's a blob-based op AND doesn't exceed max size
                val isContiguous = nextOp.dataOffset == (ops[nextIdx - 1].dataOffset + ops[nextIdx - 1].dataLength)
                val isBlobOp = nextOp.dataLength > 0 && 
                               nextOp.type != InstallOperation.Type.ZERO && 
                               nextOp.type != InstallOperation.Type.DISCARD && 
                               nextOp.type != InstallOperation.Type.MOVE
                
                if (isContiguous && isBlobOp && (bundleLength + nextOp.dataLength <= maxBundleSize)) {
                    bundleOps.add(nextOp)
                    bundleLength += nextOp.dataLength
                    nextIdx++
                } else {
                    break
                }
            }

            // Fetch the aggregated bundle
            val bundleEnd = bundleStart + bundleLength - 1
            require(bundleLength <= Int.MAX_VALUE) { "Bundle size exceeds maximum supported buffer size" }
            val bundleData = http.fetchBytes(url, bundleStart, bundleEnd)

            // Process each op in the bundle using the shared buffer
            var offsetInBundle = 0
            for ((index, op) in bundleOps.withIndex()) {
                currentCoroutineContext().ensureActive()
                val opData = bundleData.copyOfRange(offsetInBundle, offsetInBundle + op.dataLength.toInt())
                val raw = decompress(opData, op, partitionName)
                output.write(raw)
                writtenBytes += raw.size
                
                offsetInBundle += op.dataLength.toInt()
                onProgress(writtenBytes, totalBytes, i + index)
            }

            // Move pointer to the operation after the bundle
            i += bundleOps.size
        }

        output.flush()
        return ExtractionState(
            partitionName        = partitionName,
            lastCompletedOpIndex = ops.size - 1,
            bytesWritten         = writtenBytes,
            totalBytes           = totalBytes
        )
    }

    private fun decompress(data: ByteArray, op: InstallOperation, partitionName: String): ByteArray {
        val stream = ByteArrayInputStream(data)
        return when (op.type) {
            InstallOperation.Type.REPLACE ->
                data

            InstallOperation.Type.REPLACE_BZ ->
                BZip2CompressorInputStream(stream).use { it.readBytes() }

            InstallOperation.Type.REPLACE_XZ ->
                XZInputStream(stream).use { it.readBytes() }

            InstallOperation.Type.ZERO,
            InstallOperation.Type.DISCARD,
            InstallOperation.Type.MOVE ->
                ByteArray(0)

            else ->
                error(
                    "Operation type '${op.type}' in partition '$partitionName' requires a source partition. " +
                    "This usually means the OTA is incremental (delta). Please use a full OTA package."
                )
        }
    }
}
