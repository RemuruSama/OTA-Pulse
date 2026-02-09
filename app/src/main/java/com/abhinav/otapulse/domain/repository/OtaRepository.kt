package com.abhinav.otapulse.domain.repository

import com.abhinav.otapulse.domain.model.OtaRequest
import com.abhinav.otapulse.util.NetworkComponent

interface OtaRepository {
    suspend fun fetchOtaUpdate(request: OtaRequest): Result<List<com.abhinav.otapulse.util.NetworkComponent>>
    fun getDevices(): kotlinx.coroutines.flow.Flow<List<com.abhinav.otapulse.domain.model.Device>>
    fun getFavorites(): Map<String, Boolean>
    fun toggleFavoriteStatus(deviceName: String, isFavorite: Boolean)
    fun addCustomDevice(device: com.abhinav.otapulse.util.PredefinedDevice)
    fun updateCustomDevice(oldName: String, newDevice: com.abhinav.otapulse.util.PredefinedDevice)
    fun deleteCustomDevice(deviceName: String)
}
