package com.abhinav.otapulse.ota.zip

import com.abhinav.otapulse.ota.network.RangeHttpClient
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class RemoteZipEntry(
    val name: String,
    val localHeaderOffset: Long,
    val compressedSize: Long,
    val uncompressedSize: Long,
    val compressionMethod: Int
)

/**
 * Parses a remote ZIP file's central directory using HTTP Range requests.
 * Supports ZIP32 and ZIP64.
 *
 * Key fix over the original: instead of searching backward through the tail for
 * the ZIP64 EOCD Locator (LOC64) signature — which can produce false positives
 * from compressed payload data — we locate it deterministically. The ZIP spec
 * guarantees LOC64 sits exactly 20 bytes before EOCD32.
 *
 * Parsing flow:
 *   1. Fetch last 65 536 bytes (enough for any EOCD32 + comment + LOC64).
 *   2. Find EOCD32 by scanning backward for its signature.
 *   3. If CD offset/size sentinel → ZIP64 path:
 *        a. LOC64 is at eocdPos - 20; verify its signature.
 *        b. Fetch EOCD64 from the absolute offset in the locator.
 *        c. Extract real cdOffset + cdSize from EOCD64.
 *   4. Fetch the central directory and scan for the requested entry.
 */
class ZipRemoteParser(
    private val url: String,
    private val http: RangeHttpClient,
    private val fileSize: Long
) {
    companion object {
        private val EOCD_SIG   = byteArrayOf(0x50, 0x4B, 0x05, 0x06)
        private val EOCD64_SIG = byteArrayOf(0x50, 0x4B, 0x06, 0x06)
        private val LOC64_SIG  = byteArrayOf(0x50, 0x4B, 0x06, 0x07)
        private val CD_SIG     = byteArrayOf(0x50, 0x4B, 0x01, 0x02)

        /** Large enough to contain any EOCD32 + max comment (65 535 B) + LOC64 (20 B). */
        private const val TAIL_SIZE = 65_556L
    }

    // ── Public API ────────────────────────────────────────────────────────────

    suspend fun findEntry(name: String): RemoteZipEntry {
        val tailStart = maxOf(0L, fileSize - TAIL_SIZE)
        val tail = http.fetchBytes(url, tailStart, fileSize - 1)

        // EOCD32 is always present; search backward to skip any trailing garbage.
        val eocdPos = findSigFromEnd(tail, EOCD_SIG)
            ?: error(
                "No EOCD signature found in last ${tail.size} bytes. " +
                        "The server may have returned partial data, or this is not a ZIP file."
            )

        val buf = ByteBuffer.wrap(tail).order(ByteOrder.LITTLE_ENDIAN)
        val cdSizeRaw   = buf.getInt(eocdPos + 12).toUInt().toLong()
        val cdOffsetRaw = buf.getInt(eocdPos + 16).toUInt().toLong()

        val (cdSize, cdOffset) = if (
            cdOffsetRaw == 0xFFFF_FFFFL ||
            cdSizeRaw   == 0xFFFF_FFFFL
        ) {
            // ZIP64: LOC64 is ALWAYS exactly 20 bytes before EOCD32.
            resolveZip64CD(tail, tailStart, eocdPos)
        } else {
            Pair(cdSizeRaw, cdOffsetRaw)
        }

        val cdData = http.fetchBytes(url, cdOffset, cdOffset + cdSize - 1)
        return parseCentralDirectory(cdData, name)
            ?: error("'$name' not found in ZIP central directory.")
    }

    /**
     * Resolve the actual central-directory size and offset from a ZIP64 archive.
     */
    private suspend fun resolveZip64CD(
        tail: ByteArray,
        tailStart: Long,
        eocdPos: Int      // position of EOCD32 within tail[]
    ): Pair<Long, Long> {
        val loc64Pos = eocdPos - 20
        require(loc64Pos >= 0) {
            "ZIP64 EOCD Locator would be before the start of fetched tail data. " +
                    "Try increasing TAIL_SIZE."
        }
        val loc64Sig = tail.sliceArray(loc64Pos until loc64Pos + 4)
        require(loc64Sig.contentEquals(LOC64_SIG)) {
            "Expected ZIP64 EOCD Locator signature at tail[$loc64Pos] " +
                    "(file offset ${tailStart + loc64Pos}), got ${loc64Sig.toHex()}. " +
                    "The file may use an unusual ZIP64 layout."
        }

        val buf = ByteBuffer.wrap(tail).order(ByteOrder.LITTLE_ENDIAN)
        // LOC64 layout: sig(4) + diskWithEOCD64(4) + eocd64Offset(8) + totalDisks(4)
        val eocd64AbsOffset = buf.getLong(loc64Pos + 8)

        // EOCD64 is at minimum 56 bytes.
        val eocd64Bytes = http.fetchBytes(url, eocd64AbsOffset, eocd64AbsOffset + 55)
        require(eocd64Bytes.sliceArray(0..3).contentEquals(EOCD64_SIG)) {
            "ZIP64 EOCD64 signature mismatch at file offset $eocd64AbsOffset. " +
                    "Got ${eocd64Bytes.sliceArray(0..3).toHex()}."
        }

        val eocd64Buf = ByteBuffer.wrap(eocd64Bytes).order(ByteOrder.LITTLE_ENDIAN)
        // EOCD64 layout: sig(4)+size(8)+versionMadeBy(2)+versionNeeded(2)+
        //                diskNum(4)+startDisk(4)+entriesOnDisk(8)+totalEntries(8)+
        //                cdSize(8)+cdOffset(8)
        val cdSize   = eocd64Buf.getLong(40)
        val cdOffset = eocd64Buf.getLong(48)
        return Pair(cdSize, cdOffset)
    }

    // ── Central directory parser ──────────────────────────────────────────────

    private fun parseCentralDirectory(cd: ByteArray, targetName: String): RemoteZipEntry? {
        var pos = 0
        while (pos <= cd.size - 46) {
            val sig = cd.sliceArray(pos until pos + 4)
            if (!sig.contentEquals(CD_SIG)) { pos++; continue }

            val buf = ByteBuffer.wrap(cd, pos, cd.size - pos).order(ByteOrder.LITTLE_ENDIAN)

            val method           = buf.getShort(10).toInt() and 0xFFFF
            var compressedSize   = buf.getInt(20).toUInt().toLong()
            var uncompressedSize = buf.getInt(24).toUInt().toLong()
            val filenameLen      = buf.getShort(28).toInt() and 0xFFFF
            val extraLen         = buf.getShort(30).toInt() and 0xFFFF
            val commentLen       = buf.getShort(32).toInt() and 0xFFFF
            var localOffset      = buf.getInt(42).toUInt().toLong()

            val name = String(cd, pos + 46, filenameLen, Charsets.UTF_8)

            // Resolve ZIP64 extra fields when sentinel 0xFFFFFFFF values are present.
            if (compressedSize   == 0xFFFF_FFFFL ||
                uncompressedSize == 0xFFFF_FFFFL ||
                localOffset      == 0xFFFF_FFFFL
            ) {
                val extraStart = pos + 46 + filenameLen
                val extra = cd.sliceArray(extraStart until extraStart + extraLen)
                parseZip64Extra(extra)?.let { z64 ->
                    if (uncompressedSize == 0xFFFF_FFFFL) uncompressedSize = z64.uncompressedSize
                    if (compressedSize   == 0xFFFF_FFFFL) compressedSize   = z64.compressedSize
                    if (localOffset      == 0xFFFF_FFFFL) localOffset      = z64.localHeaderOffset
                }
            }

            if (name == targetName) {
                return RemoteZipEntry(name, localOffset, compressedSize, uncompressedSize, method)
            }

            pos += 46 + filenameLen + extraLen + commentLen
        }
        return null
    }

    // ── ZIP64 extra field ─────────────────────────────────────────────────────

    private data class Zip64Extra(
        val uncompressedSize: Long,
        val compressedSize: Long,
        val localHeaderOffset: Long
    )

    private fun parseZip64Extra(extra: ByteArray): Zip64Extra? {
        var i = 0
        while (i + 4 <= extra.size) {
            val buf = ByteBuffer.wrap(extra, i, extra.size - i).order(ByteOrder.LITTLE_ENDIAN)
            val headerId = buf.getShort(0).toInt() and 0xFFFF
            val dataSize = buf.getShort(2).toInt() and 0xFFFF
            if (headerId == 0x0001 && i + 4 + dataSize >= 24) {
                return Zip64Extra(
                    uncompressedSize  = buf.getLong(4),
                    compressedSize    = buf.getLong(12),
                    localHeaderOffset = buf.getLong(20)
                )
            }
            i += 4 + dataSize
        }
        return null
    }

    // ── Resolve local file header → data offset ───────────────────────────────

    suspend fun resolveDataOffset(entry: RemoteZipEntry): Long {
        val localHeader = http.fetchBytes(
            url,
            entry.localHeaderOffset,
            entry.localHeaderOffset + 29
        )
        val filenameLen = localHeader.leUShort(26)
        val extraLen    = localHeader.leUShort(28)
        return entry.localHeaderOffset + 30 + filenameLen + extraLen
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun findSigFromEnd(data: ByteArray, sig: ByteArray): Int? {
        if (data.size < sig.size) return null
        for (i in data.size - sig.size downTo 0) {
            var match = true
            for (j in sig.indices) {
                if (data[i + j] != sig[j]) {
                    match = false
                    break
                }
            }
            if (match) return i
        }
        return null
    }

    private fun ByteArray.leUShort(offset: Int): Int =
        ((this[offset + 1].toInt() and 0xFF) shl 8) or (this[offset].toInt() and 0xFF)

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it) }
}
