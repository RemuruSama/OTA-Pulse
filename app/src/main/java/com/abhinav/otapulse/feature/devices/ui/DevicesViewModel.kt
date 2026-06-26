package com.abhinav.otapulse.feature.devices.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhinav.otapulse.core.model.Device
import com.abhinav.otapulse.core.model.OtaUpdate
import com.abhinav.otapulse.core.model.RegionVariant
import com.abhinav.otapulse.feature.otatools.data.ArbLookupService
import com.abhinav.otapulse.feature.devices.domain.FetchOtaDetailsUseCase
import com.abhinav.otapulse.feature.devices.domain.GetDevicesUseCase
import com.abhinav.otapulse.feature.devices.domain.ToggleFavoriteUseCase
import com.abhinav.otapulse.feature.downloads.domain.DownloadRepository
import com.abhinav.otapulse.feature.downloads.domain.DeleteFileUseCase
import com.abhinav.otapulse.feature.downloads.domain.EnqueueDownloadUseCase
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OtaDetailsDialogData(
    val otaUpdate: OtaUpdate,
    val deviceName: String,
    val regionName: String
)

data class PartitionSelectDialogData(
    val url: String,
    val versionName: String,
    val partitions: List<com.abhinav.otapulse.ota.payload.PartitionInfo>
)

data class DevicesUiState(
    val isLoading: Boolean = true,
    val devices: List<Device> = emptyList(),
    val otaDetails: Map<String, Result<OtaUpdate>> = emptyMap(),
    val errorMessage: String? = null,
    val userMessage: String? = null,
    val searchQuery: String = "",
    val selectedBrand: String = "All",
    val pendingDownload: PendingDownload? = null,
    val showOtaDetailsDialog: OtaDetailsDialogData? = null,
    val showPartitionSelectDialog: PartitionSelectDialogData? = null,
    val isFetchingPartitions: Boolean = false,
    val isStartingExtraction: Boolean = false,
    val isSyncingCatalog: Boolean = false
)

data class PendingDownload(
    val otaUpdate: OtaUpdate,
    val deviceName: String,
    val regionName: String,
    val targetFile: java.io.File
)

@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val getDevicesUseCase: GetDevicesUseCase,
    private val fetchOtaDetailsUseCase: FetchOtaDetailsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val enqueueDownloadUseCase: EnqueueDownloadUseCase,
    private val downloadRepository: DownloadRepository,
    private val deleteFileUseCase: DeleteFileUseCase,
    private val deviceRepository: com.abhinav.otapulse.catalog.repository.DeviceRepository,
    private val arbLookupService: ArbLookupService,
    private val otaExtractor: com.abhinav.otapulse.ota.engine.OtaExtractor,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val workManager = androidx.work.WorkManager.getInstance(context)

    private val _uiState = MutableStateFlow(DevicesUiState())
    val uiState: StateFlow<DevicesUiState> = _uiState.asStateFlow()

    private var allDevices: List<Device> = emptyList()
    private var devicesCollectionJob: Job? = null

    init {
        loadDevices()
        observeSyncStatus()
    }

    private fun observeSyncStatus() {
        viewModelScope.launch {
            deviceRepository.isSyncing.collect { isSyncing ->
                _uiState.update { it.copy(isSyncingCatalog = isSyncing) }
            }
        }
    }

    fun forceSyncCatalog() {
        viewModelScope.launch {
            deviceRepository.syncCatalog()
        }
    }

    fun refreshDevices() {
        loadDevices()
    }

    fun startDownload(otaUpdate: OtaUpdate, deviceName: String, regionName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val targetFile = downloadRepository.getResolvedTargetFile(
                otaUpdate = otaUpdate,
                deviceName = deviceName,
                regionName = regionName
            )
            if (targetFile.exists()) {
                _uiState.update {
                    it.copy(
                        pendingDownload = PendingDownload(otaUpdate, deviceName, regionName, targetFile)
                    )
                }
            } else {
                enqueueDownloadUseCase(otaUpdate, deviceName, regionName)
            }
        }
    }

    fun confirmOverwriteDownload() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value.pendingDownload?.let { pending ->
                deleteFileUseCase(pending.targetFile)
                enqueueDownloadUseCase(pending.otaUpdate, pending.deviceName, pending.regionName)
                _uiState.update { it.copy(pendingDownload = null) }
            }
        }
    }

    fun cancelPendingDownload() {
        _uiState.update { it.copy(pendingDownload = null) }
    }

    fun deleteCustomDevice(deviceName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            deviceRepository.deleteCustomDevice(deviceName)
            loadDevices()
        }
    }

    private fun loadDevices() {
        devicesCollectionJob?.cancel()
        devicesCollectionJob = getDevicesUseCase()
            .onEach { devices ->
                allDevices = devices
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        devices = filterAndSortDevices(devices, it.searchQuery, it.selectedBrand)
                    )
                }
            }
            .catch { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
            .launchIn(viewModelScope)
    }

    fun fetchOtaDetails(device: Device, variant: RegionVariant) {
        viewModelScope.launch {
            val deviceKey = "${device.name}_${variant.displayName}"

            _uiState.update { currentState ->
                val updatedDevices = currentState.devices.map { d ->
                    if (d.name == device.name) d.copy(isLoadingDetails = true) else d
                }
                currentState.copy(
                    devices = updatedDevices
                )
            }

            val result = fetchOtaDetailsUseCase(device, variant)

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

            _uiState.update { currentState ->
                val updatedDevices = currentState.devices.map { d ->
                    if (d.name == device.name) d.copy(isLoadingDetails = false) else d
                }

                val dialogData = enrichedResult.getOrNull()?.let { OtaDetailsDialogData(it, device.name, variant.displayName) }

                currentState.copy(
                    devices = updatedDevices,
                    otaDetails = currentState.otaDetails + (deviceKey to enrichedResult),
                    showOtaDetailsDialog = dialogData
                )
            }
        }
    }

    fun showOtaDetailsFromHistory(entry: com.abhinav.otapulse.core.model.OtaHistoryEntry) {
        val isHomeUpdateRecord = entry.deviceName == "Custom Device" || entry.deviceName.startsWith("Custom|") || entry.deviceName.equals("This Device", ignoreCase = true)
        val resolvedDeviceName = if (entry.deviceName.startsWith("Custom|")) {
            entry.deviceName.removePrefix("Custom|").ifBlank {
                (entry.otaUpdate.versionName ?: entry.otaUpdate.componentVersion).substringBefore("_")
            }
        } else if (entry.deviceName == "Custom Device") {
            (entry.otaUpdate.versionName ?: entry.otaUpdate.componentVersion).substringBefore("_").ifBlank { "Custom Device" }
        } else {
            entry.deviceName
        }
        val regionForDialog = if (isHomeUpdateRecord) "" else entry.region

        _uiState.update { state ->
            state.copy(showOtaDetailsDialog = OtaDetailsDialogData(entry.otaUpdate, resolvedDeviceName, regionForDialog))
        }
    }

    fun toggleFavorite(deviceName: String) {
        viewModelScope.launch {
            val device = allDevices.find { it.name == deviceName }
            val wasFavorite = device?.isFavorite ?: false

            toggleFavoriteUseCase(deviceName)

            // Logic: if it WAS favorite, it is now removed. If it WAS NOT, it is now added.
            val message = if (wasFavorite) "$deviceName removed from ❤️" else "$deviceName added to ❤️"

            // After toggling, immediately reload the device list to get the new favorite state.
            loadDevices()

            _uiState.update { it.copy(userMessage = message) }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                devices = filterAndSortDevices(allDevices, query, it.selectedBrand)
            )
        }
    }

    fun onBrandSelected(brand: String) {
        _uiState.update {
            it.copy(
                selectedBrand = brand,
                devices = filterAndSortDevices(allDevices, it.searchQuery, brand)
            )
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun clearOtaDetailsDialog() {
        _uiState.update { it.copy(showOtaDetailsDialog = null) }
    }

    fun clearPartitionSelectDialog() {
        _uiState.update { it.copy(showPartitionSelectDialog = null) }
    }

    fun fetchExtractablePartitions(ota: OtaUpdate) {
        val url = ota.downloadUrl
        val versionName = ota.versionName ?: "Unknown"
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingPartitions = true) }
            try {
                val session = otaExtractor.open(url)
                val partitions = otaExtractor.listPartitions(session)

                if (partitions.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            isFetchingPartitions = false,
                            showPartitionSelectDialog = PartitionSelectDialogData(
                                url = url,
                                versionName = versionName,
                                partitions = partitions
                            )
                        )
                    }
                } else {
                    throw Exception("No partitions found in payload.")
                }
            } catch (e: Exception) {
                Log.e("DevicesViewModel", "Error fetching partitions", e)
                _uiState.update {
                    it.copy(
                        isFetchingPartitions = false,
                        userMessage = "Could not parse partitions: ${e.message}"
                    )
                }
            }
        }
    }

    fun extractPartitions(
        url: String,
        versionName: String,
        partitionNames: List<String>,
        regionName: String? = null
    ): java.util.UUID {
        val data = androidx.work.workDataOf(
            com.abhinav.otapulse.arb.worker.PartitionExtractorWorker.KEY_URL to url,
            com.abhinav.otapulse.arb.worker.PartitionExtractorWorker.KEY_VERSION_NAME to versionName,
            com.abhinav.otapulse.arb.worker.PartitionExtractorWorker.KEY_PARTITION_NAMES to partitionNames.toTypedArray(),
            com.abhinav.otapulse.arb.worker.PartitionExtractorWorker.KEY_REGION_NAME to regionName
        )

        val request = androidx.work.OneTimeWorkRequestBuilder<com.abhinav.otapulse.arb.worker.PartitionExtractorWorker>()
            .setInputData(data)
            .addTag("extraction_${partitionNames.joinToString("_")}")
            .build()

        workManager.enqueue(request)
        val msg = if (partitionNames.size == 1) "Starting extraction of ${partitionNames.first()}.img..." else "Starting extraction of ${partitionNames.size} partitions..."
        _uiState.update { it.copy(isStartingExtraction = true, userMessage = msg) }
        return request.id
    }

    fun clearStartingExtraction() {
        _uiState.update { it.copy(isStartingExtraction = false) }
    }

    fun cancelPartitionExtraction(workId: java.util.UUID, partitionNamesStr: String) {
        workManager.cancelWorkById(workId)
        val msg = if (!partitionNamesStr.contains("_")) "Cancelling extraction of $partitionNamesStr.img..." else "Cancelling extraction of partitions..."
        _uiState.update {
            it.copy(
                isStartingExtraction = false,
                userMessage = msg
            )
        }
    }

    private fun filterAndSortDevices(devices: List<Device>, query: String, brand: String): List<Device> {
        val filteredByBrand = if (brand == "All") devices else devices.filter { it.name.contains(brand, ignoreCase = true) }
        val filteredBySearch = if (query.isBlank()) filteredByBrand else {
            filteredByBrand.filter { device ->
                device.name.contains(query, ignoreCase = true) ||
                        device.firmwareGroups.values.flatten().any { 
                            it.productModel.contains(query, ignoreCase = true) || 
                            it.productName.contains(query, ignoreCase = true) 
                        }
            }
        }
        return filteredBySearch.sortedByDescending { it.isFavorite }
    }
}
