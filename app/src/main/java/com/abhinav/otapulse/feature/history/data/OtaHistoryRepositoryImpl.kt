package com.abhinav.otapulse.feature.history.data

import android.content.Context
import android.content.SharedPreferences
import com.abhinav.otapulse.core.model.OtaHistoryEntry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OtaHistoryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : OtaHistoryRepository {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("ota_history_prefs", Context.MODE_PRIVATE)
    }
    private val gson = Gson()
    private val mutex = Mutex()
    private val _historyFlow = MutableStateFlow<List<OtaHistoryEntry>>(emptyList())

    init {
        loadHistory()
    }

    private fun loadHistory() {
        val json = prefs.getString("history_list", "[]")
        val type = object : TypeToken<List<OtaHistoryEntry>>() {}.type
        val list: List<OtaHistoryEntry> = try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        _historyFlow.value = list.sortedByDescending { it.timestamp }
    }

    private fun saveHistory(list: List<OtaHistoryEntry>) {
        val sortedList = list.sortedByDescending { it.timestamp }
        _historyFlow.value = sortedList
        prefs.edit().putString("history_list", gson.toJson(sortedList)).apply()
    }

    override fun getAllHistory(): Flow<List<OtaHistoryEntry>> {
        return _historyFlow
    }

    override fun getHistoryForDevice(deviceName: String): Flow<List<OtaHistoryEntry>> {
        return _historyFlow.map { list ->
            list.filter { it.deviceName == deviceName }
        }
    }

    override suspend fun logOtaUpdate(entry: OtaHistoryEntry) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val currentList = _historyFlow.value.toMutableList()
            
            // Check if we already have this exact OTA for this device+region (deduplication)
            // Use resolvedOtaVersion or manualUrl as unique identifier
            val isDuplicate = currentList.any { 
                it.deviceName == entry.deviceName && 
                it.region == entry.region &&
                (it.otaUpdate.versionName ?: it.otaUpdate.componentVersion) == (entry.otaUpdate.versionName ?: entry.otaUpdate.componentVersion) 
            }

            if (!isDuplicate) {
                currentList.add(entry)
                // Limit history to say, 500 entries total to prevent SharedPreferences bloat
                if (currentList.size > 500) {
                    currentList.sortByDescending { it.timestamp }
                    saveHistory(currentList.take(500))
                } else {
                    saveHistory(currentList)
                }
            }
        }
    }

    override suspend fun clearHistoryForDevice(deviceName: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val currentList = _historyFlow.value.toMutableList()
            currentList.removeAll { it.deviceName == deviceName }
            saveHistory(currentList)
        }
    }

    override suspend fun clearAllHistory() = withContext(Dispatchers.IO) {
        mutex.withLock {
            saveHistory(emptyList())
        }
    }
}
