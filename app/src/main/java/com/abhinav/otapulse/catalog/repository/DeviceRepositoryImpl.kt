package com.abhinav.otapulse.catalog.repository

import android.content.Context
import android.content.SharedPreferences
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.core.common.toDomain
import com.abhinav.otapulse.core.model.Device
import com.abhinav.otapulse.di.FavoritesPrefs
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
        private const val CATALOG_URL = "https://raw.githubusercontent.com/RemuruSama/OTA-Pulse/main/catalog/devices.json"
    }

    init {
        migrateLegacyFavoritesIfNeeded()
        loadLocalCatalog()
        updateDevicesCache()
    }

    private fun loadLocalCatalog() {
        val json = try {
            if (devicesFile.exists()) {
                devicesFile.readText()
            } else {
                context.assets.open("devices.json").bufferedReader().use { it.readText() }
            }
        } catch (e: Exception) {
            "[]"
        }

        try {
            val type = object : TypeToken<List<PredefinedDevice>>() {}.type
            inMemoryFixedDevices = gson.fromJson(json, type) ?: emptyList()
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
            val request = Request.Builder().url(CATALOG_URL).build()
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val json = response.body?.string()
                if (!json.isNullOrBlank()) {
                    // Verify it's parseable
                    val type = object : TypeToken<List<PredefinedDevice>>() {}.type
                    val devices: List<PredefinedDevice> = gson.fromJson(json, type) ?: emptyList()
                    if (devices.isNotEmpty()) {
                        devicesFile.writeText(json)
                        inMemoryFixedDevices = devices
                        updateDevicesCache()
                    }
                }
            }
        } catch (e: Exception) {
            // Silently fail if there's no network or other error
        } finally {
            _isSyncing.value = false
        }
    }
}
