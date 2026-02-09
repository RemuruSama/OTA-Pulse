package com.abhinav.otapulse.ui.ota

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhinav.otapulse.domain.model.OtaRequest
import com.abhinav.otapulse.domain.usecase.FetchOtaUpdateUseCase
import com.abhinav.otapulse.util.NetworkComponent
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

    private val _otaState = MutableStateFlow<List<NetworkComponent>>(emptyList())
    val otaState: StateFlow<List<NetworkComponent>> = _otaState.asStateFlow()

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun fetchOta(request: OtaRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorState.value = null
            
            val result = fetchOtaUpdateUseCase(request)
            
            result.onSuccess { components ->
                _otaState.value = components
            }
            result.onFailure { e ->
                _errorState.value = e.message ?: "Unknown Error"
            }
            
            _isLoading.value = false
        }
    }
}
