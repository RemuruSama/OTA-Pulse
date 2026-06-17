package com.abhinav.otapulse.feature.history.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OtaHistoryDao {

    @Query("SELECT * FROM ota_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<OtaHistoryEntity>>

    @Query("SELECT * FROM ota_history WHERE deviceName = :deviceName ORDER BY timestamp DESC")
    fun getHistoryForDevice(deviceName: String): Flow<List<OtaHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entry: OtaHistoryEntity): Long

    @Query("DELETE FROM ota_history WHERE deviceName = :deviceName")
    fun deleteForDevice(deviceName: String): Int

    @Query("DELETE FROM ota_history")
    fun clearAll(): Int

    // Used for checking duplicates
    @Query("SELECT * FROM ota_history WHERE deviceName = :deviceName AND region = :region ORDER BY timestamp DESC LIMIT 10")
    fun getRecentHistoryForDeviceAndRegion(deviceName: String, region: String): List<OtaHistoryEntity>
}
