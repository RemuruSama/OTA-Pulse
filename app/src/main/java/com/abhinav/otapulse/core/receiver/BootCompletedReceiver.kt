package com.abhinav.otapulse.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.abhinav.otapulse.core.worker.SoftwareUpdateCheckWorker
import com.abhinav.otapulse.feature.settings.SettingsFragment
import java.util.concurrent.TimeUnit
import androidx.work.BackoffPolicy
import java.time.Duration

class BootCompletedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            Log.d(TAG, "Device booted ($action), checking if auto software update check is enabled")
            scheduleSoftwareUpdateCheck(context)
        }
    }

    private fun scheduleSoftwareUpdateCheck(context: Context) {
        val prefs = context.getSharedPreferences(
            SettingsFragment.APP_SETTINGS_PREFS,
            Context.MODE_PRIVATE
        )
        val isEnabled = prefs.getBoolean(
            SettingsFragment.PREF_AUTO_SOFTWARE_UPDATE_CHECK,
            true
        )
        if (!isEnabled) {
            Log.d(TAG, "Auto software update check is disabled, skipping scheduling")
            return
        }

        Log.d(TAG, "Scheduling SoftwareUpdateCheckWorker")

        val intervalHours = prefs.getLong(
            SettingsFragment.PREF_CHECK_INTERVAL_HOURS,
            SettingsFragment.DEFAULT_CHECK_INTERVAL_HOURS
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<SoftwareUpdateCheckWorker>(
            intervalHours, TimeUnit.HOURS
        ).setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10000L, // 10 seconds MIN_BACKOFF_MILLIS value
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SoftwareUpdateCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
