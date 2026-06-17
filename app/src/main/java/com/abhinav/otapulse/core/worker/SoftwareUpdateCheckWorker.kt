package com.abhinav.otapulse.core.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.abhinav.otapulse.catalog.model.RegionData
import com.abhinav.otapulse.core.common.DeviceUtils
import com.abhinav.otapulse.core.model.Device
import com.abhinav.otapulse.core.model.OtaUpdate
import com.abhinav.otapulse.core.model.RegionVariant
import com.abhinav.otapulse.core.notifications.DownloadNotificationHelper
import com.abhinav.otapulse.feature.devices.domain.FetchOtaDetailsUseCase
import com.abhinav.otapulse.feature.settings.SettingsFragment
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Periodic background worker that checks if a new device software (OTA) update
 * is available. It queries ALL known servers concurrently and picks the build
 * with the highest (latest) version string, so a stale server can never hide
 * a newer update that is already live on another server.
 */
@HiltWorker
class SoftwareUpdateCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val fetchOtaDetailsUseCase: FetchOtaDetailsUseCase,
    private val notificationHelper: DownloadNotificationHelper
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "SoftwareUpdateWorker"
        const val WORK_NAME = "software_update_check"
        private const val PREFS_NAME = "software_update_prefs"
        private const val KEY_LAST_NOTIFIED_VERSION = "last_notified_version"
        private val SERVER_SEARCH_ORDER = listOf("EU", "GL", "IN", "CN")
    }

    override suspend fun doWork(): Result {
        // Check if the feature is enabled
        val appSettingsPrefs = applicationContext.getSharedPreferences(
            SettingsFragment.APP_SETTINGS_PREFS, Context.MODE_PRIVATE
        )
        val isEnabled = appSettingsPrefs.getBoolean(
            SettingsFragment.PREF_AUTO_SOFTWARE_UPDATE_CHECK, true
        )
        if (!isEnabled) {
            Log.d(TAG, "Auto software update check is disabled, skipping")
            return Result.success()
        }

        // Get current device info
        val currentOtaVersion = DeviceUtils.getOtaVersion()
        if (currentOtaVersion.isBlank()) {
            Log.w(TAG, "Could not read ro.build.version.ota, skipping check")
            return Result.success()
        }

        val productName = DeviceUtils.getSystemProperty("ro.product.name")
        val productModel = DeviceUtils.getSystemProperty("ro.product.model")
        val nvId = DeviceUtils.getSystemProperty("ro.build.oplus_nv_id")
        val otaVersionLetter = DeviceUtils.getOtaVersionLetter()
        val isOnePlus = DeviceUtils.getDeviceBrand().equals("OnePlus", ignoreCase = true)

        val apiModel = productName.ifBlank { productModel }
        if (apiModel.isBlank()) {
            Log.w(TAG, "Could not determine device model, skipping check")
            return Result.success()
        }

        val letter = otaVersionLetter.ifBlank { "A" }
        val otaVersionString = constructOtaString(productModel, letter)
        if (otaVersionString.isBlank()) {
            Log.w(TAG, "Could not construct OTA version string, skipping check")
            return Result.success()
        }

        val region = inferRegionFromNvId(nvId)
        val reqMode = if (isOnePlus) "taste" else "client_auto"

        // Determine custom search order based on NV ID
        val searchOrder = if (nvId == "10010111") {
            listOf("CN") + (SERVER_SEARCH_ORDER - "CN")
        } else {
            SERVER_SEARCH_ORDER
        }

        Log.d(TAG, "Checking for software update: model=$apiModel, ota=$otaVersionString, region=$region")

        // Read the real RUI version so the correct server URL and
        // encryption path are used (e.g. RUI 5 = ColorOS 14, RUI 6 = ColorOS 16).
        val ruiVersion = DeviceUtils.getRuiVersion(fallback = 4)
        Log.d(TAG, "Device RUI version: $ruiVersion")

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

        // ── Query ALL servers concurrently, then pick the newest version ──────────
        // This prevents a stale server (with an older build) from hiding a newer
        // version that is already available on another server.
        val serverResults: List<OtaUpdate> = coroutineScope {
            searchOrder.map { server ->
                async {
                    val regionVariant = RegionVariant(
                        displayName = region,
                        productModel = apiModel,
                        firmwareVersion = otaVersionString,
                        region = server,
                        nvId = nvId.takeIf { it.isNotBlank() },
                        language = "en-EN"
                    )
                    try {
                        val result = fetchOtaDetailsUseCase(device, regionVariant, reqMode, 0)
                        result.getOrNull()?.also { ota ->
                            Log.d(TAG, "[$server] version: ${ota.realOtaVersion ?: ota.componentVersion}")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "[$server] failed: ${e.message}")
                        null
                    }
                }
            }.mapNotNull { it.await() }
        }

        if (serverResults.isEmpty()) {
            Log.d(TAG, "All servers returned no result, skipping")
            return Result.success()
        }

        // Pick the update with the highest OTA version string across all servers.
        // realOtaVersion encodes a build timestamp in its last segment
        // (e.g. CPH2487_11.H.54_3540_202602261724), so lexicographic max naturally
        // selects the most recent build.  componentVersion is used as a fallback.
        val latestOta = serverResults.maxWith(compareBy { it.resolvedOtaVersion() })

        val serverOtaVersion = latestOta.resolvedOtaVersion()
        Log.d(TAG, "Best server version across all servers: $serverOtaVersion  (current: $currentOtaVersion)")

        if (serverOtaVersion != currentOtaVersion && serverOtaVersion.isNotBlank()) {
            val displayVersion = latestOta.versionName ?: serverOtaVersion
            notifyIfNew(serverOtaVersion, displayVersion, latestOta, region, device)
        } else {
            Log.d(TAG, "Device is up to date")
        }

        return Result.success()
    }

    /**
     * Returns the best available OTA version string for comparison purposes.
     * Prefers [OtaUpdate.realOtaVersion] (exact match to ro.build.version.ota)
     * and falls back to the base part of [OtaUpdate.componentVersion].
     */
    private fun OtaUpdate.resolvedOtaVersion(): String =
        realOtaVersion
            ?: componentVersion.substringBefore(".")
                .let { base -> if (base.count { it == '_' } >= 3) base else componentVersion }

    private fun notifyIfNew(
        componentVersion: String,
        displayVersion: String,
        otaUpdate: OtaUpdate,
        region: String,
        device: Device
    ) {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastNotified = prefs.getString(KEY_LAST_NOTIFIED_VERSION, null)

        if (lastNotified == componentVersion) {
            Log.d(TAG, "Already notified for version $componentVersion, skipping")
            return
        }

        Log.i(TAG, "New software update available: $displayVersion ($componentVersion)")
        notificationHelper.showSoftwareUpdateNotification(displayVersion, otaUpdate, region, device)
        prefs.edit().putString(KEY_LAST_NOTIFIED_VERSION, componentVersion).apply()
    }

    private fun inferRegionFromNvId(nvId: String): String {
        val normalizedNvId = nvId.trim()
        val nvRegion = RegionData.regions.firstOrNull {
            it.nvid.equals(normalizedNvId, ignoreCase = true)
        }?.displayName
        return nvRegion ?: "GLO"
    }

    private fun constructOtaString(rawId: String, letter: String): String {
        val suffixesToStrip = listOf(
            "EEA", "IN", "RU", "TR", "CN", "EU", "TW", "MEA", "SA",
            "SG", "TH", "LATAM", "BR", "MY", "ID", "KZ", "OCA", "VN", "GLO"
        ).distinct()
        var baseModel = rawId
        for (suffix in suffixesToStrip) {
            if (baseModel.endsWith(suffix, ignoreCase = true)) {
                baseModel = baseModel.dropLast(suffix.length)
                break
            }
        }
        val cleanBase = baseModel.replace(Regex("NV[0-9A-Z]{2}$", RegexOption.IGNORE_CASE), "")
        return "${cleanBase}_11.${letter}.01_0001_100001010000"
    }
}
