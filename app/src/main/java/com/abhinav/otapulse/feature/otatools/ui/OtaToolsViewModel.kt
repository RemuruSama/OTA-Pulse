package com.abhinav.otapulse.feature.otatools.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhinav.otapulse.feature.downloads.domain.DownloadRepository
import com.abhinav.otapulse.core.model.Device
import com.abhinav.otapulse.core.model.OtaUpdate
import com.abhinav.otapulse.core.model.RegionVariant
import com.abhinav.otapulse.catalog.model.RegionData
import com.abhinav.otapulse.feature.otatools.data.ArbLookupService
import com.abhinav.otapulse.feature.devices.domain.FetchOtaDetailsUseCase
import com.abhinav.otapulse.ota.payload.PartitionInfo
import com.abhinav.otapulse.core.network.OtaResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OtaToolsUiState(
    val isLoading: Boolean = false,
    val isCheckingArb: Boolean = false,
    val checkingArbSource: String? = null,
    val result: Result<OtaUpdate>? = null,
    val multiResults: List<OtaUpdate>? = null,
    val deviceName: String = "",
    val regionName: String = "",
    val showOtaDetailsDialog: OtaUpdate? = null,
    val isFetchingPartitions: Boolean = false,
    val fetchingSource: String? = null,
    val isStartingExtraction: Boolean = false,
    val activeExtractionWorkId: java.util.UUID? = null,
    val activeExtractionNames: List<String> = emptyList(),
    val showPartitionSelectDialog: ManualQuerySelectDialogData? = null,
    val resolverResult: ResolvedLinkUiState? = null,
    val arbCheckResult: ArbCheckUiState? = null,
    val userMessage: String? = null
)

data class ManualQuerySelectDialogData(
    val source: String,
    val versionName: String,
    val sourceLabel: String,
    val partitions: List<PartitionInfo>
)

data class ResolvedLinkUiState(
    val originalUrl: String,
    val resolvedUrl: String,
    val fileName: String?
)

data class ArbCheckUiState(
    val source: String,
    val sourceLabel: String,
    val displayName: String,
    val arbInfo: ArbLookupService.ArbInfo
)

@HiltViewModel
class OtaToolsViewModel @Inject constructor(
    private val fetchOtaDetailsUseCase: FetchOtaDetailsUseCase,
    private val downloadRepository: DownloadRepository,
    private val arbLookupService: ArbLookupService,
    private val otaExtractor: com.abhinav.otapulse.ota.engine.OtaExtractor,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val appSettingsPreferences: com.abhinav.otapulse.core.preferences.AppSettingsPreferences
) : ViewModel() {

    private val workManager = androidx.work.WorkManager.getInstance(context)

    private val _uiState = MutableStateFlow(OtaToolsUiState())
    val uiState: StateFlow<OtaToolsUiState> = _uiState.asStateFlow()

    fun sendRequest(
        model: String,
        displayDeviceName: String? = null,
        otaVersion: String,
        ruiVersion: Int,
        region: String,
        server: String,
        regionsArray: Array<String>,
        // Advanced Options
        imei: String = "0",
        beta: Boolean = false,
        nvId: String? = null,
        language: String? = "en-EN",
        reqMode: String? = "manual",
        gray: Int = 0,
        autoShowDialog: Boolean = true
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, result = null) }

            val dummyDevice = Device(
                name = "Custom|" + (displayDeviceName?.takeIf { it.isNotBlank() } ?: model),
                ruiVersion = ruiVersion,
                imei = imei,
                beta = beta,
                imageResId = null,
                firmwareGroups = emptyMap(),
                isFavorite = false,
                isCustom = true
            )

            // CRITICAL: Decouple 'Region Variant' (NVID) from 'Target Server'
            // Resolve NV ID from RegionData if user didn't provide one
            val resolvedNvId = nvId ?: RegionData.regions.find {
                it.displayName.equals(region, ignoreCase = true)
            }?.nvid ?: ""

            val fullFirmwareVersion = buildFirmwareVersionString(model, resolvedNvId, otaVersion)

            val effectiveProductName = displayDeviceName?.takeIf { it.isNotBlank() } ?: model
            val regionVariant = RegionVariant(
                displayName = region,
                productModel = model,
                productName = effectiveProductName,
                firmwareVersion = fullFirmwareVersion,
                region = server, // Use the manually selected server
                nvId = resolvedNvId.ifEmpty { null },
                language = language
            )

            val result = fetchOtaDetailsUseCase(dummyDevice, regionVariant, reqMode, gray)

            // Enrich with verified ARB data from community database
            val isArbDetectionEnabled = appSettingsPreferences.getAppSettings().arbDetection

            val enrichedResult = result.map { ota ->
                if (isArbDetectionEnabled) {
                    val arbInfo = arbLookupService.lookupByUrl(ota.downloadUrl)
                    if (arbInfo != null) {
                        ota.copy(arbStatus = arbInfo.toDisplayString())
                    } else ota
                } else {
                    ota.copy(arbStatus = "N/A")
                }
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    result = enrichedResult,
                    deviceName = dummyDevice.name,
                    regionName = "${regionVariant.displayName} (Server: $server)",
                    showOtaDetailsDialog = if (autoShowDialog) enrichedResult.getOrNull() else null
                )
            }
        }
    }

    fun sendRequestAcrossServers(
        model: String,
        otaVersion: String,
        ruiVersion: Int,
        region: String,
        servers: List<String>,
        regionsArray: Array<String>,
        imei: String = "0",
        beta: Boolean = false,
        nvId: String? = null,
        language: String? = "en-EN",
        reqMode: String? = "manual",
        gray: Int = 0,
        autoShowDialog: Boolean = true,
        displayDeviceName: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, result = null, userMessage = null) }

            val dummyDevice = Device(
                name = "Custom Device",
                ruiVersion = ruiVersion,
                imei = imei,
                beta = beta,
                imageResId = null,
                firmwareGroups = emptyMap(),
                isFavorite = false,
                isCustom = true
            )

            val isArbDetectionEnabled = appSettingsPreferences.getAppSettings().arbDetection

            // Resolve NV ID from RegionData if user didn't provide one
            val resolvedNvId = nvId ?: RegionData.regions.find {
                it.displayName.equals(region, ignoreCase = true)
            }?.nvid ?: ""

            val fullFirmwareVersion = buildFirmwareVersionString(model, resolvedNvId, otaVersion)
            val effectiveProductName = displayDeviceName?.takeIf { it.isNotBlank() } ?: model

            // ── Fan-out to ALL servers in parallel, then pick the latest version ──
            // This guarantees that a stale server can never hide a newer build
            // that is already live on another server.
            data class ServerResult(val server: String, val ota: OtaUpdate)

            val successfulResults: List<ServerResult> = coroutineScope {
                servers.map { server ->
                    async {
                        val regionVariant = RegionVariant(
                            displayName = region,
                            productModel = model,
                            productName = effectiveProductName,
                            firmwareVersion = fullFirmwareVersion,
                            region = server,
                            nvId = resolvedNvId.ifEmpty { null },
                            language = language
                        )
                        runCatching {
                            fetchOtaDetailsUseCase(dummyDevice, regionVariant, reqMode, gray)
                                .getOrThrow()
                                .let { ota ->
                                    val enriched = if (isArbDetectionEnabled) {
                                        val arbInfo = arbLookupService.lookupByUrl(ota.downloadUrl)
                                        if (arbInfo != null) ota.copy(arbStatus = arbInfo.toDisplayString()) else ota
                                    } else {
                                        ota.copy(arbStatus = "N/A")
                                    }
                                    ServerResult(server, enriched)
                                }
                        }.getOrNull()
                    }
                }.mapNotNull { it.await() }
            }

            if (successfulResults.isEmpty()) {
                // All servers failed — surface the last error for UX feedback
                val lastError = runCatching {
                    val regionVariant = RegionVariant(
                        displayName = region,
                        productModel = model,
                        productName = model,
                        firmwareVersion = fullFirmwareVersion,
                        region = servers.last(),
                        nvId = resolvedNvId.ifEmpty { null },
                        language = language
                    )
                    fetchOtaDetailsUseCase(dummyDevice, regionVariant, reqMode, gray)
                }.getOrElse { Result.failure<OtaUpdate>(it) }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        result = lastError as? Result<OtaUpdate>
                            ?: Result.failure(Exception("No update found on any server.")),
                        deviceName = dummyDevice.name,
                        regionName = "$region (Searched: ${servers.joinToString(", ")})"
                    )
                }
                return@launch
            }

            // Pick the result with the highest OTA version string.
            // realOtaVersion encodes a build timestamp in its trailing segment
            // (e.g. CPH2487_11.H.54_3540_202602261724), so lexicographic max
            // naturally selects the most recent build.
            val best = successfulResults.maxWith(compareBy { it.ota.resolvedOtaVersion() })

            _uiState.update {
                it.copy(
                    isLoading = false,
                    result = Result.success(best.ota),
                    deviceName = dummyDevice.name,
                    regionName = "$region (Server: ${best.server})",
                    showOtaDetailsDialog = if (autoShowDialog) best.ota else null
                )
            }
        }
    }

    fun sendRequestAcrossVersionsAndServers(
        model: String,
        displayDeviceName: String? = null,
        baseOtaVersion: String, // E.g., RMX3840_11 or whatever base is before the letter
        ruiVersion: Int,
        region: String,
        servers: List<String>,
        letters: List<String>, // E.g. listOf("A", "C", "F", "H", "J")
        reqModes: List<String> = listOf("manual", "server_auto", "client_auto", "taste"),
        imei: String = "0",
        beta: Boolean = false,
        nvId: String? = null,
        language: String? = "en-EN",
        reqMode: String? = "manual",
        gray: Int = 0,
        autoShowDialog: Boolean = true
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, result = null, multiResults = null, userMessage = null) }

            val dummyDevice = Device(
                name = "Custom|" + (displayDeviceName?.takeIf { it.isNotBlank() } ?: model),
                ruiVersion = ruiVersion,
                imei = imei,
                beta = beta,
                imageResId = null,
                firmwareGroups = emptyMap(),
                isFavorite = false,
                isCustom = true
            )

            val isArbDetectionEnabled = appSettingsPreferences.getAppSettings().arbDetection

            // Resolve NV ID from RegionData if user didn't provide one
            val resolvedNvId = nvId ?: RegionData.regions.find {
                it.displayName.equals(region, ignoreCase = true)
            }?.nvid ?: ""

            data class ServerResult(val server: String, val letter: String, val mode: String, val ota: OtaUpdate)

            val successfulResults: List<ServerResult> = coroutineScope {
                letters.flatMap { letter ->
                    val otaVersion = buildFirmwareVersionString(model, resolvedNvId, letter)
                    
                    servers.flatMap { server ->
                        reqModes.map { mode ->
                            async {
                                val effectiveProductName = displayDeviceName?.takeIf { it.isNotBlank() } ?: model
                                val regionVariant = RegionVariant(
                                    displayName = region,
                                    productModel = model,
                                    productName = effectiveProductName,
                                    firmwareVersion = otaVersion,
                                    region = server,
                                    nvId = resolvedNvId.ifEmpty { null },
                                    language = language
                                )
                                runCatching {
                                    fetchOtaDetailsUseCase(dummyDevice, regionVariant, mode, gray)
                                        .getOrThrow()
                                        .let { ota ->
                                            val enriched = if (isArbDetectionEnabled) {
                                                val arbInfo = arbLookupService.lookupByUrl(ota.downloadUrl)
                                                if (arbInfo != null) ota.copy(arbStatus = arbInfo.toDisplayString()) else ota
                                            } else {
                                                ota.copy(arbStatus = "N/A")
                                            }
                                            ServerResult(server, letter, mode, enriched)
                                        }
                                }.getOrNull()
                            }
                        }
                    }
                }.mapNotNull { it.await() }
            }

            if (successfulResults.isEmpty()) {
                // All failed
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        result = Result.failure(Exception("No update found across any branch letter, server, or request mode.")),
                        multiResults = emptyList(),
                        deviceName = dummyDevice.name,
                        regionName = "$region (Searched: ${servers.size} servers × ${letters.size} branches × ${reqModes.size} modes)"
                    )
                }
                return@launch
            }

            // Group by branch letter and build version/filename so exact identical updates across different CDNs (otafs vs gauss) aren't duplicated,
            // while preserving distinct builds across branches.
            val bestResults = successfulResults
                .groupBy { "${it.letter}_${it.ota.realVersionName ?: it.ota.versionName?.substringBefore(" [") ?: it.ota.downloadUrl.substringAfterLast("/")}" }
                .map { (_, results) ->
                    val first = results.first()
                    val allModes = results.map { it.mode.uppercase() }.distinct().joinToString(" • ")
                    val allServers = results.map { it.server.uppercase() }.distinct().joinToString(", ")
                    val baseTitle = (first.ota.versionName ?: first.ota.realVersionName ?: first.ota.realOtaVersion ?: "Branch ${first.letter}").substringBefore(" [").trim()
                    val annotatedTitle = "$baseTitle [Branch ${first.letter} • Mode: $allModes • Server: $allServers]"
                    first.ota.copy(
                        versionName = annotatedTitle,
                        realVersionName = annotatedTitle
                    )
                }
                .sortedByDescending { it.resolvedOtaVersion() }

            val bestOverall = bestResults.maxWithOrNull(compareBy { it.resolvedOtaVersion() })

            _uiState.update {
                it.copy(
                    isLoading = false,
                    result = bestOverall?.let { best -> Result.success(best) },
                    multiResults = bestResults,
                    deviceName = dummyDevice.name,
                    regionName = "$region (Full Matrix: ${reqModes.size} modes × ${servers.size} servers × ${letters.size} branches)",
                    showOtaDetailsDialog = null // Don't auto-show if we have multiple results
                )
            }
        }
    }

    /**
     * Returns the canonical OTA version string used for comparing builds across servers.
     * Prefers [OtaUpdate.realOtaVersion] and falls back to [OtaUpdate.componentVersion].
     */
    private fun OtaUpdate.resolvedOtaVersion(): String =
        realOtaVersion
            ?: componentVersion.substringBefore(".")
                .let { base -> if (base.count { it == '_' } >= 3) base else componentVersion }

    fun fetchExtractablePartitions(ota: OtaUpdate) {
        fetchExtractablePartitions(
            source = ota.url,
            versionName = ota.versionName ?: "Custom",
            sourceLabel = "Manual query result"
        )
    }

    fun fetchExtractablePartitions(source: String, versionName: String, sourceLabel: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingPartitions = true, fetchingSource = source) }
            try {
                val session = otaExtractor.open(source)
                val partitions = otaExtractor.listPartitions(session)
                _uiState.update {
                    it.copy(
                        isFetchingPartitions = false,
                        fetchingSource = null,
                        showPartitionSelectDialog = ManualQuerySelectDialogData(
                            source = source,
                            versionName = versionName,
                            sourceLabel = sourceLabel,
                            partitions = partitions
                        )
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("OtaToolsViewModel", "Error fetching partitions", e)
                val errorMessage = when (e) {
                    is java.lang.IllegalArgumentException -> e.message ?: "Invalid OTA package structure."
                    else -> e.message ?: "Unknown error"
                }
                _uiState.update {
                    it.copy(
                        isFetchingPartitions = false,
                        fetchingSource = null,
                        userMessage = "Could not parse partitions: $errorMessage"
                    )
                }
            }
        }
    }

    fun resolveLink(inputUrl: String) {
        viewModelScope.launch {
            val trimmedUrl = inputUrl.trim()
            if (trimmedUrl.isBlank()) {
                _uiState.update { it.copy(userMessage = "Paste a link first.") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }

            runCatching { OtaResolver.resolveUrl(trimmedUrl) }
                .onSuccess { resolved ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            resolverResult = ResolvedLinkUiState(
                                originalUrl = trimmedUrl,
                                resolvedUrl = resolved.url,
                                fileName = resolved.contentDispositionFileName
                            )
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            userMessage = "Could not resolve link: ${error.message}"
                        )
                    }
                }
        }
    }

    fun checkArb(source: String, sourceLabel: String, displayName: String) {
        viewModelScope.launch {
            val trimmedSource = source.trim()
            if (trimmedSource.isBlank()) {
                _uiState.update { it.copy(userMessage = "Paste a link or choose a ZIP first.") }
                return@launch
            }

            _uiState.update { it.copy(isCheckingArb = true, checkingArbSource = source, arbCheckResult = null) }

            runCatching { arbLookupService.lookup(trimmedSource) }
                .onSuccess { arbInfo ->
                    if (arbInfo != null) {
                        _uiState.update {
                            it.copy(
                                isCheckingArb = false,
                                checkingArbSource = null,
                                arbCheckResult = ArbCheckUiState(
                                    source = trimmedSource,
                                    sourceLabel = sourceLabel,
                                    displayName = displayName,
                                    arbInfo = arbInfo
                                )
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isCheckingArb = false,
                                checkingArbSource = null,
                                userMessage = "Could not extract ARB metadata from this package."
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isCheckingArb = false,
                            checkingArbSource = null,
                            userMessage = "Could not check ARB: ${error.message}"
                        )
                    }
                }
        }
    }

    fun clearResolverResult() {
        _uiState.update { it.copy(resolverResult = null) }
    }

    fun clearArbCheckResult() {
        _uiState.update { it.copy(arbCheckResult = null) }
    }

    fun extractPartitions(source: String, versionName: String, partitionNames: List<String>, regionName: String? = null): java.util.UUID {
        val data = androidx.work.workDataOf(
            com.abhinav.otapulse.arb.worker.PartitionExtractorWorker.KEY_SOURCE to source,
            com.abhinav.otapulse.arb.worker.PartitionExtractorWorker.KEY_URL to source,
            com.abhinav.otapulse.arb.worker.PartitionExtractorWorker.KEY_VERSION_NAME to versionName,
            com.abhinav.otapulse.arb.worker.PartitionExtractorWorker.KEY_PARTITION_NAMES to partitionNames.toTypedArray(),
            com.abhinav.otapulse.arb.worker.PartitionExtractorWorker.KEY_REGION_NAME to regionName
        )

        val tagNames = partitionNames.joinToString("_")
        val request = androidx.work.OneTimeWorkRequestBuilder<com.abhinav.otapulse.arb.worker.PartitionExtractorWorker>()
            .setInputData(data)
            .addTag("extraction_$tagNames")
            .build()

        workManager.enqueue(request)
        _uiState.update { it.copy(
            isStartingExtraction = true,
            activeExtractionWorkId = request.id,
            activeExtractionNames = partitionNames
        ) }
        return request.id
    }

    fun clearPartitionSelectDialog() {
        _uiState.update { it.copy(showPartitionSelectDialog = null) }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun clearStartingExtraction() {
        _uiState.update { it.copy(isStartingExtraction = false) }
    }

    fun clearActiveExtraction() {
        _uiState.update { it.copy(isStartingExtraction = false, activeExtractionWorkId = null, activeExtractionNames = emptyList()) }
    }

    fun cancelPartitionExtraction(workId: java.util.UUID, partitionName: String) {
        workManager.cancelWorkById(workId)
        _uiState.update { it.copy(isStartingExtraction = false, activeExtractionWorkId = null, activeExtractionNames = emptyList()) }
    }

    fun showOtaDetails(ota: OtaUpdate) {
        _uiState.update { it.copy(showOtaDetailsDialog = ota) }
    }

    fun clearOtaDetailsDialog() {
        _uiState.update { it.copy(showOtaDetailsDialog = null) }
    }

    fun startDownload(otaUpdate: OtaUpdate, deviceName: String, regionName: String, isFromHomeUpdate: Boolean = false) {
        viewModelScope.launch {
            downloadRepository.enqueueDownload(otaUpdate, deviceName, regionName, isFromHomeUpdate)
        }
    }

    private fun buildFirmwareVersionString(modelInput: String, nvIdInput: String?, versionOrLetter: String): String {
        if (versionOrLetter.contains("_11.") || versionOrLetter.count { it == '_' } >= 3) {
            return versionOrLetter
        }

        val suffixesToStrip = listOf("EEA", "IN", "RU", "TR", "CN", "EU", "TW", "MEA", "SA", "SG", "TH", "LATAM", "BR", "MY", "ID", "KZ", "OCA", "VN", "GLO").distinct()
        var baseModel = modelInput.substringBefore("_11").substringBefore(".")
        for (suffix in suffixesToStrip) {
            if (baseModel.endsWith(suffix, ignoreCase = true)) {
                baseModel = baseModel.dropLast(suffix.length)
                break
            }
        }
        val cleanBase = baseModel.replace(Regex("NV[0-9A-Z]{2}$", RegexOption.IGNORE_CASE), "")

        val nvSuffix = if (nvIdInput != null && nvIdInput.trim().startsWith("NV", ignoreCase = true) && nvIdInput.trim().length == 4) {
            nvIdInput.trim().uppercase()
        } else {
            ""
        }

        val letter = if (versionOrLetter.length == 1 && versionOrLetter[0].isLetter()) {
            versionOrLetter.uppercase()
        } else {
            versionOrLetter.takeIf { it.isNotBlank() } ?: "A"
        }

        return "${cleanBase}${nvSuffix}_11.${letter}.01_0001_100001010000"
    }
}
