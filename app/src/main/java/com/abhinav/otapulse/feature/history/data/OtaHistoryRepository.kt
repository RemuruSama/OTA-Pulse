package com.abhinav.otapulse.feature.history.data

import com.abhinav.otapulse.core.model.OtaHistoryEntry
import kotlinx.coroutines.flow.Flow

interface OtaHistoryRepository {
    fun getAllHistory(): Flow<List<OtaHistoryEntry>>
    fun getHistoryForDevice(deviceName: String): Flow<List<OtaHistoryEntry>>
    suspend fun logOtaUpdate(entry: OtaHistoryEntry)
    suspend fun clearHistoryForDevice(deviceName: String)
    suspend fun clearAllHistory()
}
