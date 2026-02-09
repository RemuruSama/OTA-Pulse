package com.abhinav.otapulse.ui.custom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhinav.otapulse.data.repository.DownloadManager
import com.abhinav.otapulse.domain.model.Device
import com.abhinav.otapulse.domain.model.OtaUpdate
import com.abhinav.otapulse.domain.model.RegionVariant
import com.abhinav.otapulse.domain.usecase.FetchOtaDetailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomUpdateUiState(
    val isLoading: Boolean = false,
    val result: Result<OtaUpdate>? = null,
    val deviceName: String = "",
    val regionName: String = ""
)

@HiltViewModel
class CustomUpdateViewModel @Inject constructor(
    private val fetchOtaDetailsUseCase: FetchOtaDetailsUseCase,
    private val downloadManager: DownloadManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomUpdateUiState())
    val uiState: StateFlow<CustomUpdateUiState> = _uiState.asStateFlow()

    fun sendRequest(
        model: String,
        otaVersion: String,
        ruiVersion: Int,
        region: String,
        server: String,
        regionsArray: Array<String>,
        // Advanced Options
        imei: String = "0",
        beta: Boolean = false,
        nvId: String? = null,
        language: String? = "en-EN"
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, result = null) }

            val dummyDevice = Device(
                name = "Custom Device",
                ruiVersion = ruiVersion,
                imei = imei,
                beta = beta,
                imageUrl = null,
                imageResId = null,
                firmwareGroups = emptyMap(),
                isFavorite = false,
                isCustom = true
            )

            // CRITICAL: Decouple 'Region Variant' (NVID) from 'Target Server'
            // 'displayName': Used for UI (e.g., "Vietnam")
            // 'region': PASSED TO REQUEST. We use the 'server' here (e.g., "IN" or "EU")
            // This forces the request to hit the specified server URL while keeping the model/otaVersion
            val regionVariant = RegionVariant(
                displayName = region,
                productModel = model,
                firmwareVersion = otaVersion,
                region = server, // Use the manually selected server
                nvId = nvId,
                language = language
            )

            val result = fetchOtaDetailsUseCase(dummyDevice, regionVariant)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    result = result,
                    deviceName = dummyDevice.name,
                    regionName = "${regionVariant.displayName} (Server: $server)"
                )
            }
        }
    }

    fun startDownload(otaUpdate: OtaUpdate, deviceName: String, regionName: String) {
        viewModelScope.launch {
            downloadManager.enqueueDownload(otaUpdate, deviceName, regionName)
        }
    }
}