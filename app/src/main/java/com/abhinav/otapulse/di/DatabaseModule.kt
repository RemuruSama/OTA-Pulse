package com.abhinav.otapulse.di

import android.content.Context
import androidx.room.Room
import com.abhinav.otapulse.core.database.AppDatabase
import com.abhinav.otapulse.feature.history.data.local.OtaHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "otapulse_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideOtaHistoryDao(appDatabase: AppDatabase): OtaHistoryDao {
        return appDatabase.otaHistoryDao()
    }
}
