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
    lateinit var notificationHelper: DownloadNotificationHelper

    companion object {
        const val ACTION_PAUSE_DOWNLOAD = "com.abhinav.otapulse.ACTION_PAUSE_DOWNLOAD"
        const val ACTION_RESUME_DOWNLOAD = "com.abhinav.otapulse.ACTION_RESUME_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "com.abhinav.otapulse.ACTION_CANCEL_DOWNLOAD"
        const val EXTRA_DOWNLOAD_ID = "com.abhinav.otapulse.EXTRA_DOWNLOAD_ID"

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
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val downloadId = intent?.getIntExtra(EXTRA_DOWNLOAD_ID, -1) ?: -1
        if (downloadId == -1 || intent?.action == null || context == null) {
            return
        }

        val action = intent.action
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
