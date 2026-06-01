package com.abhinav.otapulse.feature.history.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhinav.otapulse.core.model.OtaHistoryEntry
import com.abhinav.otapulse.feature.history.data.OtaHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OtaHistoryViewModel @Inject constructor(
    private val repository: OtaHistoryRepository
) : ViewModel() {

    private val _deviceName = MutableStateFlow<String?>(null)

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
}
