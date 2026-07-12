/*
 * Copyright (C) 2026 OTA Pulse
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.abhinav.otapulse.feature.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.abhinav.otapulse.R
import com.abhinav.otapulse.catalog.repository.CustomDeviceManager
import com.abhinav.otapulse.core.preferences.AppSettings
import com.abhinav.otapulse.core.preferences.AppSettingsPreferences
import com.abhinav.otapulse.core.preferences.ThemePreferences
import com.abhinav.otapulse.core.preferences.ThemeSettings
import com.abhinav.otapulse.core.ui.theme.ThemeMode
import com.abhinav.otapulse.core.worker.SoftwareUpdateCheckWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class SettingsUiState(
    val toastMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val customDeviceManager: CustomDeviceManager,
    private val themePreferences: ThemePreferences,
    private val appSettingsPreferences: AppSettingsPreferences
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    val themeSettings: StateFlow<ThemeSettings> = themePreferences.themeSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = themePreferences.getThemeSettings()
        )

    val appSettings: StateFlow<AppSettings> = appSettingsPreferences.appSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = appSettingsPreferences.getAppSettings()
        )

    fun setThemeMode(mode: ThemeMode) = themePreferences.setThemeMode(mode)
    fun setNightMode(mode: Int) = themePreferences.setNightMode(mode)
    fun setAmoledDark(enabled: Boolean) = themePreferences.setAmoledDark(enabled)
    fun setDynamicColor(enabled: Boolean) = themePreferences.setDynamicColor(enabled)
    fun setGradientBackground(enabled: Boolean) = themePreferences.setGradientBackground(enabled)

    fun setAdvancedMode(enabled: Boolean) = appSettingsPreferences.setAdvancedMode(enabled)
    fun setAutoUpdateCheck(enabled: Boolean) = appSettingsPreferences.setAutoUpdateCheck(enabled)
    fun setArbDetection(enabled: Boolean) = appSettingsPreferences.setArbDetection(enabled)
    fun setBrowserDesktopMode(enabled: Boolean) = appSettingsPreferences.setBrowserDesktopMode(enabled)
    fun setBrowserShowControls(enabled: Boolean) = appSettingsPreferences.setBrowserShowControls(enabled)

    fun setAutoSoftwareUpdateCheck(enabled: Boolean) {
        appSettingsPreferences.setAutoSoftwareUpdateCheck(enabled)
        val workManager = WorkManager.getInstance(getApplication())
        if (enabled) {
            enqueueUpdateCheckWork(workManager)
        } else {
            workManager.cancelUniqueWork(SoftwareUpdateCheckWorker.WORK_NAME)
        }
    }

    fun setCheckIntervalHours(hours: Long) {
        appSettingsPreferences.setCheckIntervalHours(hours)
        val workManager = WorkManager.getInstance(getApplication())
        if (appSettingsPreferences.getAppSettings().autoSoftwareUpdateCheck) {
            enqueueUpdateCheckWork(workManager, replaceExisting = true)
        }
    }

    private fun enqueueUpdateCheckWork(workManager: WorkManager, replaceExisting: Boolean = false) {
        val intervalHours = appSettingsPreferences.getAppSettings().checkIntervalHours
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val workRequest = PeriodicWorkRequestBuilder<SoftwareUpdateCheckWorker>(
            intervalHours, TimeUnit.HOURS
        ).setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10000L,
                TimeUnit.MILLISECONDS
            )
            .build()
        workManager.enqueueUniquePeriodicWork(
            SoftwareUpdateCheckWorker.WORK_NAME,
            if (replaceExisting) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun getCustomDevicesAsJson(): String {
        return customDeviceManager.getCustomDevicesAsJson()
    }

    fun exportToFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().contentResolver.openFileDescriptor(uri, "w")?.use {
                    FileOutputStream(it.fileDescriptor).use { stream ->
                        stream.write(getCustomDevicesAsJson().toByteArray())
                    }
                }
                onExportSuccess()
            } catch (e: Exception) {
                onExportFailed(e.message)
            }
        }
    }

    fun importFromFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val stringBuilder = StringBuilder()
                getApplication<Application>().contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line: String? = reader.readLine()
                        while (line != null) {
                            stringBuilder.append(line)
                            line = reader.readLine()
                        }
                    }
                }
                importCustomDevices(stringBuilder.toString())
            } catch (e: Exception) {
                onImportFailed(e.message)
            }
        }
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
