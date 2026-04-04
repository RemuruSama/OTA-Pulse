package com.abhinav.otapulse.ota.payload

import chromeos_update_engine.UpdateMetadata.DeltaArchiveManifest
import com.abhinav.otapulse.ota.network.RangeHttpClient
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class PayloadHeader(
    val version: Long,
    val manifestSize: Long,
    val signatureSize: Int,
    /** Absolute offset in the remote ZIP where partition blob data starts */
    val blobOffset: Long
)

data class PartitionInfo(
    val name: String,
    val sizeBytes: Long,
    val opCount: Int
) {
    val formattedSize: String get() = when {
        sizeBytes >= 1_073_741_824L -> "%.2f GB".format(sizeBytes / 1_073_741_824.0)
        sizeBytes >= 1_048_576L     -> "%.1f MB".format(sizeBytes / 1_048_576.0)
        sizeBytes >= 1024L          -> "%.1f KB".format(sizeBytes / 1024.0)
        else                        -> "$sizeBytes B"
    }
}

class PayloadManifest(
    private val url: String,
    private val http: RangeHttpClient,
    private val payloadDataOffset: Long
) {
    companion object {
        private val MAGIC = "CrAU".toByteArray(Charsets.US_ASCII)
        // magic(4) + version(8) + manifest_size(8) + sig_size(4) = 24 bytes
        private const val FIXED_HEADER_SIZE = 24L
    }

    fun readHeader(): PayloadHeader {
        val headerBytes = http.fetchBytes(
            url,
            payloadDataOffset,
            payloadDataOffset + FIXED_HEADER_SIZE - 1
        )

        val magic = headerBytes.sliceArray(0..3)
        require(magic.contentEquals(MAGIC)) {
            "Invalid payload.bin magic: '${String(magic)}'. " +
            "Expected 'CrAU'. The file may be corrupted, encrypted, or not a valid OTA payload."
        }

        val buf = ByteBuffer.wrap(headerBytes).order(ByteOrder.BIG_ENDIAN)
        val version      = buf.getLong(4)
        val manifestSize = buf.getLong(12)
        val sigSize      = buf.getInt(20)

        val blobOffset = payloadDataOffset + FIXED_HEADER_SIZE + manifestSize + sigSize

        return PayloadHeader(version, manifestSize, sigSize, blobOffset)
    }

    fun readManifest(header: PayloadHeader): DeltaArchiveManifest {
        val start = payloadDataOffset + FIXED_HEADER_SIZE
        val end   = start + header.manifestSize - 1
        val bytes = http.fetchBytes(url, start, end)
        return DeltaArchiveManifest.parseFrom(bytes)
    }
}
