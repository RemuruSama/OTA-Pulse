package com.abhinav.otapulse.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.abhinav.otapulse.R
import com.abhinav.otapulse.domain.model.DownloadInfo
import com.abhinav.otapulse.receiver.DownloadActionReceiver
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Status
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = NotificationManagerCompat.from(context)

    companion object {
        // Split channels: One for silent progress, one for important alerts (Error/Complete)
        private const val CHANNEL_ID_PROGRESS = "ota_pulse_progress"
        private const val CHANNEL_ID_ALERTS = "ota_pulse_alerts"

        private const val REQUEST_CODE_PAUSE = 100
        private const val REQUEST_CODE_RESUME = 101
        private const val REQUEST_CODE_CANCEL = 102
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 1. Silent Channel for Progress Updates (Low Importance)
            val progressChannel = NotificationChannel(
                CHANNEL_ID_PROGRESS,
                "Download Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active download progress"
                setSound(null, null)
            }

            // 2. Alert Channel for Completion/Errors (Default/High Importance)
            val alertChannel = NotificationChannel(
                CHANNEL_ID_ALERTS,
                "Download Status",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for completed or failed downloads"
            }

            notificationManager.createNotificationChannel(progressChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    fun showProgressNotification(downloadInfo: DownloadInfo) {
        val progressText = "${downloadInfo.progress}%"

        val contentText = if (downloadInfo.status == Status.PAUSED) {
            "$progressText - Paused"
        } else {
            // Use shared FormatUtils for consistency with App UI
            val speedText = FormatUtils.formatDownloadSpeed(downloadInfo.speed)
            val etaText = formatEta(downloadInfo.eta)
            "$progressText • $speedText • $etaText"
        }

        // Fix: For Manual/Direct downloads (External/Unknown Version), show the filename instead of "External - Unknown Version"
        val isStandardUpdate = downloadInfo.regionName.isNotBlank() && 
                               downloadInfo.otaUpdate?.versionName?.isNotBlank() == true &&
                               downloadInfo.otaUpdate.versionName != "Unknown Version" &&
                               downloadInfo.regionName != "External"

        val title = if (isStandardUpdate) {
            "${downloadInfo.regionName} - ${downloadInfo.otaUpdate!!.versionName}"
        } else {
            downloadInfo.fileName
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_PROGRESS)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true) // Prevents sound/vibration on every update
            .setProgress(100, downloadInfo.progress, false)

        // Cancel Action
        val cancelIntent = DownloadActionReceiver.getCancelIntent(context, downloadInfo.id)
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_CANCEL + downloadInfo.id,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(R.drawable.ic_close, "Cancel", cancelPendingIntent)

        // Pause/Resume Action
        when (downloadInfo.status) {
            Status.DOWNLOADING, Status.QUEUED -> {
                val pauseIntent = DownloadActionReceiver.getPauseIntent(context, downloadInfo.id)
                val pausePendingIntent = PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE_PAUSE + downloadInfo.id,
                    pauseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(R.drawable.ic_pause, "Pause", pausePendingIntent)
            }
            Status.PAUSED -> {
                val resumeIntent = DownloadActionReceiver.getResumeIntent(context, downloadInfo.id)
                val resumePendingIntent = PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE_RESUME + downloadInfo.id,
                    resumeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(R.drawable.ic_play_arrow, "Resume", resumePendingIntent)
            }
            else -> {}
        }

        notify(downloadInfo.id, builder)
    }

    fun showCompletedNotification(downloadInfo: DownloadInfo) {
        val title = "Download Complete"
        val contentText = downloadInfo.fileName

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setProgress(0, 0, false)

        notify(downloadInfo.id, builder)
    }

    fun showErrorNotification(downloadInfo: DownloadInfo) {
        val title = "Download Failed"

        // Friendly Error Mapping
        val errorDescription = when (downloadInfo.error) {
            Error.REQUEST_NOT_SUCCESSFUL -> "Link expired. Please retry in app."
            Error.HTTP_NOT_FOUND -> "File not found on server."
            Error.NO_NETWORK_CONNECTION -> "No internet connection."
            Error.UNKNOWN -> "Unknown error. Please retry."
            else -> downloadInfo.error.toString().replace("_", " ")
        }

        val contentText = "$errorDescription"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setProgress(0, 0, false)

        notify(downloadInfo.id, builder)
    }

    fun cancelNotification(downloadId: Int) {
        notificationManager.cancel(downloadId)
    }

    private fun notify(id: Int, builder: NotificationCompat.Builder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        notificationManager.notify(id, builder.build())
    }

    private fun formatEta(etaInMilliSeconds: Long): String {
        if (etaInMilliSeconds <= 0) return "--"
        val hours = TimeUnit.MILLISECONDS.toHours(etaInMilliSeconds)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(etaInMilliSeconds) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(etaInMilliSeconds) % 60

        return when {
            hours > 0 -> String.format("%dh %02dm", hours, minutes)
            minutes > 0 -> String.format("%02dm %02ds", minutes, seconds)
            else -> String.format("%ds", seconds)
        }
    }
}