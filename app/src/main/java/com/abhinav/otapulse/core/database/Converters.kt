package com.abhinav.otapulse.core.database

import androidx.room.TypeConverter
import com.abhinav.otapulse.core.model.OtaUpdate
import com.google.gson.Gson

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromOtaUpdate(otaUpdate: OtaUpdate?): String? {
        return gson.toJson(otaUpdate)
    }

    @TypeConverter
    fun toOtaUpdate(data: String?): OtaUpdate? {
        if (data == null) return null
        return try {
            gson.fromJson(data, OtaUpdate::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
