package com.abhinav.otapulse.feature.devices.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhinav.otapulse.catalog.repository.CustomDeviceManager
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.catalog.model.RegionInfo
import com.abhinav.otapulse.core.model.RegionVariant
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AddDeviceUiState(
    val deviceName: String = "",
    val firmwareGroups: Map<String, List<RegionVariant>> = emptyMap(),
    val isSaveSuccess: Boolean = false,
    val errorMessage: String? = null,
    val isEditMode: Boolean = false,
    val oldDeviceName: String? = null
)

@HiltViewModel
class AddDeviceViewModel @Inject constructor(
    private val deviceRepository: com.abhinav.otapulse.catalog.repository.DeviceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddDeviceUiState())
    val uiState = _uiState.asStateFlow()

    fun setEditDevice(device: PredefinedDevice) {
        _uiState.update {
            it.copy(
                deviceName = device.name,
                firmwareGroups = device.firmwareGroups,
                isEditMode = true,
                oldDeviceName = device.name
            )
        }
    }

    fun onDeviceNameChanged(name: String) {
        _uiState.update { it.copy(deviceName = name) }
    }

    fun addFirmwareGroup(androidVersion: String) {
        if (androidVersion.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Android Version name cannot be empty.") }
            return
        }
        if (_uiState.value.firmwareGroups.containsKey(androidVersion)) {
            _uiState.update { it.copy(errorMessage = "This firmware group already exists.") }
            return
        }
        val newGroups = _uiState.value.firmwareGroups.toMutableMap()
        newGroups[androidVersion] = emptyList()
        _uiState.update { it.copy(firmwareGroups = newGroups) }
    }

    fun editFirmwareGroup(oldName: String, newName: String) {
        if (newName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "New name cannot be empty.") }
            return
        }
        if (_uiState.value.firmwareGroups.containsKey(newName)) {
            _uiState.update { it.copy(errorMessage = "A group with this name already exists.") }
            return
        }
        val currentGroups = _uiState.value.firmwareGroups
        val variants = currentGroups[oldName] ?: return

        val newGroups = currentGroups.toMutableMap()
        newGroups.remove(oldName)
        newGroups[newName] = variants
        _uiState.update { it.copy(firmwareGroups = newGroups) }
    }

    fun deleteFirmwareGroup(androidVersion: String) {
        val newGroups = _uiState.value.firmwareGroups.toMutableMap()
        newGroups.remove(androidVersion)
        _uiState.update { it.copy(firmwareGroups = newGroups) }
    }

    fun addVariantToGroup(
        androidVersion: String,
        productModel: String,
        productName: String,
        selectedRegion: RegionInfo?,
        versionLetter: String,
        ruiVersion: Int
    ) {
        if (productName.isBlank() || productModel.isBlank() || versionLetter.isBlank()) {
            _uiState.update { it.copy(errorMessage = "All variant fields are required.") }
            return
        }
        if (selectedRegion == null) {
            _uiState.update { it.copy(errorMessage = "A region must be selected.") }
            return
        }

        val firmwareBase = productModel + selectedRegion.nvid
        val newVariant = RegionVariant(
            displayName = selectedRegion.displayName,
            productModel = productName,
            firmwareVersion = "${firmwareBase}_11.${versionLetter}.01_0001_100001010000",
            region = selectedRegion.serverCode
        )

        val newGroups = _uiState.value.firmwareGroups.toMutableMap()
        val variantsForGroup = newGroups[androidVersion]?.toMutableList() ?: mutableListOf()
        variantsForGroup.add(newVariant)
        newGroups[androidVersion] = variantsForGroup
        _uiState.update { it.copy(firmwareGroups = newGroups) }
    }

    fun removeVariantFromGroup(androidVersion: String, variant: RegionVariant) {
        val newGroups = _uiState.value.firmwareGroups.toMutableMap()
        val variantsForGroup = newGroups[androidVersion]?.toMutableList() ?: return
        variantsForGroup.remove(variant)
        newGroups[androidVersion] = variantsForGroup
        _uiState.update { it.copy(firmwareGroups = newGroups) }
    }

    fun saveDevice(name: String, ruiVersion: Int) {
        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Device Name is required.") }
            return
        }
        if (_uiState.value.firmwareGroups.isEmpty() || _uiState.value.firmwareGroups.values.all { it.isEmpty() }) {
            _uiState.update { it.copy(errorMessage = "At least one firmware variant must be added.") }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newDevice = PredefinedDevice(
                    name = name,
                    ruiVersion = ruiVersion,
                    firmwareGroups = _uiState.value.firmwareGroups
                )
                
                if (_uiState.value.isEditMode && _uiState.value.oldDeviceName != null) {
                    deviceRepository.updateCustomDevice(_uiState.value.oldDeviceName!!, newDevice)
                } else {
                    deviceRepository.addCustomDevice(newDevice)
                }

                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isSaveSuccess = true) }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(errorMessage = "Failed to save device: ${e.message}") }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetSaveSuccess() {
        _uiState.update { it.copy(isSaveSuccess = false) }
    }
}
