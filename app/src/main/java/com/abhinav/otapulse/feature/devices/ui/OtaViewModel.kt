package com.abhinav.otapulse.feature.devices.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhinav.otapulse.core.model.OtaRequest
import com.abhinav.otapulse.core.model.OtaUpdate
import com.abhinav.otapulse.feature.devices.domain.FetchOtaUpdateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OtaViewModel @Inject constructor(
    private val fetchOtaUpdateUseCase: FetchOtaUpdateUseCase
) : ViewModel() {

    private val _otaState = MutableStateFlow<List<OtaUpdate>>(emptyList())
    val otaState: StateFlow<List<OtaUpdate>> = _otaState.asStateFlow()

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun fetchOta(request: OtaRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorState.value = null
            
            val result = fetchOtaUpdateUseCase(request)
            
            result.onSuccess { updates ->
                _otaState.value = updates
            }
            result.onFailure { e ->
                _errorState.value = e.message ?: "Unknown Error"
            }
            
            _isLoading.value = false
        }
    }
}

