package com.abhinav.otapulse.core.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.abhinav.otapulse.R
import com.abhinav.otapulse.app.MainActivity
import com.abhinav.otapulse.core.common.FormatUtils
import com.abhinav.otapulse.core.model.DownloadInfo
import com.abhinav.otapulse.core.receiver.DownloadActionReceiver
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
        private const val CHANNEL_ID_PROGRESS = "ota_pulse_progress"
        private const val CHANNEL_ID_ALERTS = "ota_pulse_alerts"

        private const val REQUEST_CODE_PAUSE = 100
        private const val REQUEST_CODE_RESUME = 101
        private const val REQUEST_CODE_CANCEL = 102
        private const val NOTIFICATION_GROUP = "ota_pulse_downloads"
        private const val SUMMARY_ID = 44321 
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val progressChannel = NotificationChannel(
                CHANNEL_ID_PROGRESS,
                context.getString(R.string.notif_channel_progress),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notif_channel_progress_desc)
                setSound(null, null)
            }

            val alertChannel = NotificationChannel(
                CHANNEL_ID_ALERTS,
                context.getString(R.string.notif_channel_alerts),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_alerts_desc)
            }

            notificationManager.createNotificationChannel(progressChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    /**
     * Shows or updates the group summary notification.
     * Required to prevent SystemUI crashes in grouped notifications.
     */
    fun showSummaryNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val summaryNotification = NotificationCompat.Builder(context, CHANNEL_ID_PROGRESS)
            .setSmallIcon(R.drawable.ic_download)
            .setContentIntent(createOpenDownloadsPendingIntent())
            .setGroup(NOTIFICATION_GROUP)
            .setGroupSummary(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentTitle(context.getString(R.string.title_downloads))
            .setContentText(context.getString(R.string.app_name))
            .setSubText(context.getString(R.string.app_name))
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setStyle(NotificationCompat.InboxStyle()
                .setSummaryText(context.getString(R.string.title_downloads)))
            .setAutoCancel(true)
            .setSilent(true)
            .build()
        
        notificationManager.notify(SUMMARY_ID, summaryNotification)
    }

    /**
     * Builds a progress notification without posting it.
     * Useful for Foreground Service initialization to ensure grouping consistency.
     */
    fun getProgressNotificationBuilder(downloadInfo: DownloadInfo): NotificationCompat.Builder {
        val progressText = "${downloadInfo.progress}%"

        val contentText = if (downloadInfo.status == Status.PAUSED) {
            "$progressText - ${context.getString(R.string.notif_paused)}"
        } else {
            val speedText = FormatUtils.formatDownloadSpeed(downloadInfo.speed)
            val etaText = formatEta(downloadInfo.eta)
            "$progressText • $speedText • $etaText"
        }

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
            .setContentTitle(title.ifBlank { context.getString(R.string.app_name) })
            .setContentText(contentText.ifBlank { "..." })
            .setContentIntent(createOpenDownloadsPendingIntent())
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setGroup(NOTIFICATION_GROUP)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setProgress(100, downloadInfo.progress, false)
            .setShowWhen(true)

        // Cancel Action
        val cancelIntent = DownloadActionReceiver.getCancelIntent(context, downloadInfo.id)
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_CANCEL + downloadInfo.id,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(R.drawable.ic_close, context.getString(R.string.notif_cancel), cancelPendingIntent)

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
                builder.addAction(R.drawable.ic_pause, context.getString(R.string.notif_pause), pausePendingIntent)
            }
            Status.PAUSED -> {
                val resumeIntent = DownloadActionReceiver.getResumeIntent(context, downloadInfo.id)
                val resumePendingIntent = PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE_RESUME + downloadInfo.id,
                    resumeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(R.drawable.ic_play_arrow, context.getString(R.string.notif_resume), resumePendingIntent)
            }
            else -> {}
        }

        return builder
    }

    fun showProgressNotification(downloadInfo: DownloadInfo) {
        val builder = getProgressNotificationBuilder(downloadInfo)
        
        showSummaryNotification()
        notify(downloadInfo.id, builder)
    }

    fun showAppUpdateNotification(info: com.abhinav.otapulse.core.model.AppUpdateInfo) {
        val title = context.getString(R.string.update_available_title, info.version)
        val contentText = context.getString(R.string.tap_to_view_update)

        val intent = Intent(context, com.abhinav.otapulse.app.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            action = "com.abhinav.otapulse.ACTION_SHOW_UPDATE_DIALOG"
            putExtra("update_version", info.version)
            putExtra("update_url", info.downloadUrl)
            putExtra("update_changelog", info.changelog)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            200, 
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notify(200, builder)
    }

    fun showCompletedNotification(downloadInfo: DownloadInfo) {
        val title = context.getString(R.string.notif_download_complete)
        val contentText = downloadInfo.fileName

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setGroup(NOTIFICATION_GROUP)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setProgress(0, 0, false)

        showSummaryNotification()
        notify(downloadInfo.id, builder)
    }

    fun showErrorNotification(downloadInfo: DownloadInfo) {
        val title = context.getString(R.string.notif_download_failed)

        val errorDescription = when (downloadInfo.error) {
            Error.REQUEST_NOT_SUCCESSFUL -> context.getString(R.string.notif_error_link_expired)
            Error.HTTP_NOT_FOUND -> context.getString(R.string.notif_error_not_found)
            Error.NO_NETWORK_CONNECTION -> context.getString(R.string.notif_error_no_network)
            Error.UNKNOWN -> context.getString(R.string.notif_error_unknown)
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
            .setGroup(NOTIFICATION_GROUP)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setProgress(0, 0, false)

        showSummaryNotification()
        notify(downloadInfo.id, builder)
    }

    fun cancelNotification(downloadId: Int) {
        notificationManager.cancel(downloadId)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val activeNotifications = nm.activeNotifications
            val groupActive = activeNotifications.any { 
                it.id != SUMMARY_ID && it.notification.group == NOTIFICATION_GROUP
            }
            if (!groupActive) {
                notificationManager.cancel(SUMMARY_ID)
            }
        }
    }

    private fun notify(id: Int, builder: NotificationCompat.Builder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        notificationManager.notify(id, builder.build())
    }

    private fun createOpenDownloadsPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            action = MainActivity.ACTION_OPEN_DOWNLOADS
        }
        return PendingIntent.getActivity(
            context,
            300,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
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
