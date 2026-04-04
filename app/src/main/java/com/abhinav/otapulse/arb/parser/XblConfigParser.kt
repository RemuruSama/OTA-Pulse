package com.abhinav.otapulse.arb.parser

import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Kotlin port of koaaN/arbextract.c
 *
 * Parses an `xbl_config.img` ELF64 binary to extract the Anti-Rollback (ARB) index
 * from the OEM Metadata section within the HASH segment.
 *
 * Algorithm:
 * 1. Validate ELF64 magic and class
 * 2. Read program headers, find the last non-empty PT_NULL segment (HASH segment)
 * 3. Scan first 0x1000 bytes for a "Hash Table Segment Header" with sanity checks
 * 4. Read OEM Metadata at offset: header_off + 36 + common_sz + qti_sz
 *    - uint32 major, uint32 minor, uint32 arb (little-endian)
 */
class XblConfigParser {

    companion object {
        private const val TAG = "XblConfigParser"

        // ELF magic: 0x7f 'E' 'L' 'F'
        private val ELF_MAGIC = byteArrayOf(0x7f, 0x45, 0x4c, 0x46)
        private const val EI_CLASS = 4
        private const val ELFCLASS64 = 2.toByte()

        // ELF64 header offsets
        private const val E_PHOFF_OFFSET = 0x20    // program header table offset (uint64)
        private const val E_PHENTSIZE_OFFSET = 0x36 // program header entry size (uint16)
        private const val E_PHNUM_OFFSET = 0x38     // number of program headers (uint16)
        private const val ELF_HEADER_SIZE = 64

        // Program header fields (ELF64_Phdr) – little-endian
        private const val PH_SIZE = 56
    }

    data class ArbResult(
        val arb: Int,
        val major: Int,
        val minor: Int
    ) {
        fun toDisplayString(): String {
            return if (arb == 0) "Safe" else "Protected"
        }
    }

    /**
     * Parse xbl_config image data and extract the ARB value.
     * @param data The raw xbl_config.img bytes (decompressed)
     * @return ArbResult or null if parsing fails
     */
    fun parse(data: ByteArray): ArbResult? {
        try {
            // 1. Validate ELF64 header
            if (data.size < ELF_HEADER_SIZE) {
                Log.w(TAG, "Data too small for ELF header: ${data.size}")
                return null
            }

            // Check ELF magic
            for (i in 0 until 4) {
                if (data[i] != ELF_MAGIC[i]) {
                    Log.w(TAG, "Invalid ELF magic")
                    return null
                }
            }

            // Check ELF64 class
            if (data[EI_CLASS] != ELFCLASS64) {
                Log.w(TAG, "Not ELF64 (class = ${data[EI_CLASS]})")
                return null
            }

            val headerBuf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

            // 2. Read program header table info
            headerBuf.position(E_PHOFF_OFFSET)
            val phOff = headerBuf.long

            headerBuf.position(E_PHENTSIZE_OFFSET)
            val phEntSize = java.lang.Short.toUnsignedInt(headerBuf.short)

            headerBuf.position(E_PHNUM_OFFSET)
            val phNum = java.lang.Short.toUnsignedInt(headerBuf.short)

            // 3. Find the HASH segment: last non-empty PT_NULL
            var hashOff = 0L
            var hashSize = 0L

            for (i in phNum - 1 downTo 0) {
                val phPos = (phOff + i.toLong() * phEntSize).toInt()
                if (phPos + PH_SIZE > data.size) continue

                val phBuf = ByteBuffer.wrap(data, phPos, PH_SIZE).order(ByteOrder.LITTLE_ENDIAN)
                val pType = phBuf.int       // p_type
                phBuf.int                   // p_flags
                val pOffset = phBuf.long    // p_offset
                phBuf.long                  // p_vaddr
                phBuf.long                  // p_paddr
                val pFileSz = phBuf.long    // p_filesz

                // PT_NULL with non-zero size
                if (pType == 0 && pFileSz > 0) {
                    hashOff = pOffset
                    hashSize = pFileSz
                    break
                }
            }

            if (hashSize == 0L) {
                Log.w(TAG, "HASH segment not found")
                return null
            }

            // 4. Read the HASH segment
            val hashStart = hashOff.toInt()
            val hashLen = hashSize.toInt()
            if (hashStart + hashLen > data.size) {
                Log.w(TAG, "HASH segment extends beyond data (off=$hashStart, size=$hashLen, total=${data.size})")
                return null
            }

            // 5. Scan for Hash Table Segment Header within first 0x1000 bytes
            val scanLimit = minOf(0x1000, hashLen)
            var headerOff = -1
            var commonSz = 0
            var qtiSz = 0

            var off = 0
            while (off + 36 <= scanLimit) {
                val version = readUint32(data, hashStart + off)
                val common = readUint32(data, hashStart + off + 4)
                val qti = readUint32(data, hashStart + off + 8)
                val oem = readUint32(data, hashStart + off + 12)
                val hashTblSz = readUint32(data, hashStart + off + 16)

                // Sanity checks (matching arbextract.c)
                if (version in 1..10 &&
                    common <= 0x1000 &&
                    oem <= 0x4000 &&
                    hashTblSz <= 0x4000 &&
                    off + 36 + common.toInt() + qti.toInt() + oem.toInt() <= hashLen
                ) {
                    headerOff = off
                    commonSz = common.toInt()
                    qtiSz = qti.toInt()
                    break
                }
                off += 4
            }

            if (headerOff < 0) {
                Log.w(TAG, "Hash table header not found")
                return null
            }

            // 6. Read OEM Metadata
            val oemMdOff = hashStart + headerOff + 36 + commonSz + qtiSz
            if (oemMdOff + 12 > data.size) {
                Log.w(TAG, "OEM metadata offset out of bounds")
                return null
            }

            val major = readUint32(data, oemMdOff).toInt()
            val minor = readUint32(data, oemMdOff + 4).toInt()
            val arb = readUint32(data, oemMdOff + 8).toInt()

            Log.i(TAG, "OEM Metadata — Major: $major, Minor: $minor, ARB: $arb")

            return ArbResult(arb = arb, major = major, minor = minor)

        } catch (e: Exception) {
            Log.w(TAG, "Error parsing xbl_config", e)
            return null
        }
    }

    private fun readUint32(data: ByteArray, offset: Int): Long {
        return ByteBuffer.wrap(data, offset, 4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .int
            .toLong() and 0xFFFFFFFFL
    }
}
