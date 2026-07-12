package com.abhinav.otapulse.ota.payload

import chromeos_update_engine.UpdateMetadata.DeltaArchiveManifest
import chromeos_update_engine.UpdateMetadata.InstallOperation
import com.abhinav.otapulse.ota.network.RangeHttpClient
import com.abhinav.otapulse.ota.resume.ExtractionState
import com.abhinav.otapulse.ota.utils.BsDiffApplier
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.brotli.BrotliCompressorInputStream
import org.tukaani.xz.XZInputStream
import java.io.ByteArrayInputStream
import java.io.OutputStream
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Interface for reading data from a source partition during delta OTA extraction.
 */
interface SourceReader {
    /**
     * Read data from the source partition into a [ByteArray].
     * [startBlock] and [numBlocks] are in [blockSize] units.
     */
    fun readBlocks(startBlock: Long, numBlocks: Long, blockSize: Long): ByteArray
}

/**
 * The core engine for decompressing and writing partition data.
 * Supports REPLACE, REPLACE_BZ, REPLACE_XZ, SOURCE_COPY, SOURCE_BSDIFF, and MOVE operations.
 */
class PayloadExtractor(
    private val url: String,
    private val http: RangeHttpClient,
    private val header: PayloadHeader,
    private val blockSize: Long = 4096L,
    private val sourceReader: SourceReader? = null
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
            val op = ops[i]

            // 1. Handle non-blob or source-only operations
            if (op.type == InstallOperation.Type.ZERO) {
                val zeroBytes = op.dstExtentsList.sumOf { it.numBlocks } * blockSize
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

            if (op.type == InstallOperation.Type.SOURCE_COPY) {
                val reader = sourceReader ?: error("SOURCE_COPY requires a source partition reader.")
                op.srcExtentsList.forEach { extent ->
                    val data = reader.readBlocks(extent.startBlock, extent.numBlocks, blockSize)
                    output.write(data)
                    writtenBytes += data.size
                }
                onProgress(writtenBytes, totalBytes, i)
                i++
                continue
            }

            if (op.type == InstallOperation.Type.MOVE) {
                // MOVE is typically used within the same partition, but for simple extraction we treat it as COPY if source is provided
                // Historically MOVE used src_extents in the same partition.
                val reader = sourceReader ?: error("MOVE requires a source partition reader.")
                op.srcExtentsList.forEach { extent ->
                    val data = reader.readBlocks(extent.startBlock, extent.numBlocks, blockSize)
                    output.write(data)
                    writtenBytes += data.size
                }
                onProgress(writtenBytes, totalBytes, i)
                i++
                continue
            }

            if (op.type == InstallOperation.Type.DISCARD || op.dataLength == 0L) {
                onProgress(writtenBytes, totalBytes, i)
                i++
                continue
            }

            // 2. Handle blob-based operations (REPLACE*, SOURCE_BSDIFF)
            // Coalesce contiguous operations into a bundle
            val bundleOps = mutableListOf<InstallOperation>()
            bundleOps.add(op)
            
            val bundleStart = header.blobOffset + op.dataOffset
            var bundleLength = op.dataLength
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
                               nextOp.type != InstallOperation.Type.MOVE &&
                               nextOp.type != InstallOperation.Type.SOURCE_COPY
                
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
            for ((index, bOp) in bundleOps.withIndex()) {
                currentCoroutineContext().ensureActive()
                val opData = bundleData.copyOfRange(offsetInBundle, offsetInBundle + bOp.dataLength.toInt())
                val raw = processOperation(opData, bOp, partitionName)
                output.write(raw)
                writtenBytes += raw.size
                
                offsetInBundle += bOp.dataLength.toInt()
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

    private fun processOperation(data: ByteArray, op: InstallOperation, partitionName: String): ByteArray {
        val stream = ByteArrayInputStream(data)
        return when (op.type) {
            InstallOperation.Type.REPLACE ->
                data

            InstallOperation.Type.REPLACE_BZ ->
                BZip2CompressorInputStream(stream).use { it.readBytes() }

            InstallOperation.Type.REPLACE_XZ ->
                XZInputStream(stream).use { it.readBytes() }

            InstallOperation.Type.SOURCE_BSDIFF,
            InstallOperation.Type.BROTLI_BSDIFF -> {
                val reader = sourceReader ?: error("${op.type} requires a source partition reader.")
                val oldData = op.srcExtentsList.fold(ByteArray(0)) { acc, extent ->
                    acc + reader.readBlocks(extent.startBlock, extent.numBlocks, blockSize)
                }.copyOfRange(0, op.srcLength.toInt())
                
                val patch = if (op.type == InstallOperation.Type.BROTLI_BSDIFF) {
                    BrotliCompressorInputStream(stream).use { it.readBytes() }
                } else {
                    data
                }
                BsDiffApplier.applyPatch(oldData, patch)
            }

            InstallOperation.Type.ZERO,
            InstallOperation.Type.DISCARD,
            InstallOperation.Type.MOVE,
            InstallOperation.Type.SOURCE_COPY ->
                ByteArray(0)

            else ->
                error(
                    "Operation type '${op.type}' in partition '$partitionName' is not supported. " +
                    "This usually means the OTA uses an advanced delta format (like PUFFDIFF or ZUCCHINI)."
                )
        }
    }
}
