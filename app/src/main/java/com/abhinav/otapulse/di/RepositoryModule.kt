package com.abhinav.otapulse.di

import com.abhinav.otapulse.data.repository.AppUpdateRepositoryImpl
import com.abhinav.otapulse.data.repository.OtaRepositoryImpl
import com.abhinav.otapulse.domain.repository.AppUpdateRepository
import com.abhinav.otapulse.domain.repository.OtaRepository
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
    abstract fun bindDownloadRepository(
        downloadManager: com.abhinav.otapulse.data.repository.DownloadManager
    ): com.abhinav.otapulse.domain.repository.DownloadRepository

    companion object {
        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder().build()
        }
    }
}
