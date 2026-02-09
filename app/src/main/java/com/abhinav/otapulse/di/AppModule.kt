package com.abhinav.otapulse.di

import android.content.Context
import android.content.SharedPreferences
import com.abhinav.otapulse.util.DownloadNotificationHelper
import com.abhinav.otapulse.util.OtaFetchNotificationManager
import com.google.gson.Gson
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchConfiguration
import com.tonyodev.fetch2.HttpUrlConnectionDownloader
import com.tonyodev.fetch2core.Downloader
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
    @FavoritesPrefs // Use the qualifier for favorites
    fun provideFavoritesSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("favorites_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    @CustomDevicesPrefs // Use the qualifier for custom devices
    fun provideCustomDevicesSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("custom_devices_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideFetch(
        @ApplicationContext context: Context,
        notificationHelper: DownloadNotificationHelper
    ): Fetch {
        val fetchNotificationManager = OtaFetchNotificationManager(context, notificationHelper)
        
        val fetchConfiguration = FetchConfiguration.Builder(context)
            .setDownloadConcurrentLimit(3)
            .setHttpDownloader(HttpUrlConnectionDownloader(Downloader.FileDownloaderType.PARALLEL))
            .setNotificationManager(fetchNotificationManager) // Enable Foreground Service
            .build()

        return Fetch.getInstance(fetchConfiguration)
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }
}
