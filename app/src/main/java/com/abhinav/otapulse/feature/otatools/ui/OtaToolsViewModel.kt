package com.abhinav.otapulse.feature.otatools.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhinav.otapulse.feature.downloads.domain.DownloadRepository
import com.abhinav.otapulse.core.model.Device
import com.abhinav.otapulse.core.model.OtaUpdate
import com.abhinav.otapulse.core.model.RegionVariant
import com.abhinav.otapulse.feature.otatools.data.ArbLookupService
import com.abhinav.otapulse.feature.devices.domain.FetchOtaDetailsUseCase
import com.abhinav.otapulse.ota.payload.PartitionInfo
import com.abhinav.otapulse.core.network.OtaResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OtaToolsUiState(
    val isLoading: Boolean = false,
    val isCheckingArb: Boolean = false,
    val result: Result<OtaUpdate>? = null,
    val deviceName: String = "",
    val regionName: String = "",
    val showOtaDetailsDialog: OtaUpdate? = null,
    val isFetchingPartitions: Boolean = false,
    val isStartingExtraction: Boolean = false,
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
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val workManager = androidx.work.WorkManager.getInstance(context)

    private val _uiState = MutableStateFlow(OtaToolsUiState())
    val uiState: StateFlow<OtaToolsUiState> = _uiState.asStateFlow()

    fun sendRequest(
        model: String,
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
        gray: Int = 0
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, result = null) }

            val dummyDevice = Device(
                name = "Custom Device",
                ruiVersion = ruiVersion,
                imei = imei,
                beta = beta,
                imageUrl = null,
                imageResId = null,
                firmwareGroups = emptyMap(),
                isFavorite = false,
                isCustom = true
            )

            // CRITICAL: Decouple 'Region Variant' (NVID) from 'Target Server'
            val regionVariant = RegionVariant(
                displayName = region,
                productModel = model,
                firmwareVersion = otaVersion,
                region = server, // Use the manually selected server
                nvId = nvId,
                language = language
            )

            val result = fetchOtaDetailsUseCase(dummyDevice, regionVariant, reqMode, gray)

            // Enrich with verified ARB data from community database
            val appSettingsPrefs = context.getSharedPreferences(com.abhinav.otapulse.feature.settings.SettingsFragment.APP_SETTINGS_PREFS, android.content.Context.MODE_PRIVATE)
            val isArbDetectionEnabled = appSettingsPrefs.getBoolean(com.abhinav.otapulse.feature.settings.SettingsFragment.PREF_ARB_DETECTION_ENABLED, true)

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
                    showOtaDetailsDialog = enrichedResult.getOrNull()
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
        gray: Int = 0
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, result = null, userMessage = null) }

            val dummyDevice = Device(
                name = "Custom Device",
                ruiVersion = ruiVersion,
                imei = imei,
                beta = beta,
                imageUrl = null,
                imageResId = null,
                firmwareGroups = emptyMap(),
                isFavorite = false,
                isCustom = true
            )

            val appSettingsPrefs = context.getSharedPreferences(
                com.abhinav.otapulse.feature.settings.SettingsFragment.APP_SETTINGS_PREFS,
                android.content.Context.MODE_PRIVATE
            )
            val isArbDetectionEnabled = appSettingsPrefs.getBoolean(
                com.abhinav.otapulse.feature.settings.SettingsFragment.PREF_ARB_DETECTION_ENABLED,
                true
            )

            var finalResult: Result<OtaUpdate>? = null
            var matchedServer: String? = null

            for (server in servers) {
                val regionVariant = RegionVariant(
                    displayName = region,
                    productModel = model,
                    firmwareVersion = otaVersion,
                    region = server,
                    nvId = nvId,
                    language = language
                )

                val result = fetchOtaDetailsUseCase(dummyDevice, regionVariant, reqMode, gray)
                val enrichedResult = result.map { ota ->
                    if (isArbDetectionEnabled) {
                        val arbInfo = arbLookupService.lookupByUrl(ota.downloadUrl)
                        if (arbInfo != null) ota.copy(arbStatus = arbInfo.toDisplayString()) else ota
                    } else {
                        ota.copy(arbStatus = "N/A")
                    }
                }

                if (enrichedResult.isSuccess) {
                    finalResult = enrichedResult
                    matchedServer = server
                    break
                }

                finalResult = enrichedResult
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    result = finalResult,
                    deviceName = dummyDevice.name,
                    regionName = if (matchedServer != null) {
                        "${region} (Server: $matchedServer)"
                    } else {
                        "${region} (Searched: ${servers.joinToString(", ")})"
                    }
                )
            }
        }
    }

    fun fetchExtractablePartitions(ota: OtaUpdate) {
        fetchExtractablePartitions(
            source = ota.url,
            versionName = ota.versionName ?: "Custom",
            sourceLabel = "Manual query result"
        )
    }

    fun fetchExtractablePartitions(source: String, versionName: String, sourceLabel: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingPartitions = true) }
            try {
                val session = otaExtractor.open(source)
                val partitions = otaExtractor.listPartitions(session)
                _uiState.update {
                    it.copy(
                        isFetchingPartitions = false,
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
                _uiState.update {
                    it.copy(
                        isFetchingPartitions = false,
                        userMessage = "Could not parse partitions: ${e.message}"
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

            _uiState.update { it.copy(isCheckingArb = true, arbCheckResult = null) }

            runCatching { arbLookupService.lookup(trimmedSource) }
                .onSuccess { arbInfo ->
                    if (arbInfo != null) {
                        _uiState.update {
                            it.copy(
                                isCheckingArb = false,
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
                                userMessage = "Could not extract ARB metadata from this package."
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isCheckingArb = false,
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

    fun extractPartition(source: String, versionName: String, partitionName: String, regionName: String? = null): java.util.UUID {
        val data = androidx.work.workDataOf(
            com.abhinav.otapulse.arb.worker.PartitionExtractorWorker.KEY_SOURCE to source,
            com.abhinav.otapulse.arb.worker.PartitionExtractorWorker.KEY_URL to source,
            com.abhinav.otapulse.arb.worker.PartitionExtractorWorker.KEY_VERSION_NAME to versionName,
            com.abhinav.otapulse.arb.worker.PartitionExtractorWorker.KEY_PARTITION_NAME to partitionName,
            com.abhinav.otapulse.arb.worker.PartitionExtractorWorker.KEY_REGION_NAME to regionName
        )

        val request = androidx.work.OneTimeWorkRequestBuilder<com.abhinav.otapulse.arb.worker.PartitionExtractorWorker>()
            .setInputData(data)
            .addTag("extraction_$partitionName")
            .build()

        workManager.enqueue(request)
        _uiState.update { it.copy(isStartingExtraction = true) }
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

    fun cancelPartitionExtraction(workId: java.util.UUID, partitionName: String) {
        workManager.cancelWorkById(workId)
        _uiState.update { it.copy(isStartingExtraction = false) }
    }

    fun clearOtaDetailsDialog() {
        _uiState.update { it.copy(showOtaDetailsDialog = null) }
    }

    fun startDownload(otaUpdate: OtaUpdate, deviceName: String, regionName: String) {
        viewModelScope.launch {
            downloadRepository.enqueueDownload(otaUpdate, deviceName, regionName)
        }
    }
}
