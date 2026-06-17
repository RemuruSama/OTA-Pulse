package com.abhinav.otapulse.feature.history.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.abhinav.otapulse.core.model.OtaUpdate

@Entity(tableName = "ota_history")
data class OtaHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val deviceName: String,
    val region: String,
    val otaUpdate: OtaUpdate
)
