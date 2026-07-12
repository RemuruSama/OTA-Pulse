package com.abhinav.otapulse.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class OtaPulseApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var appSettingsPreferences: com.abhinav.otapulse.core.preferences.AppSettingsPreferences

    override val workManagerConfiguration: Configuration by lazy {
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        scheduleSoftwareUpdateCheck()
    }

    private fun scheduleSoftwareUpdateCheck() {
        val appSettings = appSettingsPreferences.getAppSettings()
        if (!appSettings.autoSoftwareUpdateCheck) return

        val intervalHours = appSettings.checkIntervalHours

        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()

        val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.abhinav.otapulse.core.worker.SoftwareUpdateCheckWorker>(
            intervalHours, java.util.concurrent.TimeUnit.HOURS
        ).setConstraints(constraints)
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                10000L, // 10 seconds MIN_BACKOFF_MILLIS value
                java.util.concurrent.TimeUnit.MILLISECONDS
            )
            .build()

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            com.abhinav.otapulse.core.worker.SoftwareUpdateCheckWorker.WORK_NAME,
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
