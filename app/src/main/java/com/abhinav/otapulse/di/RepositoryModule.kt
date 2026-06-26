package com.abhinav.otapulse.di

import com.abhinav.otapulse.feature.settings.AppUpdateRepositoryImpl
import com.abhinav.otapulse.catalog.repository.DeviceRepositoryImpl
import com.abhinav.otapulse.ota.engine.OtaRepositoryImpl
import com.abhinav.otapulse.feature.settings.AppUpdateRepository
import com.abhinav.otapulse.catalog.repository.DeviceRepository
import com.abhinav.otapulse.ota.engine.OtaRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAppUpdateRepository(
        appUpdateRepositoryImpl: AppUpdateRepositoryImpl
    ): AppUpdateRepository

    @Binds
    @Singleton
    abstract fun bindOtaRepository(
        otaRepositoryImpl: OtaRepositoryImpl
    ): OtaRepository

    @Binds
    @Singleton
    abstract fun bindDeviceRepository(
        deviceRepositoryImpl: DeviceRepositoryImpl
    ): DeviceRepository

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(
        downloadManager: com.abhinav.otapulse.feature.downloads.data.DownloadManager
    ): com.abhinav.otapulse.feature.downloads.domain.DownloadRepository

    @Binds
    @Singleton
    abstract fun bindOtaHistoryRepository(
        impl: com.abhinav.otapulse.feature.history.data.OtaHistoryRepositoryImpl
    ): com.abhinav.otapulse.feature.history.data.OtaHistoryRepository

    companion object {
        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .fastFallback(true)
                .retryOnConnectionFailure(true)
                .build()
        }
    }
}
