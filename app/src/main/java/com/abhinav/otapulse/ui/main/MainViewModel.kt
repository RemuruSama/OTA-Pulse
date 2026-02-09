package com.abhinav.otapulse.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhinav.otapulse.domain.model.AppUpdateInfo
import com.abhinav.otapulse.domain.usecase.CheckAppUpdateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val checkAppUpdateUseCase: CheckAppUpdateUseCase
) : ViewModel() {

    private val _appUpdateState = MutableStateFlow<AppUpdateInfo?>(null)
    val appUpdateState: StateFlow<AppUpdateInfo?> = _appUpdateState.asStateFlow()

    fun checkForUpdate(currentVersion: String) {
        viewModelScope.launch {
            val result = checkAppUpdateUseCase(currentVersion)
            result.onSuccess { updateInfo ->
                if (updateInfo != null) {
                    _appUpdateState.value = updateInfo
                }
            }
            result.onFailure {
                // Log failure or handle error state
            }
        }
    }
    
    fun clearUpdateState() {
        _appUpdateState.value = null
    }
}
