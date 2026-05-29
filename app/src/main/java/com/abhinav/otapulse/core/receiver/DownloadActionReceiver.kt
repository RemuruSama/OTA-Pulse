package com.abhinav.otapulse.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.abhinav.otapulse.core.download.OkHttpDownloadEngine
import com.abhinav.otapulse.core.model.toDownloadInfo
import com.abhinav.otapulse.core.notifications.DownloadNotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class DownloadActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var engine: OkHttpDownloadEngine

    @Inject
    lateinit var downloadManager: com.abhinav.otapulse.feature.downloads.data.DownloadManager

    @Inject
    lateinit var notificationHelper: DownloadNotificationHelper

    companion object {
        const val ACTION_PAUSE_DOWNLOAD = "com.abhinav.otapulse.ACTION_PAUSE_DOWNLOAD"
        const val ACTION_RESUME_DOWNLOAD = "com.abhinav.otapulse.ACTION_RESUME_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "com.abhinav.otapulse.ACTION_CANCEL_DOWNLOAD"
        const val ACTION_START_OTA_DOWNLOAD = "com.abhinav.otapulse.ACTION_START_OTA_DOWNLOAD"
        
        const val EXTRA_DOWNLOAD_ID = "com.abhinav.otapulse.EXTRA_DOWNLOAD_ID"
        const val EXTRA_OTA_UPDATE = "com.abhinav.otapulse.EXTRA_OTA_UPDATE"
        const val EXTRA_DEVICE_NAME = "com.abhinav.otapulse.EXTRA_DEVICE_NAME"
        const val EXTRA_REGION_NAME = "com.abhinav.otapulse.EXTRA_REGION_NAME"

        fun getPauseIntent(context: Context, downloadId: Int): Intent {
            return Intent(context, DownloadActionReceiver::class.java).apply {
                action = ACTION_PAUSE_DOWNLOAD
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
        }

        fun getResumeIntent(context: Context, downloadId: Int): Intent {
            return Intent(context, DownloadActionReceiver::class.java).apply {
                action = ACTION_RESUME_DOWNLOAD
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
        }

        fun getCancelIntent(context: Context, downloadId: Int): Intent {
            return Intent(context, DownloadActionReceiver::class.java).apply {
                action = ACTION_CANCEL_DOWNLOAD
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
        }

        fun getStartDownloadIntent(
            context: Context, 
            otaUpdate: com.abhinav.otapulse.core.model.OtaUpdate, 
            deviceName: String, 
            regionName: String
        ): Intent {
            return Intent(context, DownloadActionReceiver::class.java).apply {
                action = ACTION_START_OTA_DOWNLOAD
                putExtra(EXTRA_OTA_UPDATE, otaUpdate)
                putExtra(EXTRA_DEVICE_NAME, deviceName)
                putExtra(EXTRA_REGION_NAME, regionName)
            }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == null || context == null) {
            return
        }

        val action = intent.action
        
        if (action == ACTION_START_OTA_DOWNLOAD) {
            val otaUpdate = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(EXTRA_OTA_UPDATE, com.abhinav.otapulse.core.model.OtaUpdate::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(EXTRA_OTA_UPDATE)
            }
            val deviceName = intent.getStringExtra(EXTRA_DEVICE_NAME) ?: "Unknown Device"
            val regionName = intent.getStringExtra(EXTRA_REGION_NAME) ?: "GLO"
            
            if (otaUpdate != null) {
                downloadManager.enqueueDownload(otaUpdate, deviceName, regionName)
                
                // Close the notification panel if possible
                context.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
                
                // Cancel the original software update notification since we're acting on it
                notificationHelper.cancelNotification(301) // SOFTWARE_UPDATE_NOTIFICATION_ID
            }
            return
        }

        val downloadId = intent.getIntExtra(EXTRA_DOWNLOAD_ID, -1)
        if (downloadId == -1) return

        var shouldUpdateNotificationAfterAction = false

        when (action) {
            ACTION_PAUSE_DOWNLOAD -> {
                engine.pause(downloadId)
                shouldUpdateNotificationAfterAction = true
            }
            ACTION_RESUME_DOWNLOAD -> {
                engine.resume(downloadId)
                shouldUpdateNotificationAfterAction = true
            }
            ACTION_CANCEL_DOWNLOAD -> {
                engine.cancel(downloadId)
                notificationHelper.cancelNotification(downloadId)
            }
        }

        if (shouldUpdateNotificationAfterAction) {
            // Get the latest download info synchronously and update the notification.
            engine.getDownload(downloadId)?.let { record ->
                notificationHelper.showProgressNotification(record.toDownloadInfo())
            }
        }
    }
}
