package com.abhinav.otapulse.catalog.repository

import android.content.SharedPreferences
import com.abhinav.otapulse.catalog.repository.CustomDeviceManager
import com.abhinav.otapulse.di.FavoritesPrefs
import com.abhinav.otapulse.core.model.Device
import com.abhinav.otapulse.catalog.repository.DeviceRepository
import com.abhinav.otapulse.catalog.DeviceCatalog
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.core.common.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    @FavoritesPrefs private val favoritesPrefs: SharedPreferences,
    private val customDeviceManager: CustomDeviceManager
) : DeviceRepository {

    private val _devicesFlow = MutableStateFlow<List<Device>>(emptyList())

    // Mutex prevents concurrent read-modify-write on _devicesFlow
    private val cacheMutex = Mutex()

    companion object {
        private const val KEY_FAVORITES_SET = "favorites_set"
        private const val KEY_FAVORITES_LEGACY_JSON = "favorites"
    }

    init {
        migrateLegacyFavoritesIfNeeded()
        updateDevicesCache()
    }

    /**
     * One-time migration from the old JSON-string favorites format to a StringSet.
     * Runs only if the new key is absent but the old key is present.
     */
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
            val fixedDevices = DeviceCatalog.predefinedDevices.map { it.toDomain() }
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
}
