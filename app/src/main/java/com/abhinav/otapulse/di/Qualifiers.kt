package com.abhinav.otapulse.di

import javax.inject.Qualifier

/**
 * Qualifier for SharedPreferences used by FavoritesManager.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FavoritesPrefs

/**
 * Qualifier for SharedPreferences used by CustomDeviceManager.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CustomDevicesPrefs
