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

package com.abhinav.otapulse.feature.updates.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhinav.otapulse.feature.otatools.data.ArbLookupService
import com.abhinav.otapulse.core.model.Device
import com.abhinav.otapulse.catalog.model.RegionData
import com.abhinav.otapulse.core.model.RegionVariant
import com.abhinav.otapulse.core.common.DeviceUtils
import com.abhinav.otapulse.core.model.OtaUpdate
import com.abhinav.otapulse.core.preferences.AppSettingsPreferences
import com.abhinav.otapulse.feature.devicecatalog.ui.PartitionSelectDialogData
import com.abhinav.otapulse.feature.devices.domain.FetchOtaDetailsUseCase
import com.abhinav.otapulse.feature.downloads.domain.DownloadRepository
import com.abhinav.otapulse.ota.engine.OtaExtractor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeUpdateViewModel @Inject constructor(
    private val fetchOtaDetailsUseCase: FetchOtaDetailsUseCase,
    private val downloadRepository: DownloadRepository,
    private val arbLookupService: ArbLookupService,
    private val otaExtractor: OtaExtractor,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val appSettingsPreferences: AppSettingsPreferences
) : ViewModel() {

    private val workManager = androidx.work.WorkManager.getInstance(context)

    private val _uiState = MutableStateFlow(HomeUpdateUiState())
    val uiState: StateFlow<HomeUpdateUiState> = _uiState.asStateFlow()

    init {
        populateDeviceInputs()
    }

    private fun populateDeviceInputs() {
        val model = DeviceUtils.getSystemProperty("ro.product.model")
        val name = DeviceUtils.getSystemProperty("ro.product.name")
        val marketName = DeviceUtils.getSystemProperty("ro.vendor.oplus.market.name")
        val nvId = DeviceUtils.getSystemProperty("ro.build.oplus_nv_id")
        val otaVersionLetter = DeviceUtils.getOtaVersionLetter()
        val osVersion = DeviceUtils.getOsVersion()
        val displayOtaVersion = DeviceUtils.getDisplayOtaVersion()
        val fallbackOtaVersion = DeviceUtils.getOtaVersion()

        val defaultModel = if (model.isNotBlank()) model else "RMX3840"
        val defaultName = if (name.isNotBlank()) name else defaultModel
        val defaultMarket = if (marketName.isNotBlank()) marketName else defaultName
        val defaultLetter = if (otaVersionLetter.isNotBlank()) otaVersionLetter else "A"
        val defaultReq = if (isOnePlusDevice()) "taste" else "manual"

        _uiState.update {
            it.copy(
                deviceModel = defaultModel,
                deviceName = defaultName,
                marketName = defaultMarket,
                nvId = nvId,
                versionLetter = defaultLetter,
                reqMode = defaultReq,
                osVersion = osVersion,
                displayOtaVersion = displayOtaVersion,
                fallbackOtaVersion = fallbackOtaVersion
            )
        }
    }

    fun updateModel(value: String) {
        _uiState.update { it.copy(deviceModel = value) }
    }

    fun updateName(value: String) {
        _uiState.update { it.copy(deviceName = value) }
    }

    fun updateNvId(value: String) {
        _uiState.update { it.copy(nvId = value) }
    }

    fun updateVersionLetter(value: String) {
        _uiState.update { it.copy(versionLetter = value) }
    }

    fun updateReqMode(value: String) {
        _uiState.update { it.copy(reqMode = value) }
    }

    fun selectOta(ota: OtaUpdate?) {
        _uiState.update { it.copy(selectedOta = ota) }
    }

    fun clearPartitionSelectDialog() {
        _uiState.update { it.copy(partitionSelectDialog = null) }
    }

    fun clearStartingExtraction() {
        _uiState.update { it.copy(isStartingExtraction = false) }
    }

    fun cancelPartitionExtraction(workId: java.util.UUID) {
        workManager.cancelWorkById(workId)
        _uiState.update { it.copy(isStartingExtraction = false) }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun checkForUpdate() {
        val model = _uiState.value.deviceModel.trim()
        val name = _uiState.value.deviceName.trim()
        val nvIdInput = _uiState.value.nvId.trim()
        val reqMode = _uiState.value.reqMode.trim().ifBlank { defaultReqMode(model) }

        if (model.isBlank()) {
            _uiState.update { it.copy(userMessage = "Please fill required fields") }
            return
        }

        val baseOtaVersion = getBaseOtaString(model)
        val region = inferRegionFromNvId(nvIdInput)
        val apiModelParam = if (name.isNotBlank()) name else model
        val finalNvId = nvIdInput

        val serverSearchOrder = listOf("EU", "GL", "IN", "CN")
        val customSearchOrder = if (finalNvId == "10010111") {
            listOf("CN") + (serverSearchOrder - "CN")
        } else {
            serverSearchOrder
        }

        val letters = listOf("A", "C", "F", "H", "J")

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, multiResults = null, selectedOta = null, userMessage = null) }

            val dummyDevice = Device(
                name = "Custom|" + apiModelParam,
                ruiVersion = 4,
                imei = "0",
                beta = false,
                imageResId = null,
                firmwareGroups = emptyMap<String, List<RegionVariant>>(),
                isFavorite = false,
                isCustom = true
            )

            val isArbDetectionEnabled = appSettingsPreferences.getAppSettings().arbDetection

            val nvSuffix = if (finalNvId.trim().startsWith("NV", ignoreCase = true) && finalNvId.trim().length == 4) {
                finalNvId.trim().uppercase()
            } else {
                ""
            }

            data class ServerResult(val server: String, val letter: String, val ota: OtaUpdate)

            val successfulResults: List<ServerResult> = coroutineScope {
                letters.flatMap { l ->
                    val otaVersion = "${baseOtaVersion}${nvSuffix}_11.${l}.01_0001_100001010000"
                    customSearchOrder.map { server ->
                        async {
                            val regionVariant = RegionVariant(
                                displayName = region,
                                productModel = model,
                                productName = apiModelParam,
                                firmwareVersion = otaVersion,
                                region = server,
                                nvId = finalNvId.takeIf { it.isNotBlank() },
                                language = "en-EN"
                            )
                            runCatching {
                                fetchOtaDetailsUseCase(dummyDevice, regionVariant, reqMode, 0)
                                    .getOrThrow()
                                    .let { ota ->
                                        val enriched = if (isArbDetectionEnabled) {
                                            val arbInfo = arbLookupService.lookupByUrl(ota.downloadUrl)
                                            if (arbInfo != null) ota.copy(arbStatus = arbInfo.toDisplayString()) else ota
                                        } else {
                                            ota.copy(arbStatus = "N/A")
                                        }
                                        ServerResult(server, l, enriched)
                                    }
                            }.getOrNull()
                        }
                    }
                }.mapNotNull { it.await() }
            }

            if (successfulResults.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "No update found for this configuration.",
                        multiResults = emptyList()
                    )
                }
                return@launch
            }

            val bestResultsPerLetter = successfulResults
                .groupBy { it.letter }
                .map { (_, results) -> results.maxWith(compareBy { it.ota.resolvedOtaVersion() }) }
                .sortedByDescending { it.letter }

            val bestOverall = bestResultsPerLetter.maxWithOrNull(compareBy { it.ota.resolvedOtaVersion() })

            _uiState.update {
                it.copy(
                    isLoading = false,
                    multiResults = bestResultsPerLetter.map { sr -> sr.ota },
                    error = null
                )
            }
        }
    }

    fun startDownload(otaUpdate: OtaUpdate) {
        viewModelScope.launch {
            val deviceName = _uiState.value.deviceName.ifBlank { _uiState.value.deviceModel }
            val regionName = inferRegionFromNvId(_uiState.value.nvId)
            downloadRepository.enqueueDownload(otaUpdate, deviceName, regionName, true)
        }
    }

    fun fetchExtractablePartitions(ota: OtaUpdate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingPartitions = true) }
            try {
                val source = ota.url
                val versionName = ota.versionName ?: "Custom"
                val session = otaExtractor.open(source)
                val partitions = otaExtractor.listPartitions(session)
                _uiState.update {
                    it.copy(
                        isFetchingPartitions = false,
                        partitionSelectDialog = PartitionSelectDialogData(
                            url = source,
                            versionName = versionName,
                            partitions = partitions
                        )
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeUpdateViewModel", "Error fetching partitions", e)
                _uiState.update {
                    it.copy(
                        isFetchingPartitions = false,
                        userMessage = "Could not parse partitions: ${e.message}"
                    )
                }
            }
        }
    }

    fun extractPartitions(source: String, versionName: String, partitionNames: List<String>, regionName: String? = null): java.util.UUID {
        val data = androidx.work.workDataOf(
            com.abhinav.otapulse.arb.worker.PartitionExtractorWorker.KEY_SOURCE to source,
            com.abhinav.otapulse.arb.worker.PartitionExtractorWorker.KEY_URL to source,
            com.abhinav.otapulse.arb.worker.PartitionExtractorWorker.KEY_VERSION_NAME to versionName,
            com.abhinav.otapulse.arb.worker.PartitionExtractorWorker.KEY_PARTITION_NAMES to partitionNames.toTypedArray(),
            com.abhinav.otapulse.arb.worker.PartitionExtractorWorker.KEY_REGION_NAME to regionName
        )

        val tagNames = partitionNames.joinToString("_")
        val request = androidx.work.OneTimeWorkRequestBuilder<com.abhinav.otapulse.arb.worker.PartitionExtractorWorker>()
            .setInputData(data)
            .addTag("extraction_$tagNames")
            .build()

        workManager.enqueue(request)
        _uiState.update { it.copy(isStartingExtraction = true) }
        return request.id
    }

    private fun defaultReqMode(model: String): String = if (isOnePlusDevice(model)) "taste" else "manual"

    private fun isOnePlusDevice(model: String = ""): Boolean {
        val brand = DeviceUtils.getDeviceBrand()
        return brand.equals("OnePlus", ignoreCase = true) || model.startsWith("CPH", ignoreCase = true) || model.startsWith("P", ignoreCase = true)
    }

    private fun inferRegionFromNvId(nvId: String): String {
        val normalizedNvId = nvId.trim()
        val nvRegion = RegionData.regions.firstOrNull {
            it.nvid.equals(normalizedNvId, ignoreCase = true)
        }?.displayName
        return nvRegion ?: "GLO"
    }

    private fun getBaseOtaString(rawId: String): String {
        val suffixesToStrip = listOf("EEA", "IN", "RU", "TR", "CN", "EU", "TW", "MEA", "SA", "SG", "TH", "LATAM", "BR", "MY", "ID", "KZ", "OCA", "VN", "GLO")
            .distinct()
        var baseModel = rawId.substringBefore("_11").substringBefore(".")
        for (suffix in suffixesToStrip) {
            if (baseModel.endsWith(suffix, ignoreCase = true)) {
                baseModel = baseModel.dropLast(suffix.length)
                break
            }
        }

        return baseModel.replace(Regex("NV[0-9A-Z]{2}$", RegexOption.IGNORE_CASE), "")
    }

    private fun OtaUpdate.resolvedOtaVersion(): String =
        realOtaVersion
            ?: componentVersion.substringBefore(".")
                .let { base -> if (base.count { it == '_' } >= 3) base else componentVersion }
}
