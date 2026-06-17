package com.abhinav.otapulse.catalog.repository

import com.abhinav.otapulse.core.model.Device
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository responsible for device catalog management, favorites, and custom devices.
 * Separated from [OtaRepository] so OTA-fetching logic remains focused.
 */
interface DeviceRepository {
    fun getDevices(): Flow<List<Device>>
    fun getFavorites(): Set<String>
    fun toggleFavoriteStatus(deviceName: String)
    fun addCustomDevice(device: PredefinedDevice)
    fun updateCustomDevice(oldName: String, newDevice: PredefinedDevice)
    fun deleteCustomDevice(deviceName: String)
    suspend fun syncCatalog()
    val isSyncing: StateFlow<Boolean>
}
