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
            try {
                val result = customDeviceManager.overwriteDevicesFromJson(json)
                val message = if (result.skippedCount > 0) {
                    "Imported ${result.importedCount} custom devices. Skipped ${result.skippedCount} invalid entr${if (result.skippedCount == 1) "y" else "ies"}."
                } else {
                    "Imported ${result.importedCount} custom devices successfully!"
                }
                _uiState.update { it.copy(toastMessage = message) }
            } catch (_: IllegalArgumentException) {
                _uiState.update { it.copy(toastMessage = "Import failed: Invalid file format.") }
            }
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
