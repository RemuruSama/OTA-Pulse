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

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        scheduleSoftwareUpdateCheck()
    }

    private fun scheduleSoftwareUpdateCheck() {
        val prefs = getSharedPreferences(
            com.abhinav.otapulse.feature.settings.SettingsFragment.APP_SETTINGS_PREFS,
            MODE_PRIVATE
        )
        val isEnabled = prefs.getBoolean(
            com.abhinav.otapulse.feature.settings.SettingsFragment.PREF_AUTO_SOFTWARE_UPDATE_CHECK,
            true
        )
        if (!isEnabled) return

        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()

        val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.abhinav.otapulse.core.worker.SoftwareUpdateCheckWorker>(
            6, java.util.concurrent.TimeUnit.HOURS
        ).setConstraints(constraints).build()

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            com.abhinav.otapulse.core.worker.SoftwareUpdateCheckWorker.WORK_NAME,
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
