package com.abhinav.otapulse.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.abhinav.otapulse.domain.model.DownloadInfo
import com.abhinav.otapulse.util.DownloadNotificationHelper
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Fetch
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class DownloadActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var fetch: Fetch

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
                fetch.pause(downloadId)
                shouldUpdateNotificationAfterAction = true
            }
            ACTION_RESUME_DOWNLOAD -> {
                fetch.resume(downloadId)
                shouldUpdateNotificationAfterAction = true
            }
            ACTION_CANCEL_DOWNLOAD -> {
                fetch.cancel(downloadId)
                notificationHelper.cancelNotification(downloadId)
                // Notification is now cancelled; no further update from here is needed.
                // Your FetchListener's onCancelled method should handle any other cleanup.
            }
        }

        if (shouldUpdateNotificationAfterAction) {
            // Attempt to get the latest download info and update the notification.
            // Using Fetch's getDownload with a callback is asynchronous.
            fetch.getDownload(downloadId) { fetchDownload: com.tonyodev.fetch2.Download? ->
                fetchDownload?.let {
                    // IMPORTANT: The status here (it.status) depends on how quickly Fetch
                    // processes the pause/resume action and updates its database relative
                    // to this callback firing. A FetchListener is generally more reliable
                    // for getting confirmed status changes.
                    val downloadInfo = mapFetchDownloadToDownloadInfo(it)
                    notificationHelper.showProgressNotification(downloadInfo)
                }
            }
        }
    }

    private fun mapFetchDownloadToDownloadInfo(fetchDownload: com.tonyodev.fetch2.Download): DownloadInfo {
        return DownloadInfo(
            id = fetchDownload.id,
            file = fetchDownload.file, // Added: Use the full file path
            fileName = File(fetchDownload.file).name,
            progress = fetchDownload.progress,
            speed = fetchDownload.downloadedBytesPerSecond,
            eta = fetchDownload.etaInMilliSeconds,
            status = fetchDownload.status,
            error = fetchDownload.error ?: Error.NONE,
            totalBytes = fetchDownload.total, // Added: Use total bytes from fetchDownload
            original = fetchDownload, // Added: Pass the original fetchDownload object
            otaUpdate = null, // Added: Pass null for now, adjust if needed
            deviceName = "", // Added: Pass empty string for now, adjust if needed
            regionName = "" // Added: Pass empty string for now, adjust if needed
        )
    }
}
