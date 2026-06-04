package com.abhinav.otapulse.core.download

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground service that keeps the process alive while downloads are active.
 *
 * Acquires a partial [PowerManager.WakeLock] and [WifiManager.WifiLock] to prevent
 * the CPU and Wi-Fi radio from sleeping during large OTA file downloads.
 *
 * The service is started by [DownloadManager] when a download is enqueued and
 * stopped when no active downloads remain.
 */
@AndroidEntryPoint
class DownloadForegroundService : Service() {

    @Inject lateinit var engine: OkHttpDownloadEngine

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    companion object {
        private const val TAG = "DownloadFgService"
        private const val NOTIFICATION_ID = 44300
        private const val WAKELOCK_TAG = "OtaPulse:DownloadWakeLock"
        private const val WIFILOCK_TAG = "OtaPulse:DownloadWifiLock"
        private const val MAX_WAKELOCK_TIMEOUT_MS = 4L * 60 * 60 * 1000 // 4 hours

        fun start(context: Context) {
            val intent = Intent(context, DownloadForegroundService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start foreground service", e)
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, DownloadForegroundService::class.java))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop foreground service", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
        acquireWifiLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        promoteToForeground()
        return START_STICKY
    }

    override fun onDestroy() {
        releaseWakeLock()
        releaseWifiLock()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun promoteToForeground() {
        // Build a minimal, standalone notification — no action buttons, no progress bar.
        // Per-download notifications are shown separately by DownloadNotificationHelper.
        val openIntent = android.app.PendingIntent.getActivity(
            this,
            0,
            Intent(this, com.abhinav.otapulse.app.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                action = com.abhinav.otapulse.app.MainActivity.ACTION_OPEN_DOWNLOADS
            },
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = androidx.core.app.NotificationCompat.Builder(this, "ota_pulse_progress")
            .setSmallIcon(com.abhinav.otapulse.R.drawable.ic_download)
            .setContentTitle(getString(com.abhinav.otapulse.R.string.app_name))
            .setContentText(getString(com.abhinav.otapulse.R.string.notif_downloading_background))
            .setContentIntent(openIntent)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_SERVICE)
            .build()

        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                else 0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground", e)
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG).apply {
                acquire(MAX_WAKELOCK_TIMEOUT_MS)
            }
            Log.d(TAG, "WakeLock acquired")
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "WakeLock released")
            }
        }
        wakeLock = null
    }

    private fun acquireWifiLock() {
        if (wifiLock == null) {
            val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            @Suppress("DEPRECATION")
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                WifiManager.WIFI_MODE_FULL_LOW_LATENCY
            else
                WifiManager.WIFI_MODE_FULL_HIGH_PERF
            wifiLock = wm?.createWifiLock(mode, WIFILOCK_TAG)?.apply {
                acquire()
            }
            Log.d(TAG, "WifiLock acquired")
        }
    }

    private fun releaseWifiLock() {
        wifiLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "WifiLock released")
            }
        }
        wifiLock = null
    }
}
