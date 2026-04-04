package com.abhinav.otapulse.catalog.repository

import android.content.SharedPreferences
import com.abhinav.otapulse.di.FavoritesPrefs
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesManager @Inject constructor(
    @FavoritesPrefs private val sharedPreferences: SharedPreferences
) {
    fun addFavorite(deviceName: String, region: String) {
        // FIX: Use apply() for asynchronous save
        sharedPreferences.edit().putString(deviceName, region).apply()
    }

    fun removeFavorite(deviceName: String) {
        // FIX: Use apply() for asynchronous save
        sharedPreferences.edit().remove(deviceName).apply()
    }

    fun getFavoritedRegion(deviceName: String): String? {
        return sharedPreferences.getString(deviceName, null)
    }

    fun getFavorites(): Map<String, String> {
        @Suppress("UNCHECKED_CAST")
        return sharedPreferences.all as Map<String, String>
    }
}
