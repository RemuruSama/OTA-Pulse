package com.abhinav.otapulse.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.abhinav.otapulse.feature.history.data.local.OtaHistoryDao
import com.abhinav.otapulse.feature.history.data.local.OtaHistoryEntity

@Database(entities = [OtaHistoryEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun otaHistoryDao(): OtaHistoryDao
}
