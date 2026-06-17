package com.abhinav.otapulse.feature.history.data

import android.content.Context
import android.content.SharedPreferences
import com.abhinav.otapulse.core.model.OtaHistoryEntry
import com.abhinav.otapulse.feature.history.data.local.OtaHistoryDao
import com.abhinav.otapulse.feature.history.data.local.OtaHistoryEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OtaHistoryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: OtaHistoryDao
) : OtaHistoryRepository {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("ota_history_prefs", Context.MODE_PRIVATE)
    }
    private val gson = Gson()

    init {
        migrateFromSharedPreferences()
    }

    private fun migrateFromSharedPreferences() {
        val json = prefs.getString("history_list", null)
        if (!json.isNullOrBlank() && json != "[]") {
            try {
                val type = object : TypeToken<List<OtaHistoryEntry>>() {}.type
                val list: List<OtaHistoryEntry> = gson.fromJson(json, type) ?: emptyList()
                
                // Fire and forget migration
                CoroutineScope(Dispatchers.IO).launch {
                    list.forEach { entry ->
                        dao.insert(
                            OtaHistoryEntity(
                                timestamp = entry.timestamp,
                                deviceName = entry.deviceName,
                                region = entry.region,
                                otaUpdate = entry.otaUpdate.copy(rawJson = null) // strip rawJson on migration
                            )
                        )
                    }
                    prefs.edit().remove("history_list").apply()
                }
            } catch (e: Throwable) {
                // Ignore parse errors, just clear to prevent further crashes
                prefs.edit().remove("history_list").apply()
            }
        }
    }

    override fun getAllHistory(): Flow<List<OtaHistoryEntry>> {
        return dao.getAllHistory().map { list ->
            list.map { entity ->
                OtaHistoryEntry(
                    timestamp = entity.timestamp,
                    deviceName = entity.deviceName,
                    region = entity.region,
                    otaUpdate = entity.otaUpdate
                )
            }
        }
    }

    override fun getHistoryForDevice(deviceName: String): Flow<List<OtaHistoryEntry>> {
        return dao.getHistoryForDevice(deviceName).map { list ->
            list.map { entity ->
                OtaHistoryEntry(
                    timestamp = entity.timestamp,
                    deviceName = entity.deviceName,
                    region = entity.region,
                    otaUpdate = entity.otaUpdate
                )
            }
        }
    }

    override suspend fun logOtaUpdate(entry: OtaHistoryEntry) = withContext(Dispatchers.IO) {
        // Strip rawJson to prevent database bloat
        val entryToSave = entry.copy(
            otaUpdate = entry.otaUpdate.copy(rawJson = null)
        )
        
        // Basic deduplication: Check recent history
        val recentHistory = dao.getRecentHistoryForDeviceAndRegion(entryToSave.deviceName, entryToSave.region)
        val isDuplicate = recentHistory.any {
            (it.otaUpdate.versionName ?: it.otaUpdate.componentVersion) == (entryToSave.otaUpdate.versionName ?: entryToSave.otaUpdate.componentVersion)
        }

        if (!isDuplicate) {
            dao.insert(
                OtaHistoryEntity(
                    timestamp = entryToSave.timestamp,
                    deviceName = entryToSave.deviceName,
                    region = entryToSave.region,
                    otaUpdate = entryToSave.otaUpdate
                )
            )
        }
    }

    override suspend fun clearHistoryForDevice(deviceName: String) {
        withContext(Dispatchers.IO) {
            dao.deleteForDevice(deviceName)
        }
    }

    override suspend fun clearAllHistory() {
        withContext(Dispatchers.IO) {
            dao.clearAll()
        }
    }
}
