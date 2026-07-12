package com.abhinav.otapulse.ota.utils

import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BsDiffApplierTest {

    @Test
    fun testSimplePatch() {
        val oldData = "Hello World".toByteArray()
        val newData = "Hello Kotlin".toByteArray()
        
        // Construct a manual bsdiff patch for "Hello World" -> "Hello Kotlin"
        // bsdiff format:
        // 0-7: "BSDIFF40"
        // 8-15: control block length
        // 16-23: diff block length
        // 24-31: new file size
        
        // Control block (3 entries): addLen, copyLen, offsetChange
        // We want to keep "Hello " (6 bytes), then diff "World" (5 bytes) with "Kotli" (5 bytes), then add "n" (1 byte)
        // Actually, let's keep it simpler: 
        // 1. Keep "Hello " (6 bytes), change "World" to "Kotlin" (6 bytes)
        // Control entry 1: addLen=12, copyLen=0, offsetChange=0
        
        val diffData = ByteArray(12)
        // newData[i] = oldData[i] + diff[i]  => diff[i] = newData[i] - oldData[i]
        for (i in 0 until 6) {
            diffData[i] = (newData[i] - oldData[i]).toByte() // Should be 0
        }
        for (i in 6 until 11) {
            diffData[i] = (newData[i] - oldData[i]).toByte()
        }
        diffData[11] = newData[11] // "n" since oldData ends at 10
        
        val ctrlStream = ByteArrayOutputStream()
        BZip2CompressorOutputStream(ctrlStream).use { bz ->
            writeBsdiffLong(bz, 12) // addLen
            writeBsdiffLong(bz, 0)  // copyLen
            writeBsdiffLong(bz, 0)  // offsetChange
        }
        val ctrlBytes = ctrlStream.toByteArray()
        
        val diffStream = ByteArrayOutputStream()
        BZip2CompressorOutputStream(diffStream).use { bz ->
            bz.write(diffData)
        }
        val diffBytes = diffStream.toByteArray()
        
        val extraStream = ByteArrayOutputStream()
        BZip2CompressorOutputStream(extraStream).use { bz -> }
        val extraBytes = extraStream.toByteArray()
        
        val patch = ByteBuffer.allocate(32 + ctrlBytes.size + diffBytes.size + extraBytes.size)
            .order(ByteOrder.LITTLE_ENDIAN)
        patch.put("BSDIFF40".toByteArray())
        writeBsdiffLong(patch, ctrlBytes.size.toLong())
        writeBsdiffLong(patch, diffBytes.size.toLong())
        writeBsdiffLong(patch, newData.size.toLong())
        patch.put(ctrlBytes)
        patch.put(diffBytes)
        patch.put(extraBytes)
        
        val result = BsDiffApplier.applyPatch(oldData, patch.array())
        assertArrayEquals(newData, result)
    }

    private fun writeBsdiffLong(out: java.io.OutputStream, value: Long) {
        var v = if (value < 0) -value else value
        if (value < 0) v = v or Long.MIN_VALUE
        
        val buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(v).array()
        out.write(buf)
    }

    private fun writeBsdiffLong(buf: ByteBuffer, value: Long) {
        var v = if (value < 0) -value else value
        if (value < 0) v = v or Long.MIN_VALUE
        buf.putLong(v)
    }
}
