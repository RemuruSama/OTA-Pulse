package com.abhinav.otapulse.feature.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abhinav.otapulse.R
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
    application: Application,
    private val customDeviceManager: CustomDeviceManager
) : AndroidViewModel(application) {

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
                    getApplication<Application>().getString(R.string.toast_import_partial_success, result.importedCount, result.skippedCount)
                } else {
                    getApplication<Application>().getString(R.string.toast_import_success, result.importedCount)
                }
                _uiState.update { it.copy(toastMessage = message) }
            } catch (_: IllegalArgumentException) {
                _uiState.update { it.copy(toastMessage = getApplication<Application>().getString(R.string.toast_import_invalid_format)) }
            }
        }
    }

    fun onExportSuccess() {
        _uiState.update { it.copy(toastMessage = getApplication<Application>().getString(R.string.toast_export_success)) }
    }

    fun onExportFailed(error: String?) {
        _uiState.update { it.copy(toastMessage = getApplication<Application>().getString(R.string.toast_export_failed, error ?: getApplication<Application>().getString(R.string.unknown))) }
    }

    fun onImportFailed(error: String?) {
        _uiState.update { it.copy(toastMessage = getApplication<Application>().getString(R.string.toast_import_failed, error ?: getApplication<Application>().getString(R.string.unknown))) }
    }

    fun clearToastMessage() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}
