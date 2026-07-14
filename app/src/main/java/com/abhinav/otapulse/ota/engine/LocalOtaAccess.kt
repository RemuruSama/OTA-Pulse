package com.abhinav.otapulse.ota.engine

import android.content.Context
import android.net.Uri
import chromeos_update_engine.UpdateMetadata.DeltaArchiveManifest
import chromeos_update_engine.UpdateMetadata.InstallOperation
import com.abhinav.otapulse.ota.payload.PayloadExtractor
import com.abhinav.otapulse.ota.payload.PayloadHeader
import com.abhinav.otapulse.ota.payload.SourceReader
import com.abhinav.otapulse.ota.resume.ExtractionState
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.brotli.BrotliCompressorInputStream
import org.tukaani.xz.XZInputStream
import java.io.ByteArrayInputStream
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal interface LocalByteReader : Closeable {
    val fileSize: Long
    fun fetchBytes(start: Long, endInclusive: Long): ByteArray
}

internal class LocalRangeReader(
    context: Context,
    uri: Uri
) : LocalByteReader {

    private val parcelFileDescriptor = requireNotNull(
        context.contentResolver.openFileDescriptor(uri, "r")
    ) { "Unable to open local OTA file." }

    private val inputStream = FileInputStream(parcelFileDescriptor.fileDescriptor)
    private val channel: FileChannel = inputStream.channel

    override val fileSize: Long
        get() = channel.size()

    override fun fetchBytes(start: Long, endInclusive: Long): ByteArray {
        require(start >= 0L && endInclusive >= start) { "Invalid range: $start-$endInclusive" }
        val lengthLong = endInclusive - start + 1L
        require(lengthLong <= Int.MAX_VALUE) { "Requested range exceeds maximum buffer size" }
        val length = lengthLong.toInt()
        val buffer = ByteBuffer.allocate(length)
        // Positional read (does not touch the channel's shared cursor) so this is safe to
        // call concurrently from a prefetch coroutine while the main loop is decompressing
        // a previous bundle - required for LocalPayloadExtractor's read-ahead pipeline.
        var position = start
        while (buffer.hasRemaining()) {
            val read = channel.read(buffer, position)
            if (read <= 0) error("Unexpected EOF while reading local OTA data.")
            position += read
        }
        return buffer.array()
    }

    override fun close() {
        inputStream.close()
        parcelFileDescriptor.close()
    }
}

internal class LocalFileReader(
    file: File
) : LocalByteReader {
    private val inputStream = FileInputStream(file)
    private val channel: FileChannel = inputStream.channel

    override val fileSize: Long
        get() = channel.size()

    override fun fetchBytes(start: Long, endInclusive: Long): ByteArray {
        require(start >= 0L && endInclusive >= start) { "Invalid range: $start-$endInclusive" }
        val lengthLong = endInclusive - start + 1L
        require(lengthLong <= Int.MAX_VALUE) { "Requested range exceeds maximum buffer size" }
        val length = lengthLong.toInt()
        val buffer = ByteBuffer.allocate(length)
        var position = start
        while (buffer.hasRemaining()) {
            val read = channel.read(buffer, position)
            if (read <= 0) error("Unexpected EOF while reading cached payload data.")
            position += read
        }
        return buffer.array()
    }

    override fun close() {
        inputStream.close()
    }
}

internal class OffsetLocalByteReader(
    private val delegate: LocalByteReader,
    private val offset: Long,
    override val fileSize: Long
) : LocalByteReader {

    override fun fetchBytes(start: Long, endInclusive: Long): ByteArray {
        return delegate.fetchBytes(start + offset, endInclusive + offset)
    }

    override fun close() {
        delegate.close()
    }
}

internal data class LocalZipEntry(
    val name: String,
    val localHeaderOffset: Long,
    val compressedSize: Long,
    val uncompressedSize: Long,
    /**
     * ZIP compression method from the central directory:
     *   0 = STORED (no compression) — payload.bin in all valid full OTA ZIPs
     *   8 = DEFLATE
     */
    val compressionMethod: Int
)

internal class LocalZipParser(
    private val reader: LocalByteReader
) {
    companion object {
        private val EOCD_SIG   = byteArrayOf(0x50, 0x4B, 0x05, 0x06)
        private val EOCD64_SIG = byteArrayOf(0x50, 0x4B, 0x06, 0x06)
        private val LOC64_SIG  = byteArrayOf(0x50, 0x4B, 0x06, 0x07)
        private val CD_SIG     = byteArrayOf(0x50, 0x4B, 0x01, 0x02)
        private const val TAIL_SIZE = 65_556L
    }

    fun findEntry(name: String): LocalZipEntry {
        val fileSize  = reader.fileSize
        val tailStart = maxOf(0L, fileSize - TAIL_SIZE)
        val tail      = reader.fetchBytes(tailStart, fileSize - 1)
        val eocdPos   = findSigFromEnd(tail, EOCD_SIG)
            ?: error("No EOCD signature found. The selected file is not a valid ZIP.")

        val buf        = ByteBuffer.wrap(tail).order(ByteOrder.LITTLE_ENDIAN)
        val cdSizeRaw  = buf.getInt(eocdPos + 12).toUInt().toLong()
        val cdOffsetRaw = buf.getInt(eocdPos + 16).toUInt().toLong()

        val loc64Pos = eocdPos - 20
        val isZip64  = loc64Pos >= 0 &&
                tail.sliceArray(loc64Pos until loc64Pos + 4).contentEquals(LOC64_SIG)

        val (cdSize, cdOffset) = if (isZip64) {
            resolveZip64CD(tail, tailStart, eocdPos)
        } else {
            cdSizeRaw to cdOffsetRaw
        }

        val cd = reader.fetchBytes(cdOffset, cdOffset + cdSize - 1)
        return parseCentralDirectory(cd, name)
            ?: error("'$name' not found in the selected OTA ZIP.")
    }

    fun resolveDataOffset(entry: LocalZipEntry): Long {
        // Local file header: bytes 26-27 = filename length, bytes 28-29 = extra field length
        val localHeader = reader.fetchBytes(entry.localHeaderOffset, entry.localHeaderOffset + 29)
        val filenameLen = localHeader.leUShort(26)
        val extraLen    = localHeader.leUShort(28)
        return entry.localHeaderOffset + 30 + filenameLen + extraLen
    }

    private fun resolveZip64CD(
        tail: ByteArray,
        tailStart: Long,
        eocdPos: Int
    ): Pair<Long, Long> {
        val loc64Pos = eocdPos - 20
        require(loc64Pos >= 0) { "ZIP64 locator is outside the readable tail window." }
        require(tail.sliceArray(loc64Pos until loc64Pos + 4).contentEquals(LOC64_SIG)) {
            "ZIP64 locator signature mismatch."
        }

        val buf         = ByteBuffer.wrap(tail).order(ByteOrder.LITTLE_ENDIAN)
        val eocd64Offset = buf.getLong(loc64Pos + 8)
        val eocd64      = reader.fetchBytes(eocd64Offset, eocd64Offset + 55)
        require(eocd64.sliceArray(0..3).contentEquals(EOCD64_SIG)) {
            "ZIP64 EOCD signature mismatch at offset ${tailStart + loc64Pos}."
        }
        val eocd64Buf = ByteBuffer.wrap(eocd64).order(ByteOrder.LITTLE_ENDIAN)
        return eocd64Buf.getLong(40) to eocd64Buf.getLong(48)
    }

    private fun parseCentralDirectory(cd: ByteArray, targetName: String): LocalZipEntry? {
        // Use a single wrap so absolute-index reads (buf.getInt/getShort) always
        // address from cd[0]. Every field access must add `pos` explicitly.
        val buf = ByteBuffer.wrap(cd).order(ByteOrder.LITTLE_ENDIAN)
        var pos = 0

        while (pos <= cd.size - 46) {
            if (!cd.sliceArray(pos until pos + 4).contentEquals(CD_SIG)) {
                pos++
                continue
            }

            // Central directory entry field offsets (relative to the start of each entry):
            //   +10  compression method (2 bytes)
            //   +20  compressed size   (4 bytes)
            //   +24  uncompressed size (4 bytes)
            //   +28  filename length   (2 bytes)
            //   +30  extra field len   (2 bytes)
            //   +32  file comment len  (2 bytes)
            //   +42  local header offset (4 bytes)
            //   +46  filename          (variable)
            val compressionMethod = buf.getShort(pos + 10).toInt() and 0xFFFF
            var compressedSize    = buf.getInt(pos + 20).toUInt().toLong()
            var uncompressedSize  = buf.getInt(pos + 24).toUInt().toLong()
            val fileNameLen       = buf.getShort(pos + 28).toInt() and 0xFFFF
            val extraLen          = buf.getShort(pos + 30).toInt() and 0xFFFF
            val commentLen        = buf.getShort(pos + 32).toInt() and 0xFFFF
            var localOffset       = buf.getInt(pos + 42).toUInt().toLong()
            val entryName         = String(cd, pos + 46, fileNameLen, Charsets.UTF_8)

            // Resolve ZIP64 extended fields if sizes/offset use the 0xFFFFFFFF sentinel
            val uncompressedMasked = uncompressedSize == 0xFFFF_FFFFL
            val compressedMasked   = compressedSize   == 0xFFFF_FFFFL
            val offsetMasked       = localOffset      == 0xFFFF_FFFFL

            if (uncompressedMasked || compressedMasked || offsetMasked) {
                val extraStart = pos + 46 + fileNameLen
                val extra      = cd.sliceArray(extraStart until extraStart + extraLen)
                parseZip64Extra(extra, uncompressedMasked, compressedMasked, offsetMasked)?.let { zip64 ->
                    if (uncompressedMasked && zip64.uncompressedSize    != -1L) uncompressedSize = zip64.uncompressedSize
                    if (compressedMasked   && zip64.compressedSize      != -1L) compressedSize   = zip64.compressedSize
                    if (offsetMasked       && zip64.localHeaderOffset   != -1L) localOffset      = zip64.localHeaderOffset
                }
            }

            if (entryName == targetName) {
                return LocalZipEntry(
                    name              = entryName,
                    localHeaderOffset = localOffset,
                    compressedSize    = compressedSize,
                    uncompressedSize  = uncompressedSize,
                    compressionMethod = compressionMethod
                )
            }
            pos += 46 + fileNameLen + extraLen + commentLen
        }
        return null
    }

    private data class Zip64Extra(
        val uncompressedSize: Long,
        val compressedSize: Long,
        val localHeaderOffset: Long
    )

    private fun parseZip64Extra(
        extra: ByteArray,
        uncompressedMasked: Boolean,
        compressedMasked: Boolean,
        offsetMasked: Boolean
    ): Zip64Extra? {
        val buf = ByteBuffer.wrap(extra).order(ByteOrder.LITTLE_ENDIAN)
        var i = 0
        while (i + 4 <= extra.size) {
            val headerId = buf.getShort(i).toInt() and 0xFFFF
            val dataSize = buf.getShort(i + 2).toInt() and 0xFFFF
            if (headerId == 0x0001 && i + 4 + dataSize <= extra.size) {
                var ptr = i + 4
                var uncompressedSize  = -1L
                var compressedSize    = -1L
                var localHeaderOffset = -1L

                if (uncompressedMasked && ptr + 8 <= i + 4 + dataSize) {
                    uncompressedSize = buf.getLong(ptr); ptr += 8
                }
                if (compressedMasked && ptr + 8 <= i + 4 + dataSize) {
                    compressedSize = buf.getLong(ptr); ptr += 8
                }
                if (offsetMasked && ptr + 8 <= i + 4 + dataSize) {
                    localHeaderOffset = buf.getLong(ptr)
                }

                return Zip64Extra(uncompressedSize, compressedSize, localHeaderOffset)
            }
            i += 4 + dataSize
        }
        return null
    }

    private fun findSigFromEnd(data: ByteArray, sig: ByteArray): Int? {
        if (data.size < sig.size) return null
        for (i in data.size - sig.size downTo 0) {
            var match = true
            for (j in sig.indices) {
                if (data[i + j] != sig[j]) { match = false; break }
            }
            if (match) return i
        }
        return null
    }

    private fun ByteArray.leUShort(offset: Int): Int =
        ((this[offset + 1].toInt() and 0xFF) shl 8) or (this[offset].toInt() and 0xFF)
}

// ─────────────────────────────────────────────────────────────────────────────

internal class LocalPayloadManifest(
    private val reader: LocalByteReader,
    private val payloadDataOffset: Long
) {
    companion object {
        private val MAGIC            = "CrAU".toByteArray(Charsets.US_ASCII)
        private const val FIXED_HEADER_SIZE = 24L
    }

    fun readHeader(): PayloadHeader {
        val headerBytes = reader.fetchBytes(payloadDataOffset, payloadDataOffset + FIXED_HEADER_SIZE - 1)
        val magic = headerBytes.sliceArray(0..3)
        require(magic.contentEquals(MAGIC)) {
            "Invalid payload.bin magic. The selected file is not a supported OTA payload (CrAU magic missing)."
        }

        val buffer       = ByteBuffer.wrap(headerBytes).order(ByteOrder.BIG_ENDIAN)
        val version      = buffer.getLong(4)
        val manifestSize = buffer.getLong(12)
        val signatureSize = buffer.getInt(20)
        val blobOffset   = payloadDataOffset + FIXED_HEADER_SIZE + manifestSize + signatureSize
        return PayloadHeader(version, manifestSize, signatureSize, blobOffset)
    }

    fun readManifest(header: PayloadHeader): DeltaArchiveManifest {
        val start = payloadDataOffset + FIXED_HEADER_SIZE
        val end   = start + header.manifestSize - 1
        return DeltaArchiveManifest.parseFrom(reader.fetchBytes(start, end))
    }
}

// ─────────────────────────────────────────────────────────────────────────────

internal class LocalPayloadExtractor(
    private val reader: LocalByteReader,
    private val header: PayloadHeader,
    private val blockSize: Long = 4096L,
    private val sourceReader: SourceReader? = null
) {
    companion object {
        // Larger bundles = fewer fetchBytes() calls (fewer read()/JNI round-trips and
        // buffer allocations) for the contiguous REPLACE/REPLACE_XZ runs that make up
        // the bulk of a full OTA payload. Safe to grow further with read-ahead in play,
        // since it's no longer purely blocking time.
        private const val MAX_BUNDLE_SIZE = 32 * 1024 * 1024L
        private const val ZERO_CHUNK_SIZE = 1 * 1024 * 1024
        private const val STREAM_COPY_BUFFER_SIZE = 256 * 1024
    }

    // Reused across every ZERO op in this partition instead of a 4KB buffer looped
    // per block - a multi-GB free-space extent goes from ~1M output.write() calls to
    // a few hundred.
    private val zeroBuffer = ByteArray(ZERO_CHUNK_SIZE)

    private data class BundlePlan(
        val ops: List<InstallOperation>,
        val start: Long,
        val length: Long,
        val nextOpIndex: Int
    )

    private fun isBundleable(op: InstallOperation): Boolean =
        op.dataLength > 0 &&
                op.type != InstallOperation.Type.ZERO &&
                op.type != InstallOperation.Type.DISCARD &&
                op.type != InstallOperation.Type.MOVE &&
                op.type != InstallOperation.Type.SOURCE_COPY

    /** Groups contiguous blob ops starting at [startIdx] into one read, up to MAX_BUNDLE_SIZE. Pure metadata walk, no I/O. */
    private fun planBundle(startIdx: Int, operations: List<InstallOperation>): BundlePlan {
        val firstOp = operations[startIdx]
        val bundleOps = mutableListOf(firstOp)
        val bundleStart = header.blobOffset + firstOp.dataOffset
        var bundleLength = firstOp.dataLength

        var nextIdx = startIdx + 1
        while (nextIdx < operations.size) {
            val nextOp = operations[nextIdx]
            val previous = operations[nextIdx - 1]
            val isContiguous = nextOp.dataOffset == previous.dataOffset + previous.dataLength

            if (isContiguous && isBundleable(nextOp) && bundleLength + nextOp.dataLength <= MAX_BUNDLE_SIZE) {
                bundleOps.add(nextOp)
                bundleLength += nextOp.dataLength
                nextIdx++
            } else {
                break
            }
        }
        require(bundleLength <= Int.MAX_VALUE) { "Bundle size exceeds maximum supported buffer size" }
        return BundlePlan(bundleOps, bundleStart, bundleLength, nextIdx)
    }

    suspend fun extract(
        partitionName: String,
        manifest: DeltaArchiveManifest,
        output: OutputStream,
        resumeState: ExtractionState? = null,
        onProgress: suspend (done: Long, total: Long, opIndex: Int) -> Unit = { _, _, _ -> }
    ): ExtractionState = coroutineScope {
        val partition = manifest.partitionsList
            .firstOrNull { it.partitionName == partitionName }
            ?: error("Partition '$partitionName' not found in manifest.")

        val totalBytes   = partition.newPartitionInfo.size
        val startOpIdx   = resumeState?.lastCompletedOpIndex?.plus(1) ?: 0
        var writtenBytes = resumeState?.bytesWritten ?: 0L
        val operations   = partition.operationsList
        var i = startOpIdx

        // One bundle's bytes are read ahead on the IO dispatcher while the previously
        // fetched bundle is being decompressed and written on this coroutine - overlaps
        // storage read latency with CPU-bound decompression instead of paying for both
        // serially. By construction this is only ever non-null when operations[i] is
        // exactly the bundle it was fetched for (queued right after planning that bundle).
        var prefetch: Deferred<ByteArray>? = null
        var prefetchPlan: BundlePlan? = null

        try {
            while (i < operations.size) {
                currentCoroutineContext().ensureActive()
                val op = operations[i]

                if (op.type == InstallOperation.Type.ZERO) {
                    var remaining = op.dstExtentsList.sumOf { it.numBlocks } * blockSize
                    val zeroBytes = remaining
                    while (remaining > 0) {
                        currentCoroutineContext().ensureActive()
                        val toWrite = minOf(remaining, zeroBuffer.size.toLong()).toInt()
                        output.write(zeroBuffer, 0, toWrite)
                        remaining -= toWrite
                    }
                    writtenBytes += zeroBytes
                    onProgress(writtenBytes, totalBytes, i)
                    i++
                    continue
                }

                if (op.type == InstallOperation.Type.SOURCE_COPY || op.type == InstallOperation.Type.MOVE) {
                    val sReader = sourceReader ?: error(
                        "Operation ${op.type} in partition '$partitionName' requires a source partition reader. " +
                                "This appears to be an incremental (delta) OTA."
                    )
                    op.srcExtentsList.forEach { extent ->
                        val data = sReader.readBlocks(extent.startBlock, extent.numBlocks, blockSize)
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

                // Start of a data bundle: use the read-ahead if one is already in flight
                // for this exact position, otherwise fetch synchronously (first bundle,
                // or right after a ZERO/SOURCE_COPY/MOVE/DISCARD run).
                val plan: BundlePlan
                val bundleData: ByteArray
                val inFlight = prefetch
                if (inFlight != null) {
                    plan = prefetchPlan!!
                    bundleData = inFlight.await()
                } else {
                    plan = planBundle(i, operations)
                    bundleData = reader.fetchBytes(plan.start, plan.start + plan.length - 1)
                }
                prefetch = null
                prefetchPlan = null

                // Kick off the next bundle's read before doing this bundle's CPU-bound
                // decompression/write, so the two overlap.
                val nextIdx = plan.nextOpIndex
                if (nextIdx < operations.size && isBundleable(operations[nextIdx])) {
                    val nextPlan = planBundle(nextIdx, operations)
                    prefetchPlan = nextPlan
                    prefetch = async(Dispatchers.IO) {
                        reader.fetchBytes(nextPlan.start, nextPlan.start + nextPlan.length - 1)
                    }
                }

                var offsetInBundle = 0
                for ((index, bOp) in plan.ops.withIndex()) {
                    currentCoroutineContext().ensureActive()
                    val opLength = bOp.dataLength.toInt()
                    writtenBytes += writeOperation(bundleData, offsetInBundle, opLength, bOp, partitionName, output)
                    offsetInBundle += opLength
                    onProgress(writtenBytes, totalBytes, i + index)
                }

                i = nextIdx
            }
        } finally {
            // No leaked in-flight read if we exit early (cancellation/error).
            prefetch?.cancel()
        }

        output.flush()
        ExtractionState(
            partitionName        = partitionName,
            lastCompletedOpIndex = operations.size - 1,
            bytesWritten         = writtenBytes,
            totalBytes           = totalBytes
        )
    }

    /**
     * Writes a single operation's payload straight out of the shared bundle buffer
     * (offset/length window, no copyOfRange) and returns bytes written. REPLACE_BZ and
     * REPLACE_XZ stream decompressed bytes directly to [output] instead of materializing
     * the whole decompressed operation in a ByteArray first. Only the BSDIFF non-brotli
     * path still needs an owned array, since BsDiffApplier takes a full ByteArray patch.
     */
    private fun writeOperation(
        bundleData: ByteArray,
        offset: Int,
        length: Int,
        op: InstallOperation,
        partitionName: String,
        output: OutputStream
    ): Int {
        return when (op.type) {
            InstallOperation.Type.REPLACE -> {
                output.write(bundleData, offset, length)
                length
            }
            InstallOperation.Type.REPLACE_BZ -> {
                BZip2CompressorInputStream(ByteArrayInputStream(bundleData, offset, length)).use {
                    it.copyTo(output, STREAM_COPY_BUFFER_SIZE)
                }.toInt()
            }
            InstallOperation.Type.REPLACE_XZ -> {
                XZInputStream(ByteArrayInputStream(bundleData, offset, length)).use {
                    it.copyTo(output, STREAM_COPY_BUFFER_SIZE)
                }.toInt()
            }

            InstallOperation.Type.SOURCE_BSDIFF,
            InstallOperation.Type.BROTLI_BSDIFF -> {
                val sReader = sourceReader ?: error(
                    "Operation ${op.type} in partition '$partitionName' requires a source partition reader. " +
                            "Incremental (delta) OTAs require a source image for extraction."
                )

                val totalSrcSize = op.srcExtentsList.sumOf { it.numBlocks * blockSize }
                require(totalSrcSize <= Int.MAX_VALUE) { "Source data size for operation exceeds 2GB" }

                val oldData = ByteArray(totalSrcSize.toInt())
                var currentPos = 0
                op.srcExtentsList.forEach { extent ->
                    val blockData = sReader.readBlocks(extent.startBlock, extent.numBlocks, blockSize)
                    System.arraycopy(blockData, 0, oldData, currentPos, blockData.size)
                    currentPos += blockData.size
                }
                val trimmedOldData = if (oldData.size.toLong() == op.srcLength) oldData else oldData.copyOfRange(0, op.srcLength.toInt())

                val patch = if (op.type == InstallOperation.Type.BROTLI_BSDIFF) {
                    BrotliCompressorInputStream(ByteArrayInputStream(bundleData, offset, length)).use { it.readBytes() }
                } else {
                    bundleData.copyOfRange(offset, offset + length)
                }
                val raw = com.abhinav.otapulse.ota.utils.BsDiffApplier.applyPatch(trimmedOldData, patch)
                output.write(raw)
                raw.size
            }

            InstallOperation.Type.ZERO,
            InstallOperation.Type.DISCARD,
            InstallOperation.Type.MOVE,
            InstallOperation.Type.SOURCE_COPY -> 0

            else -> error(
                "Operation type '${op.type}' in partition '$partitionName' is not supported. " +
                        "Incremental (delta) OTAs require a source image for extraction."
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

internal object LocalMetadataReader {
    private const val PAYLOAD_HEADER_SIZE      = 24
    private const val PAYLOAD_COPY_BUFFER_SIZE = 1024 * 1024
    private val metadataEntries = listOf(
        "metadata",
        "META-INF/com/android/metadata",
        "META-INF/com/android/metadata.pb"
    )

    fun readText(context: Context, uri: Uri): String? {
        val localPath = uri.path?.takeIf { uri.scheme == "file" }
        val zipFile   = if (localPath != null) ZipFile(localPath) else null
        if (zipFile != null) {
            zipFile.use { zip ->
                val entry = metadataEntries.asSequence()
                    .mapNotNull { name -> zip.getEntry(name)?.takeIf { !name.endsWith(".pb") } }
                    .firstOrNull() ?: return null
                return zip.getInputStream(entry).bufferedReader().use { it.readText() }
            }
        }

        context.contentResolver.openInputStream(uri)?.use { input ->
            java.util.zip.ZipInputStream(input).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    if (entry.name in metadataEntries && !entry.name.endsWith(".pb")) {
                        return zip.bufferedReader().use { it.readText() }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        return null
    }

    fun readPayloadPreview(context: Context, uri: Uri): Pair<PayloadHeader, DeltaArchiveManifest> {
        val localPath = uri.path?.takeIf { uri.scheme == "file" }
        val zipFile   = if (localPath != null) ZipFile(localPath) else null
        if (zipFile != null) {
            zipFile.use { zip ->
                val entry = zip.getEntry("payload.bin")
                    ?: error("'payload.bin' not found in the selected OTA ZIP.")
                zip.getInputStream(entry).use { input ->
                    return readPayloadPreview(input)
                }
            }
        }

        context.contentResolver.openInputStream(uri)?.use { input ->
            java.util.zip.ZipInputStream(input).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "payload.bin") return readPayloadPreview(zip)
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        error("'payload.bin' not found in the selected OTA ZIP.")
    }

    private fun readPayloadPreview(input: java.io.InputStream): Pair<PayloadHeader, DeltaArchiveManifest> {
        val headerBytes = readExact(input, PAYLOAD_HEADER_SIZE)
        val magic       = headerBytes.sliceArray(0..3)
        require(magic.contentEquals("CrAU".toByteArray(Charsets.US_ASCII))) {
            "Invalid payload.bin magic. The selected file is not a supported OTA payload (CrAU magic missing)."
        }

        val buffer        = ByteBuffer.wrap(headerBytes).order(ByteOrder.BIG_ENDIAN)
        val version       = buffer.getLong(4)
        val manifestSize  = buffer.getLong(12)
        val signatureSize = buffer.getInt(20)
        val manifestBytes = readExact(input, manifestSize.toInt())
        if (signatureSize > 0) skipExact(input, signatureSize.toLong())

        val header = PayloadHeader(
            version       = version,
            manifestSize  = manifestSize,
            signatureSize = signatureSize,
            blobOffset    = PAYLOAD_HEADER_SIZE + manifestSize + signatureSize
        )
        return header to DeltaArchiveManifest.parseFrom(manifestBytes)
    }

    private fun readExact(input: java.io.InputStream, size: Int): ByteArray {
        val buffer = ByteArray(size)
        var offset = 0
        while (offset < size) {
            val read = input.read(buffer, offset, size - offset)
            if (read <= 0) error("Unexpected EOF while reading payload header.")
            offset += read
        }
        return buffer
    }

    private fun skipExact(input: java.io.InputStream, byteCount: Long) {
        var remaining = byteCount
        while (remaining > 0) {
            val skipped = input.skip(remaining)
            if (skipped > 0) { remaining -= skipped; continue }
            if (input.read() == -1) error("Unexpected EOF while skipping payload signature.")
            remaining -= 1
        }
    }

    /**
     * Last-resort fallback: physically copies payload.bin to the cache dir.
     * Only reached when the ZIP stores payload.bin with DEFLATE compression
     * (extremely rare in Realme/OPPO OTAs) or when the ZIP parser fails.
     */
    fun extractPayloadToCache(context: Context, uri: Uri): File {
        val hashBytes = java.security.MessageDigest.getInstance("SHA-256").digest(uri.toString().toByteArray())
        val cacheKey = hashBytes.joinToString("") { "%02x".format(it) }
        val targetFile = File(context.cacheDir, "selected_ota_payload_$cacheKey.bin")
            .apply { parentFile?.mkdirs() }
        if (targetFile.exists() && targetFile.length() > PAYLOAD_HEADER_SIZE) return targetFile

        val localPath = uri.path?.takeIf { uri.scheme == "file" }
        val zipFile   = if (localPath != null) ZipFile(localPath) else null
        if (zipFile != null) {
            zipFile.use { zip ->
                val entry = zip.getEntry("payload.bin")
                    ?: error("'payload.bin' not found in the selected OTA ZIP.")
                zip.getInputStream(entry).use { input ->
                    targetFile.outputStream().buffered(PAYLOAD_COPY_BUFFER_SIZE).use { output ->
                        input.copyTo(output, PAYLOAD_COPY_BUFFER_SIZE)
                    }
                }
                return targetFile
            }
        }

        context.contentResolver.openInputStream(uri)?.use { input ->
            java.util.zip.ZipInputStream(input).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "payload.bin") {
                        targetFile.outputStream().buffered(PAYLOAD_COPY_BUFFER_SIZE).use { output ->
                            zip.copyTo(output, PAYLOAD_COPY_BUFFER_SIZE)
                        }
                        return targetFile
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        error("'payload.bin' not found in the selected OTA ZIP.")
    }
}