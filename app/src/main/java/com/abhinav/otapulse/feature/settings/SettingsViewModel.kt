package com.abhinav.otapulse.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhinav.otapulse.catalog.repository.CustomDeviceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val toastMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val customDeviceManager: CustomDeviceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    fun getCustomDevicesAsJson(): String {
        return customDeviceManager.getCustomDevicesAsJson()
    }

    fun importCustomDevices(json: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = customDeviceManager.overwriteDevicesFromJson(json)
            val message = if (success) "Custom devices imported successfully!" else "Import failed: Invalid file format."
            _uiState.update { it.copy(toastMessage = message) }
        }
    }

    fun onExportSuccess() {
        _uiState.update { it.copy(toastMessage = "Custom devices exported successfully!") }
    }

    fun onExportFailed(error: String?) {
        _uiState.update { it.copy(toastMessage = "Export failed: $error") }
    }

    fun onImportFailed(error: String?) {
        _uiState.update { it.copy(toastMessage = "Import failed: $error") }
    }

    fun clearToastMessage() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}
