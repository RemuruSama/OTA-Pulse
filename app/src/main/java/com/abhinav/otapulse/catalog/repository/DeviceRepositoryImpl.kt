package com.abhinav.otapulse.catalog.repository

import android.content.Context
import android.content.SharedPreferences
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.core.common.toDomain
import com.abhinav.otapulse.core.model.Device
import com.abhinav.otapulse.di.FavoritesPrefs
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @FavoritesPrefs private val favoritesPrefs: SharedPreferences,
    private val customDeviceManager: CustomDeviceManager,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) : DeviceRepository {

    private val _devicesFlow = MutableStateFlow<List<Device>>(emptyList())
    private val cacheMutex = Mutex()
    private val devicesFile = File(context.filesDir, "devices.json")

    private var inMemoryFixedDevices: List<PredefinedDevice> = emptyList()

    companion object {
        private const val KEY_FAVORITES_SET = "favorites_set"
        private const val KEY_FAVORITES_LEGACY_JSON = "favorites"
        private val CATALOG_URLS = listOf(
            "https://raw.githubusercontent.com/RemuruSama/OTA-Pulse/main/devices/realme.json",
            "https://raw.githubusercontent.com/RemuruSama/OTA-Pulse/main/devices/oneplus.json",
            "https://raw.githubusercontent.com/RemuruSama/OTA-Pulse/main/devices/oppo.json"
        )
    }

    init {
        migrateLegacyFavoritesIfNeeded()
        loadLocalCatalog()
        updateDevicesCache()
    }

    private fun loadLocalCatalog() {
        try {
            if (devicesFile.exists()) {
                inMemoryFixedDevices = DeviceCatalogParser.parseDevices(
                    devicesFile.readText()
                ).devices
            } else {
                val assetFiles = listOf("realme.json", "oneplus.json", "oppo.json")
                val allDevices = mutableListOf<PredefinedDevice>()

                for (fileName in assetFiles) {
                    try {
                        val json = context.assets.open(fileName)
                            .bufferedReader().use { it.readText() }
                        allDevices.addAll(
                            DeviceCatalogParser.parseDevices(json).devices
                        )
                    } catch (_: Exception) {
                        // Skip missing or invalid asset files
                    }
                }

                inMemoryFixedDevices = allDevices
            }
        } catch (e: Exception) {
            inMemoryFixedDevices = emptyList()
        }
    }

    private fun migrateLegacyFavoritesIfNeeded() {
        if (favoritesPrefs.contains(KEY_FAVORITES_LEGACY_JSON) &&
            !favoritesPrefs.contains(KEY_FAVORITES_SET)
        ) {
            try {
                val json = favoritesPrefs.getString(KEY_FAVORITES_LEGACY_JSON, "{}") ?: "{}"
                val legacyMap = org.json.JSONObject(json)
                val favoriteNames = mutableSetOf<String>()
                legacyMap.keys().forEach { key ->
                    if (legacyMap.optBoolean(key)) favoriteNames.add(key)
                }
                favoritesPrefs.edit()
                    .putStringSet(KEY_FAVORITES_SET, favoriteNames)
                    .remove(KEY_FAVORITES_LEGACY_JSON)
                    .apply()
            } catch (_: Exception) {
                // Migration failure is non-fatal; favorites will simply be empty
            }
        }
    }

    private fun updateDevicesCache() {
        if (!cacheMutex.tryLock()) return
        try {
            val favorites = getFavorites()
            val fixedDevices = inMemoryFixedDevices.map { it.toDomain() }
            val customDevices = customDeviceManager.getCustomDevices().map { it.toDomain() }

            val allDevices = (fixedDevices + customDevices).distinctBy { it.name }

            _devicesFlow.value = allDevices
                .map { device -> device.copy(isFavorite = favorites.contains(device.name)) }
                .sortedByDescending { it.isFavorite }
        } finally {
            cacheMutex.unlock()
        }
    }

    override fun getDevices(): Flow<List<Device>> = _devicesFlow.asStateFlow()

    override fun getFavorites(): Set<String> {
        return favoritesPrefs.getStringSet(KEY_FAVORITES_SET, emptySet()) ?: emptySet()
    }

    override fun toggleFavoriteStatus(deviceName: String) {
        val favorites = getFavorites().toMutableSet()
        if (favorites.contains(deviceName)) {
            favorites.remove(deviceName)
        } else {
            favorites.add(deviceName)
        }
        favoritesPrefs.edit().putStringSet(KEY_FAVORITES_SET, favorites).apply()
        updateDevicesCache()
    }

    override fun addCustomDevice(device: PredefinedDevice) {
        customDeviceManager.addDevice(device)
        updateDevicesCache()
    }

    override fun updateCustomDevice(oldName: String, newDevice: PredefinedDevice) {
        customDeviceManager.updateDevice(oldName, newDevice)
        updateDevicesCache()
    }

    override fun deleteCustomDevice(deviceName: String) {
        customDeviceManager.deleteDevice(deviceName)
        updateDevicesCache()
    }

    private val _isSyncing = MutableStateFlow(false)
    override val isSyncing: kotlinx.coroutines.flow.StateFlow<Boolean> = _isSyncing.asStateFlow()

    override suspend fun syncCatalog() = withContext(Dispatchers.IO) {
        _isSyncing.value = true
        try {
            val allDevices = mutableListOf<PredefinedDevice>()

            // Fetch from all catalog endpoints
            val timestamp = System.currentTimeMillis()
            CATALOG_URLS.forEach { url ->
                try {
                    val request = Request.Builder()
                        .url("$url?t=$timestamp")
                        .cacheControl(okhttp3.CacheControl.FORCE_NETWORK)
                        .build()
                    val response = okHttpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        val json = response.body?.string()
                        if (!json.isNullOrBlank()) {
                            allDevices.addAll(
                                DeviceCatalogParser.parseDevices(json).devices
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Ignore errors for individual requests, continue with others
                }
            }

            if (allDevices.isNotEmpty()) {
                val combinedJson = gson.toJson(allDevices)
                devicesFile.writeText(combinedJson)
                inMemoryFixedDevices = allDevices
                updateDevicesCache()
            }
        } catch (e: Exception) {
            // Silently fail if there's no network or other error
        } finally {
            _isSyncing.value = false
        }
    }
}
