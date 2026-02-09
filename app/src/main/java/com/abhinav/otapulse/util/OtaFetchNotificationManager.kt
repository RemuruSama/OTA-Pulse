package com.abhinav.otapulse.util

import android.content.Context
import com.abhinav.otapulse.domain.model.DownloadInfo
import com.abhinav.otapulse.domain.model.OtaUpdate
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.DefaultFetchNotificationManager
import com.tonyodev.fetch2.Status
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Custom FetchNotificationManager to integrate Fetch's foreground service
 * with our existing DownloadNotificationHelper UI.
 */
@Singleton
class OtaFetchNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationHelper: DownloadNotificationHelper
) : DefaultFetchNotificationManager(context) {



    override fun getFetchInstanceForNamespace(namespace: String): com.tonyodev.fetch2.Fetch {
        // This is only used if supportGroupNotifications is true, which we don't use.
        // Returning default instance (will likely throw if specific namespace requested, but we use default)
        throw UnsupportedOperationException("Namespace support not implemented")
    }

    override fun postDownloadUpdate(
        download: Download
    ): Boolean {
        // This is called by Fetch service when it wants to update the notification.
        // We delegate to our helper.

        // We only care about active states for the foreground service notification.
        // Completed/Error states are handled by DownloadManager's callbacks to show
        // a separate, dismissible notification.
        if (download.status == Status.DOWNLOADING || 
            download.status == Status.QUEUED || 
            download.status == Status.PAUSED) {
            
            notificationHelper.showProgressNotification(download.toDownloadInfo())
        }
        
        return true
    }
    
    // Helper to convert Fetch Download to our DownloadInfo
    // Duplicate of logic in DownloadManager, but needed here for independence
    private fun Download.toDownloadInfo(): DownloadInfo {
        val otaUpdateString = extras.getString("otaUpdate", "")
        val deviceName = extras.getString("deviceName", "")
        val regionName = extras.getString("regionName", "")

        return DownloadInfo(
            id = this.id,
            file = this.file,
            fileName = java.io.File(this.file).name,
            progress = this.progress,
            status = this.status,
            error = this.error,
            eta = this.etaInMilliSeconds,
            speed = this.downloadedBytesPerSecond, 
            totalBytes = this.total,
            original = this,
            otaUpdate = OtaUpdate.fromString(otaUpdateString),
            deviceName = deviceName,
            regionName = regionName
        )
    }
}
