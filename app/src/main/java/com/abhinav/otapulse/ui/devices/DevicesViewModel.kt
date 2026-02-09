package com.abhinav.otapulse.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhinav.otapulse.data.local.CustomDeviceManager
import com.abhinav.otapulse.domain.model.Device
import com.abhinav.otapulse.domain.model.OtaUpdate
import com.abhinav.otapulse.domain.model.RegionVariant
import com.abhinav.otapulse.domain.usecase.EnqueueDownloadUseCase
import com.abhinav.otapulse.domain.usecase.FetchOtaDetailsUseCase
import com.abhinav.otapulse.domain.usecase.GetDevicesUseCase
import com.abhinav.otapulse.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DevicesUiState(
    val isLoading: Boolean = true,
    val devices: List<Device> = emptyList(),
    val otaDetails: Map<String, Result<OtaUpdate>> = emptyMap(),
    val errorMessage: String? = null,
    val userMessage: String? = null,
    val searchQuery: String = "",
    val selectedBrand: String = "All",
    val pendingDownload: PendingDownload? = null
)

data class PendingDownload(
    val otaUpdate: OtaUpdate,
    val device: Device,
    val variant: RegionVariant,
    val targetFile: java.io.File
)

@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val getDevicesUseCase: GetDevicesUseCase,
    private val fetchOtaDetailsUseCase: FetchOtaDetailsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val enqueueDownloadUseCase: EnqueueDownloadUseCase,
    private val getTargetFileUseCase: com.abhinav.otapulse.domain.usecase.GetTargetFileUseCase,
    private val deleteFileUseCase: com.abhinav.otapulse.domain.usecase.DeleteFileUseCase,
    private val otaRepository: com.abhinav.otapulse.domain.repository.OtaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DevicesUiState())
    val uiState: StateFlow<DevicesUiState> = _uiState.asStateFlow()

    private var allDevices: List<Device> = emptyList()

    init {
        loadDevices()
    }

    fun refreshDevices() {
        loadDevices()
    }

    fun startDownload(otaUpdate: OtaUpdate, device: Device, variant: RegionVariant) {
        viewModelScope.launch(Dispatchers.IO) {
            val targetFile = getTargetFileUseCase(otaUpdate, device, variant)
            if (targetFile.exists()) {
                _uiState.update {
                    it.copy(
                        pendingDownload = PendingDownload(otaUpdate, device, variant, targetFile)
                    )
                }
            } else {
                enqueueDownloadUseCase(otaUpdate, device, variant)
            }
        }
    }

    fun confirmOverwriteDownload() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value.pendingDownload?.let { pending ->
                deleteFileUseCase(pending.targetFile)
                enqueueDownloadUseCase(pending.otaUpdate, pending.device, pending.variant)
                _uiState.update { it.copy(pendingDownload = null) }
            }
        }
    }

    fun cancelPendingDownload() {
        _uiState.update { it.copy(pendingDownload = null) }
    }

    fun deleteCustomDevice(deviceName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            otaRepository.deleteCustomDevice(deviceName)
            loadDevices()
        }
    }

    private fun loadDevices() {
        getDevicesUseCase()
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

            _uiState.update { currentState ->
                val updatedDevices = currentState.devices.map { d ->
                    if (d.name == device.name) d.copy(isLoadingDetails = false) else d
                }
                currentState.copy(
                    devices = updatedDevices,
                    otaDetails = currentState.otaDetails + (deviceKey to result)
                )
            }
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

    private fun filterAndSortDevices(devices: List<Device>, query: String, brand: String): List<Device> {
        val filteredByBrand = if (brand == "All") devices else devices.filter { it.name.contains(brand, ignoreCase = true) }
        val filteredBySearch = if (query.isBlank()) filteredByBrand else {
            filteredByBrand.filter { device ->
                device.name.contains(query, ignoreCase = true) ||
                        device.firmwareGroups.values.flatten().any { it.productModel.contains(query, ignoreCase = true) }
            }
        }
        return filteredBySearch.sortedByDescending { it.isFavorite }
    }
}
