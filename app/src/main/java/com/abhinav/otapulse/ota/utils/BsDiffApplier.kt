package com.abhinav.otapulse.ota.utils

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A pure Kotlin implementation of the bspatch algorithm.
 * Reference: http://www.daemonology.net/bsdiff/
 */
object BsDiffApplier {

    private const val HEADER_SIZE = 32
    private val SIGNATURE = "BSDIFF40".toByteArray(Charsets.US_ASCII)

    private const val MAX_NEW_SIZE = 512L * 1024 * 1024 // 512 MB safety limit for partition patches

    /**
     * Applies a bsdiff patch to [oldData] and returns the [newData].
     */
    fun applyPatch(oldData: ByteArray, patchData: ByteArray): ByteArray {
        if (patchData.size < HEADER_SIZE) {
            throw IllegalArgumentException("Patch too short")
        }

        val headerBuf = ByteBuffer.wrap(patchData, 0, HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        val sig = ByteArray(8)
        headerBuf.get(sig)
        if (!sig.contentEquals(SIGNATURE)) {
            throw IllegalArgumentException("Invalid patch signature")
        }

        val ctrlBlockLen = readBsdiffLong(headerBuf)
        val diffBlockLen = readBsdiffLong(headerBuf)
        val newSize = readBsdiffLong(headerBuf)

        if (ctrlBlockLen < 0 || diffBlockLen < 0 || newSize < 0) {
            throw IllegalArgumentException("Invalid header values")
        }

        if (newSize > MAX_NEW_SIZE) {
            throw IllegalArgumentException("Patch output too large: $newSize bytes (max allowed: $MAX_NEW_SIZE)")
        }

        val newData = ByteArray(newSize.toInt())

        val ctrlStream = BZip2CompressorInputStream(
            ByteArrayInputStream(patchData, HEADER_SIZE, ctrlBlockLen.toInt())
        )
        val diffStream = BZip2CompressorInputStream(
            ByteArrayInputStream(patchData, (HEADER_SIZE + ctrlBlockLen).toInt(), diffBlockLen.toInt())
        )
        val extraStream = BZip2CompressorInputStream(
            ByteArrayInputStream(patchData, (HEADER_SIZE + ctrlBlockLen + diffBlockLen).toInt(), patchData.size - (HEADER_SIZE + ctrlBlockLen + diffBlockLen).toInt())
        )

        try {
            var oldOffset = 0
            var newOffset = 0

            val ctrlBuf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
            val chunkBuf = ByteArray(8192)

            while (newOffset < newSize) {
                // Read control data
                val addLen = readStreamLong(ctrlStream, ctrlBuf)
                val copyLen = readStreamLong(ctrlStream, ctrlBuf)
                val offsetChange = readStreamLong(ctrlStream, ctrlBuf)

                if (newOffset + addLen > newSize) {
                    throw IllegalArgumentException("Corrupt patch: addLen too large")
                }

                // 1. Add old data to diff data (buffered chunks)
                var addRemaining = addLen.toInt()
                var chunkOffset = 0
                while (addRemaining > 0) {
                    val toRead = minOf(chunkBuf.size, addRemaining)
                    val bytesRead = diffStream.read(chunkBuf, 0, toRead)
                    if (bytesRead == -1) throw IllegalArgumentException("Unexpected EOF in diff stream")
                    for (j in 0 until bytesRead) {
                        val currentOldPos = oldOffset + chunkOffset + j
                        val oldVal = if (currentOldPos in oldData.indices) oldData[currentOldPos] else 0
                        newData[newOffset + chunkOffset + j] = (oldVal + chunkBuf[j]).toByte()
                    }
                    chunkOffset += bytesRead
                    addRemaining -= bytesRead
                }
                newOffset += addLen.toInt()
                oldOffset += addLen.toInt()

                if (newOffset + copyLen > newSize) {
                    throw IllegalArgumentException("Corrupt patch: copyLen too large")
                }

                // 2. Copy extra data
                var extraRead = 0
                while (extraRead < copyLen.toInt()) {
                    val read = extraStream.read(newData, newOffset + extraRead, copyLen.toInt() - extraRead)
                    if (read == -1) throw IllegalArgumentException("Unexpected EOF in extra stream")
                    extraRead += read
                }
                newOffset += copyLen.toInt()
                oldOffset += offsetChange.toInt()
            }
        } finally {
            ctrlStream.close()
            diffStream.close()
            extraStream.close()
        }

        return newData
    }

    private fun readBsdiffLong(buf: ByteBuffer): Long {
        val raw = buf.getLong()
        // bsdiff uses the high bit as a sign indicator
        var result = raw and 0x7FFFFFFFFFFFFFFFL
        if ((raw and Long.MIN_VALUE) != 0L) result = -result
        return result
    }

    private fun readStreamLong(stream: InputStream, buf: ByteBuffer): Long {
        buf.clear()
        var read = 0
        while (read < 8) {
            val r = stream.read(buf.array(), read, 8 - read)
            if (r == -1) throw IllegalArgumentException("Unexpected EOF in control stream")
            read += r
        }
        return readBsdiffLong(buf)
    }
}

