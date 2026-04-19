package com.abhinav.otapulse.di

import android.content.Context
import android.content.SharedPreferences
import com.abhinav.otapulse.core.notifications.DownloadNotificationHelper
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @FavoritesPrefs
    fun provideFavoritesSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("favorites_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    @CustomDevicesPrefs
    fun provideCustomDevicesSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("custom_devices_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }

    @Provides
    @Singleton
    fun provideOtaExtractor(@ApplicationContext context: Context): com.abhinav.otapulse.ota.engine.OtaExtractor {
        return com.abhinav.otapulse.ota.engine.OtaExtractor(context)
    }
}
