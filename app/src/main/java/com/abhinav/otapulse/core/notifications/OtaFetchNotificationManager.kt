package com.abhinav.otapulse.core.notifications

import android.content.Context
import com.abhinav.otapulse.core.model.DownloadInfo
import com.abhinav.otapulse.core.model.OtaUpdate
import com.abhinav.otapulse.core.model.toDownloadInfo
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.DefaultFetchNotificationManager
import com.tonyodev.fetch2.Status
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
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

    // Rate-limit to prevent SystemUI crashes from too-frequent notification updates
    private val lastUpdateMap = ConcurrentHashMap<Int, Long>()
    private val THROTTLE_MS = 1000L

    override fun postDownloadUpdate(
        download: Download
    ): Boolean {
        if (download.status == Status.DOWNLOADING || 
            download.status == Status.QUEUED || 
            download.status == Status.PAUSED) {

            val now = System.currentTimeMillis()
            val lastUpdate = lastUpdateMap[download.id] ?: 0L

            // Throttle: only update notification at most once per second
            if (now - lastUpdate >= THROTTLE_MS) {
                lastUpdateMap[download.id] = now
                notificationHelper.showProgressNotification(download.toDownloadInfo())
            }
        } else {
            // Clean up rate-limit tracking for completed/errored downloads
            lastUpdateMap.remove(download.id)
        }
        
        return true
    }
    
}
