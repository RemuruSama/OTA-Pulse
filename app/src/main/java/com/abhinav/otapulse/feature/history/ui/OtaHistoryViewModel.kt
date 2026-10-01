package com.abhinav.otapulse.feature.history.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhinav.otapulse.core.model.OtaHistoryEntry
import com.abhinav.otapulse.feature.history.data.OtaHistoryRepository
import com.abhinav.otapulse.feature.otatools.data.ArbLookupService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OtaHistoryViewModel @Inject constructor(
    private val repository: OtaHistoryRepository,
    private val arbLookupService: ArbLookupService
) : ViewModel() {

    private val _deviceName = MutableStateFlow<String?>(null)
    private val _checkingArbIds = MutableStateFlow<Set<Long>>(emptySet())
    val checkingArbIds: StateFlow<Set<Long>> = _checkingArbIds.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun clearUserMessage() {
        _userMessage.value = null
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val historyFlow: StateFlow<List<OtaHistoryEntry>> = _deviceName
        .flatMapLatest { name ->
            if (name == null) {
                repository.getAllHistory()
            } else {
                repository.getHistoryForDevice(name)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun checkAndSaveArbStatus(entry: OtaHistoryEntry) {
        val rawUrl = entry.otaUpdate.url.ifBlank { entry.otaUpdate.downloadUrl }
        if (rawUrl.isBlank()) {
            _userMessage.value = "No download URL available for this package"
            return
        }

        viewModelScope.launch {
            _checkingArbIds.update { it + entry.id }
            runCatching {
                arbLookupService.lookup(rawUrl)
            }.onSuccess { arbInfo ->
                val statusString = if (arbInfo != null) {
                    if (arbInfo.isSafe) "Safe (ARB 0)" else "Protected (ARB ${arbInfo.arbIndex})"
                } else {
                    "Not Detected"
                }
                repository.updateArbStatus(entry, statusString)
                _userMessage.value = "ARB: $statusString saved for ${entry.otaUpdate.versionName ?: entry.deviceName}"
            }.onFailure { error ->
                _userMessage.value = "ARB check failed: ${error.message ?: "Unknown error"}"
            }
            _checkingArbIds.update { it - entry.id }
        }
    }

    fun checkAllMissingArb(entries: List<OtaHistoryEntry>) {
        val targets = entries.filter { it.otaUpdate.arbStatus.isNullOrBlank() || it.otaUpdate.arbStatus == "N/A" }
        if (targets.isEmpty()) {
            _userMessage.value = "All records already have ARB status"
            return
        }
        viewModelScope.launch {
            _userMessage.value = "Checking ARB for ${targets.size} package(s)..."
            targets.forEach { entry ->
                val rawUrl = entry.otaUpdate.url.ifBlank { entry.otaUpdate.downloadUrl }
                if (rawUrl.isNotBlank()) {
                    _checkingArbIds.update { it + entry.id }
                    runCatching { arbLookupService.lookup(rawUrl) }
                        .onSuccess { arbInfo ->
                            val statusString = if (arbInfo != null) {
                                if (arbInfo.isSafe) "Safe (ARB 0)" else "Protected (ARB ${arbInfo.arbIndex})"
                            } else {
                                "Not Detected"
                            }
                            repository.updateArbStatus(entry, statusString)
                        }
                    _checkingArbIds.update { it - entry.id }
                }
            }
            _userMessage.value = "Finished checking ARB status for ${targets.size} packages"
        }
    }

    fun setDeviceName(name: String?) {
        _deviceName.value = name
    }

    fun clearHistory(deviceName: String?) {
        viewModelScope.launch {
            if (deviceName == null) {
                repository.clearAllHistory()
            } else {
                repository.clearHistoryForDevice(deviceName)
            }
        }
    }

    fun importHistory(entries: List<OtaHistoryEntry>) {
        viewModelScope.launch {
            entries.forEach { entry ->
                repository.logOtaUpdate(entry)
            }
        }
    }

    fun deleteHistoryEntry(entry: OtaHistoryEntry) {
        viewModelScope.launch {
            repository.deleteHistoryEntry(entry)
        }
    }

    fun restoreHistoryEntry(entry: OtaHistoryEntry) {
        viewModelScope.launch {
            repository.restoreHistoryEntry(entry)
        }
    }
}

