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

package com.abhinav.otapulse.core.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.abhinav.otapulse.core.common.DeviceUtils
import com.abhinav.otapulse.core.common.OtaUtils
import com.abhinav.otapulse.core.common.OtaUtils.resolvedOtaVersion
import com.abhinav.otapulse.core.model.Device
import com.abhinav.otapulse.core.model.OtaHistoryEntry
import com.abhinav.otapulse.core.model.OtaUpdate
import com.abhinav.otapulse.core.model.RegionVariant
import com.abhinav.otapulse.core.notifications.DownloadNotificationHelper
import com.abhinav.otapulse.core.preferences.AppSettingsPreferences
import com.abhinav.otapulse.feature.devices.domain.FetchOtaDetailsUseCase
import com.abhinav.otapulse.feature.history.data.OtaHistoryRepository
import com.abhinav.otapulse.feature.otatools.data.ArbLookupService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Periodic background worker that checks if a new software update is available for this device.
 *
 * Enhancements:
 * 1. Queries across major OS generation branches (ColorOS/OOS generations A, C, F, H, J)
 *    prioritizing the device's current generation so major upgrades are never missed.
 * 2. Queries servers in prioritized order based on detected device region and NV ID.
 * 3. Compares generation letters, 12-digit build timestamps, and build numbers to strictly verify
 *    that discovered updates are genuine newer releases (preventing false alerts or older build downgrades).
 * 4. Checks anti-rollback (ARB) status and logs discovered updates to Search History.
 * 5. Issues rich, actionable notifications supporting one-tap download or deep-linking into OTA details.
 */
@HiltWorker
class SoftwareUpdateCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val fetchOtaDetailsUseCase: FetchOtaDetailsUseCase,
    private val notificationHelper: DownloadNotificationHelper,
    private val appSettingsPreferences: AppSettingsPreferences,
    private val arbLookupService: ArbLookupService,
    private val otaHistoryRepository: OtaHistoryRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "SoftwareUpdateWorker"
        const val WORK_NAME = "software_update_check"
        private const val PREFS_NAME = "software_update_prefs"
        private const val KEY_LAST_NOTIFIED_VERSION = "last_notified_version"
    }

    override suspend fun doWork(): Result {
        val appSettings = appSettingsPreferences.getAppSettings()
        if (!appSettings.autoSoftwareUpdateCheck) {
            Log.d(TAG, "Auto software update check is disabled, skipping")
            return Result.success()
        }

        // Get current device OTA version and build timestamp
        val currentOtaVersion = DeviceUtils.getCurrentDeviceOtaVersion()
        if (currentOtaVersion.isBlank()) {
            Log.w(TAG, "Could not determine current device OTA version, skipping check")
            return Result.success()
        }

        val deviceBuildTime = DeviceUtils.getDeviceBuildTime()
        val productName = DeviceUtils.getSystemProperty("ro.product.name")
        val productModel = DeviceUtils.getSystemProperty("ro.product.model")
        val nvId = DeviceUtils.getSystemProperty("ro.build.oplus_nv_id")
        val currentLetter = DeviceUtils.getOtaVersionLetter().ifBlank { "A" }
        val brand = DeviceUtils.getDeviceBrand()
        val isOnePlus = brand.equals("OnePlus", ignoreCase = true) ||
                productModel.startsWith("CPH", ignoreCase = true) ||
                productModel.startsWith("P", ignoreCase = true)

        val apiModel = productName.ifBlank { productModel }
        if (apiModel.isBlank()) {
            Log.w(TAG, "Could not determine device model, skipping check")
            return Result.success()
        }

        val detectedRegion = DeviceUtils.getDeviceRegion()
        val region = if (detectedRegion.isNotBlank()) detectedRegion else OtaUtils.inferRegionFromNvId(nvId)
        val reqMode = if (isOnePlus) "taste" else "client_auto"
        val searchOrder = OtaUtils.getServerSearchOrder(region, nvId)
        val prioritizedLetters = OtaUtils.prioritizeLetters(currentLetter)

        Log.d(TAG, "Starting background OTA check: model=$apiModel, currentOta=$currentOtaVersion, currentLetter=$currentLetter, region=$region")

        val ruiVersion = DeviceUtils.getRuiVersion(fallback = 7)
        val device = Device(
            name = "This Device",
            ruiVersion = ruiVersion,
            imei = "0",
            beta = false,
            imageResId = null,
            firmwareGroups = emptyMap(),
            isFavorite = false,
            isCustom = true
        )

        // Query across prioritized generation letters and regional servers concurrently
        val candidateUpdates: List<OtaUpdate> = coroutineScope {
            prioritizedLetters.flatMap { letter ->
                val otaVersionString = OtaUtils.constructOtaString(productModel, nvId, letter)
                if (otaVersionString.isBlank()) return@flatMap emptyList()

                searchOrder.map { server ->
                    async {
                        val regionVariant = RegionVariant(
                            displayName = region,
                            productModel = productModel,
                            productName = apiModel,
                            firmwareVersion = otaVersionString,
                            region = server,
                            nvId = nvId.takeIf { it.isNotBlank() },
                            language = "en-EN"
                        )
                        try {
                            val result = fetchOtaDetailsUseCase(device, regionVariant, reqMode, 0)
                            result.getOrNull()?.also { ota ->
                                Log.d(TAG, "[$server / branch $letter] Found: ${ota.resolvedOtaVersion()}")
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "[$server / branch $letter] Failed: ${e.message}")
                            null
                        }
                    }
                }
            }.mapNotNull { it.await() }
        }

        if (candidateUpdates.isEmpty()) {
            Log.d(TAG, "No updates returned from servers")
            return Result.success()
        }

        // Strictly filter only updates that are genuinely newer than the device's installed build
        val newerUpdates = candidateUpdates.filter { ota ->
            val serverVersion = ota.resolvedOtaVersion()
            OtaUtils.isServerVersionNewer(
                serverOtaVersion = serverVersion,
                currentOtaVersion = currentOtaVersion,
                serverPublishedTime = ota.publishedTime,
                deviceBuildTime = deviceBuildTime
            )
        }

        if (newerUpdates.isEmpty()) {
            Log.d(TAG, "Device is already up to date (current: $currentOtaVersion)")
            return Result.success()
        }

        // Pick the latest available build among all newer candidates
        val latestOta = newerUpdates.maxWith(compareBy { it.resolvedOtaVersion() })
        val serverOtaVersion = latestOta.resolvedOtaVersion()
        val displayVersion = latestOta.versionName ?: serverOtaVersion

        Log.i(TAG, "New software update verified: $displayVersion ($serverOtaVersion) > $currentOtaVersion")

        // Enrich with ARB metadata if enabled
        val enrichedOta = if (appSettings.arbDetection) {
            val arbInfo = arbLookupService.lookupByUrl(latestOta.downloadUrl)
            if (arbInfo != null) {
                latestOta.copy(arbStatus = arbInfo.toDisplayString())
            } else {
                latestOta
            }
        } else {
            latestOta
        }

        // Log to search history so the user can easily find it later
        runCatching {
            otaHistoryRepository.logOtaUpdate(
                OtaHistoryEntry(
                    timestamp = System.currentTimeMillis(),
                    deviceName = "This Device",
                    region = region,
                    otaUpdate = enrichedOta
                )
            )
        }

        notifyIfNew(serverOtaVersion, displayVersion, enrichedOta, region, device)
        return Result.success()
    }

    private fun notifyIfNew(
        serverVersionKey: String,
        displayVersion: String,
        otaUpdate: OtaUpdate,
        region: String,
        device: Device
    ) {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastNotified = prefs.getString(KEY_LAST_NOTIFIED_VERSION, null)

        if (lastNotified == serverVersionKey) {
            Log.d(TAG, "Already notified for version $serverVersionKey, skipping")
            return
        }

        Log.i(TAG, "Posting software update notification: $displayVersion ($serverVersionKey)")
        notificationHelper.showSoftwareUpdateNotification(displayVersion, otaUpdate, region, device)
        prefs.edit().putString(KEY_LAST_NOTIFIED_VERSION, serverVersionKey).apply()
    }
}
