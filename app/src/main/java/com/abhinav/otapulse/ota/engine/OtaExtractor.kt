package com.abhinav.otapulse.ota.engine

import android.content.Context
import android.net.Uri
import chromeos_update_engine.UpdateMetadata.DeltaArchiveManifest
import chromeos_update_engine.UpdateMetadata.InstallOperation
import com.abhinav.otapulse.ota.network.RangeHttpClient
import com.abhinav.otapulse.ota.network.ServerCapabilityChecker
import com.abhinav.otapulse.ota.payload.PayloadExtractor
import com.abhinav.otapulse.ota.payload.PayloadHeader
import com.abhinav.otapulse.ota.payload.PayloadManifest
import com.abhinav.otapulse.ota.payload.PartitionInfo
import com.abhinav.otapulse.ota.resume.ExtractionState
import com.abhinav.otapulse.ota.resume.ExtractionStateStore
import com.abhinav.otapulse.ota.zip.ZipRemoteParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

// ── Data models ──────────────────────────────────────────────────────────────

data class OtaFileEntry(
    val name: String,
    val offset: Long,
    val size: Long,
    /**
     * If true, [offset] points directly to the start of the file's data (skipping ZIP header).
     * This is standard for 'ota-property-files' in the 'metadata' file.
     * If false, [offset] points to the ZIP local file header.
     */
    val isDataOffset: Boolean = false
)


data class OtaMetadataMap(
    val entries: Map<String, OtaFileEntry>,
    val otaType: String,
    val otaId: String,
    val postBuild: String,
    val securityPatch: String
) {
    fun require(name: String): OtaFileEntry =
        entries[name] ?: error("'$name' not found in OTA metadata")

    val hasMetadataFile: Boolean get() = entries.isNotEmpty()
}

// ── Main extractor ────────────────────────────────────────────────────────────

class OtaExtractor(private val context: Context) {

    private val http       = RangeHttpClient()
    private val checker    = ServerCapabilityChecker(http)
    private val stateStore = ExtractionStateStore(context)

    companion object {
        /**
         * Candidate names for the plaintext metadata file.
         */
        private val METADATA_ENTRY_NAMES = listOf(
            "metadata",                       // modern OPPO / Realme OTAs
            "META-INF/com/android/metadata",  // legacy / some OEM variants
            "META-INF/com/android/metadata.pb" // newer protobuf metadata (rare)
        )
    }

    data class Session(
        val url: String,
        val metadataMap: OtaMetadataMap,
        val manifest: DeltaArchiveManifest,
        val header: PayloadHeader,
        val extractor: PayloadExtractor? = null,
        val localUri: Uri? = null,
        val localPayloadFile: java.io.File? = null,
        val localPayloadOffset: Long? = null,
        val localPayloadSize: Long? = null
    )

    // ── Step 1: fetch & parse OTA metadata ───────────────────────────────────

    suspend fun fetchMetadataMap(url: String): OtaMetadataMap = withContext(Dispatchers.IO) {
        val caps = checker.check(url)
        require(caps.supportsRangeRequests) {
            "Server at $url does not support HTTP Range requests. " +
                    "Cannot perform partial extraction."
        }
        require(caps.contentLength > 0) {
            "Could not determine OTA file size. Server may not return Content-Length."
        }

        val zipParser = ZipRemoteParser(url, http, caps.contentLength)

        // ── Try each known metadata entry name ───────────────────────────────
        var metaMap: OtaMetadataMap? = null
        for (candidateName in METADATA_ENTRY_NAMES) {
            val entry = runCatching { zipParser.findEntry(candidateName) }.getOrNull()
                ?: continue

            val dataOffset = zipParser.resolveDataOffset(entry)
            val metaText = String(
                http.fetchBytes(url, dataOffset, dataOffset + entry.compressedSize - 1),
                Charsets.UTF_8
            )

            if (candidateName.endsWith(".pb")) continue

            metaMap = parseMetadata(metaText)
            break
        }

        if (metaMap != null) {
            if (metaMap.entries.containsKey("payload.bin")) {
                return@withContext metaMap
            }

            val payloadEntry = runCatching { zipParser.findEntry("payload.bin") }.getOrNull()
            if (payloadEntry != null) {
                val updatedEntries = metaMap.entries.toMutableMap()
                updatedEntries["payload.bin"] = OtaFileEntry(
                    "payload.bin",
                    payloadEntry.localHeaderOffset,
                    payloadEntry.compressedSize,
                    isDataOffset = false
                )
                return@withContext metaMap.copy(entries = updatedEntries)
            }
            error("Found 'metadata' file but 'payload.bin' is missing.")
        }

        // Fallback: minimal map from payload.bin
        val payloadEntry = runCatching { zipParser.findEntry("payload.bin") }.getOrNull()
            ?: error("Could not locate 'metadata' or 'payload.bin' in the ZIP.")

        val entries = mapOf(
            "payload.bin" to OtaFileEntry(
                "payload.bin",
                payloadEntry.localHeaderOffset,
                payloadEntry.compressedSize,
                isDataOffset = false
            )
        )
        OtaMetadataMap(
            entries       = entries,
            otaType       = "AB",
            otaId         = "",
            postBuild     = "",
            securityPatch = ""
        )
    }

    private fun parseMetadata(text: String): OtaMetadataMap {
        val props = mutableMapOf<String, String>()
        text.lines().forEach { line ->
            val eq = line.indexOf('=')
            if (eq > 0) props[line.substring(0, eq).trim()] = line.substring(eq + 1).trim()
        }

        val propertyFiles = props["ota-property-files"] ?: ""

        val entries = propertyFiles.trim().split(",")
            .mapNotNull { token ->
                val parts = token.trim().split(":")
                if (parts.size == 3) {
                    val name   = parts[0].trim()
                    val offset = parts[1].trim().toLongOrNull() ?: return@mapNotNull null
                    val size   = parts[2].trim().toLongOrNull() ?: return@mapNotNull null
                    name to OtaFileEntry(name, offset, size, isDataOffset = true)
                } else null
            }.toMap()

        return OtaMetadataMap(
            entries       = entries,
            otaType       = props["ota-type"] ?: "AB",
            otaId         = props["ota-id"] ?: props["ota_version"] ?: props["ota-version"] ?: "",
            postBuild     = props["post-build"] ?: "",
            securityPatch = props["post-security-patch-level"]
                ?: props["security_patch"]
                ?: props["google_patch"]
                ?: ""
        )
    }

    // ── Step 2: open session ─────────────────────────────────────────────────

    suspend fun open(url: String): Session = withContext(Dispatchers.IO) {
        if (!url.startsWith("http", ignoreCase = true)) {
            return@withContext openLocal(Uri.parse(url))
        }

        val metadataMap = fetchMetadataMap(url)
        val payloadEntry = metadataMap.require("payload.bin")

        val payloadDataOffset = if (payloadEntry.isDataOffset) {
            payloadEntry.offset
        } else {
            val localHeaderBytes = http.fetchBytes(
                url,
                payloadEntry.offset,
                payloadEntry.offset + 29
            )
            val filenameLen = leShort(localHeaderBytes, 26)
            val extraLen    = leShort(localHeaderBytes, 28)
            payloadEntry.offset + 30 + filenameLen + extraLen
        }

        val manifester = PayloadManifest(url, http, payloadDataOffset)
        val header     = manifester.readHeader()
        val manifest   = manifester.readManifest(header)

        val deltaOps = setOf(
            InstallOperation.Type.SOURCE_COPY,
            InstallOperation.Type.BSDIFF,
            InstallOperation.Type.SOURCE_BSDIFF,
            InstallOperation.Type.BROTLI_BSDIFF,
            InstallOperation.Type.PUFFDIFF,
            InstallOperation.Type.ZUCCHINI,
            InstallOperation.Type.LZ4DIFF_BSDIFF,
            InstallOperation.Type.LZ4DIFF_PUFFDIFF
        )
        val isDelta = manifest.partitionsList.any { partition ->
            partition.operationsList.any { op -> op.type in deltaOps }
        }
        require(!isDelta) {
            "This is an incremental (delta) OTA package. Partition extraction requires a full OTA."
        }

        val blockSize = if (manifest.hasBlockSize()) manifest.blockSize.toLong() else 4096L
        val extractor = PayloadExtractor(url, http, header, blockSize)
        Session(url, metadataMap, manifest, header, extractor)
    }

    private fun openLocal(uri: Uri): Session {
        val metadataText = LocalMetadataReader.readText(context, uri)
        val metadataMap = metadataText
            ?.takeUnless { it.isBlank() }
            ?.let(::parseMetadata)
            ?: OtaMetadataMap(
                entries = emptyMap(),
                otaType = "AB",
                otaId = "",
                postBuild = "",
                securityPatch = ""
            )

        val reader = LocalRangeReader(context, uri)
        var fastFound = false
        var fastHeader: PayloadHeader? = null
        var fastManifest: DeltaArchiveManifest? = null
        var payloadOffset: Long? = null
        var payloadSize: Long? = null

        try {
            val zipParser = LocalZipParser(reader)
            val payloadEntry = runCatching { zipParser.findEntry("payload.bin") }.getOrNull()
            if (payloadEntry != null && payloadEntry.compressionMethod == 0) {
                val dataOffset = zipParser.resolveDataOffset(payloadEntry)
                payloadOffset = dataOffset
                payloadSize = payloadEntry.uncompressedSize

                val offsetReader = OffsetLocalByteReader(reader, dataOffset, payloadSize)
                val manifester = LocalPayloadManifest(offsetReader, 0L)
                fastHeader = manifester.readHeader()
                fastManifest = manifester.readManifest(fastHeader)
                fastFound = true
            }
        } catch (e: Exception) {
            // fallback gracefully
        } finally {
            reader.close()
        }

        val header: PayloadHeader
        val manifest: DeltaArchiveManifest

        if (fastFound) {
            header = fastHeader!!
            manifest = fastManifest!!
        } else {
            val preview = LocalMetadataReader.readPayloadPreview(context, uri)
            header = preview.first
            manifest = preview.second
        }

        val deltaOps = setOf(
            InstallOperation.Type.SOURCE_COPY,
            InstallOperation.Type.BSDIFF,
            InstallOperation.Type.SOURCE_BSDIFF,
            InstallOperation.Type.BROTLI_BSDIFF,
            InstallOperation.Type.PUFFDIFF,
            InstallOperation.Type.ZUCCHINI,
            InstallOperation.Type.LZ4DIFF_BSDIFF,
            InstallOperation.Type.LZ4DIFF_PUFFDIFF
        )
        val isDelta = manifest.partitionsList.any { partition ->
            partition.operationsList.any { op -> op.type in deltaOps }
        }
        require(!isDelta) {
            "This is an incremental (delta) OTA package. Partition extraction requires a full OTA."
        }

        return Session(
            url = uri.toString(),
            metadataMap = metadataMap,
            manifest = manifest,
            header = header,
            localUri = uri,
            localPayloadOffset = payloadOffset,
            localPayloadSize = payloadSize
        )
    }

    // ── Step 3: list partitions ───────────────────────────────────────────────

    suspend fun listPartitions(session: Session): List<PartitionInfo> =
        withContext(Dispatchers.IO) {
            session.manifest.partitionsList.map { p ->
                PartitionInfo(
                    name      = p.partitionName,
                    sizeBytes = p.newPartitionInfo.size,
                    opCount   = p.operationsList.size
                )
            }.sortedBy { it.name }
        }

    // ── Step 4: extract ───────────────────────────────────────────────────────

    suspend fun extractToFile(
        session: Session,
        partitionName: String,
        file: java.io.File,
        onProgress: suspend (ExtractionState) -> Unit = {}
    ): ExtractionState = withContext(Dispatchers.IO) {
        val resume = stateStore.load(partitionName)

        // If resuming, open in append mode.
        val outputStream = java.io.FileOutputStream(file, resume != null)

        outputStream.use { os ->
            extract(session, partitionName, os, onProgress)
        }
    }

    suspend fun extract(
        session: Session,
        partitionName: String,
        output: java.io.OutputStream,
        onProgress: suspend (ExtractionState) -> Unit = {}
    ): ExtractionState = withContext(Dispatchers.IO) {
        val resume = stateStore.load(partitionName)
        val finalState = if (session.extractor != null) {
            session.extractor.extract(
                partitionName = partitionName,
                manifest = session.manifest,
                output = output,
                resumeState = resume
            ) { done, total, opIdx ->
                val state = ExtractionState(partitionName, opIdx, done, total)
                stateStore.save(state)
                onProgress(state)
            }
        } else if (session.localPayloadOffset != null && session.localPayloadSize != null && session.localUri != null) {
            // Fast extraction skipping cache by using Offset IO mapping directly against the ZIP
            val baseReader = LocalRangeReader(context, session.localUri)
            val offsetReader = OffsetLocalByteReader(baseReader, session.localPayloadOffset, session.localPayloadSize)
            offsetReader.use { reader ->
                val blockSize = if (session.manifest.hasBlockSize()) session.manifest.blockSize.toLong() else 4096L
                val localExtractor = LocalPayloadExtractor(reader, session.header, blockSize)
                localExtractor.extract(
                    partitionName = partitionName,
                    manifest = session.manifest,
                    output = output,
                    resumeState = resume
                ) { done, total, opIdx ->
                    val state = ExtractionState(partitionName, opIdx, done, total)
                    stateStore.save(state)
                    onProgress(state)
                }
            }
        } else {
            val localPayloadFile = session.localPayloadFile
                ?: LocalMetadataReader.extractPayloadToCache(
                    context,
                    requireNotNull(session.localUri) { "Missing local OTA URI." }
                )
            LocalFileReader(localPayloadFile).use { reader ->
                val blockSize = if (session.manifest.hasBlockSize()) session.manifest.blockSize.toLong() else 4096L
                val localExtractor = LocalPayloadExtractor(reader, session.header, blockSize)
                localExtractor.extract(
                    partitionName = partitionName,
                    manifest = session.manifest,
                    output = output,
                    resumeState = resume
                ) { done, total, opIdx ->
                    val state = ExtractionState(partitionName, opIdx, done, total)
                    stateStore.save(state)
                    onProgress(state)
                }
            }
        }
        stateStore.clear(partitionName)
        finalState
    }

    fun clearExtractionState(partitionName: String) {
        stateStore.clear(partitionName)
    }

    private fun leShort(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset + 1].toInt() and 0xFF) shl 8) or (bytes[offset].toInt() and 0xFF)
}
