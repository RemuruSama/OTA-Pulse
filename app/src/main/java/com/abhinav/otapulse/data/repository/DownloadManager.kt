package com.abhinav.otapulse.data.repository

import android.content.Context
import android.os.Environment
import android.util.Log
import com.abhinav.otapulse.domain.model.DownloadInfo
import com.abhinav.otapulse.domain.model.DownloadState
import com.abhinav.otapulse.domain.model.OtaUpdate
import com.abhinav.otapulse.domain.model.toData
import com.abhinav.otapulse.domain.repository.DownloadRepository
import com.abhinav.otapulse.util.Component
import com.abhinav.otapulse.util.DownloadNotificationHelper
import com.abhinav.otapulse.util.OtaResolver
import com.google.gson.Gson
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchListener
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2.Priority
import com.tonyodev.fetch2.Request
import com.tonyodev.fetch2core.DownloadBlock
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.net.URLDecoder
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fetch: Fetch,
    private val notificationHelper: DownloadNotificationHelper
) : DownloadRepository, FetchListener {

    private val _allDownloads = MutableStateFlow<List<DownloadInfo>>(emptyList())
    override val allDownloads: StateFlow<List<DownloadInfo>> = _allDownloads.asStateFlow()

    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    override val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Thread-Safe Map for Rate Limiting to prevent SystemUI crashes
    private val lastProgressUpdateMap = ConcurrentHashMap<Int, Long>()
    private val PROGRESS_UPDATE_DELAY_MS = 800L // Only update notification every 800ms

    // FIX: Speed Smoothing Map
    // Stores the calculated "smoothed" speed for each download ID to prevent visual jumping
    private val speedSmoothingMap = ConcurrentHashMap<Int, Long>()

    init {
        fetch.addListener(this)
        updateDownloadsList()
    }

    override fun getTargetFile(otaUpdate: OtaUpdate, deviceName: String, regionName: String): File {
        val resolvedUrl = try {
            // Very simple pre-check or use existing URL. Real resolution happens in enqueue but for
            // filename generation we might need it.
            // CAUTION: synchronous network call is bad here if we used OtaResolver.resolveUrl().
            // Ideally we should move OtaResolver usage.
            // For now, let's use the original URL to extract filename if possible, or a standard naming scheme.
            // If we depend on 'resolvedUrl' for the filename (Content-Disposition), we can't do it synchronously easily.
            // However, Looking at existing code:
            otaUpdate.downloadUrl
        } catch (e: Exception) {
            otaUpdate.downloadUrl
        }

        val publicDownloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val targetDir = File(publicDownloadsDir, Component.OTA_UPDATES_DIR)

        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val isManualDownload = otaUpdate.versionName == "Unknown Version" ||
                deviceName == "Direct Download" ||
                deviceName == "Unknown Device"

        val finalFileName = if (isManualDownload) {
            // We can't easily iterate network here. Use best guess or device name.
            val extractedName = extractFileNameFromUrl(otaUpdate.downloadUrl)
             when {
                isValidFilename(extractedName) -> extractedName
                isValidFilename(otaUpdate.fileName) -> otaUpdate.fileName
                else -> "downloaded_file.zip"
            }
        } else {
            "${regionName}-${otaUpdate.versionName ?: "update"}.zip"
        }

        return File(targetDir, finalFileName)
    }

    override fun deleteFile(file: File): Boolean {
        return try {
            if (file.exists()) {
                val deleted = file.delete()
                if (deleted) {
                    Log.i("DownloadManager", "File deleted successfully: ${file.absolutePath}")
                } else {
                    Log.e("DownloadManager", "File.delete() returned false for: ${file.absolutePath}")
                }
                deleted
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("DownloadManager", "Failed to delete file: ${file.absolutePath}", e)
            false
        }
    }

    override fun enqueueDownload(otaUpdate: OtaUpdate, deviceName: String, regionName: String) {
        ioScope.launch {
            Log.d("DownloadManager", "Starting download process for ${otaUpdate.fileName}")

            val resolvedUrl = try {
                OtaResolver.resolveUrl(otaUpdate.downloadUrl)
            } catch (e: Exception) {
                Log.w("DownloadManager", "URL resolution failed, using original: ${e.message}")
                otaUpdate.downloadUrl
            }

            // use getTargetFile logic again? Or just call it?
            // reuse logic but carefully. getTargetFile uses 'otaUpdate.downloadUrl' not resolved one for name
            // if we want perfect match we should defer name generation until here?
            // Actually, for consistency, let's strictly use the same naming logic.
            // The previous code used 'resolvedUrl' to extract filename for Manual downloads.
            // If getTargetFile uses 'otaUpdate.downloadUrl', it might differ if redirection changes the filename.
            // BUT: standard OTA updates use "${regionName}-${otaUpdate.versionName}" which is deterministic.
            // Only "Manual Download" relies on URL.
            // Let's rely on getTargetFile logic being "good enough" for check, and re-run robust logic here?
            // NO, we must target the SAME file we checked against.

            // To ensure consistency, let's make getTargetFile the source of truth.
            // Challenge: valid filename from URL might require network (Content-Disposition) which we can't do in UI thread.
            // Compromise: For Manual Downloads, we might have to accept less accurate existence check or run it async.
            // For now, let's use the implementation that aligns with 'standard' updates (99% of use cases).

            val targetFile = getTargetFile(otaUpdate, deviceName, regionName)
            // Note: Manual download filename generation inside getTargetFile using original URL might differ
            // from logic using resolvedURL if the path changes.
            // However, OtaResolver mostly handles direct link extraction.
            // Let's stick to getTargetFile result.

            Log.i("DownloadManager", "Target File Path: ${targetFile.absolutePath}")

            val request = Request(resolvedUrl, targetFile.absolutePath).apply {
                priority = Priority.HIGH
                networkType = NetworkType.ALL

                // CRITICAL HEADERS for Resumability & Stability
                addHeader("Accept-Encoding", "identity")
                addHeader("User-Agent", "okhttp/3.12.12")
                addHeader("userId", "oplus-ota|16002018")
                addHeader("Accept", "*/*")
                addHeader("Connection", "Keep-Alive")
                addHeader("Cache-Control", "no-cache")

                val otaInfoForExtras = otaUpdate.copy(fileName = targetFile.name)
                val otaUpdateJson = Gson().toJson(otaInfoForExtras)

                extras = mapOf(
                    "otaUpdate" to otaUpdateJson,
                    "deviceName" to deviceName,
                    "regionName" to regionName,
                    "originalFileName" to targetFile.name
                ).toData()
            }

            fetch.enqueue(request, {
                Log.i("DownloadManager", "Enqueued: ${targetFile.absolutePath}")
            }, {
                Log.e("DownloadManager", "Error enqueueing: ${it.throwable?.message}")
            })
        }
    }

    private fun extractFileNameFromUrl(url: String): String {
        return try {
            val urlObj = java.net.URL(url)
            val path = urlObj.path
            val rawName = path.substringAfterLast('/')
            if (rawName.isBlank()) "" else URLDecoder.decode(rawName, "UTF-8")
        } catch (e: Exception) {
            url.substringAfterLast('/').takeIf { it.isNotBlank() }?.substringBefore('?') ?: ""
        }
    }

    private fun isValidFilename(name: String): Boolean {
        return name.isNotBlank() && !name.equals("downloadCheck", ignoreCase = true) && !name.equals("External-Unknown Version.zip", ignoreCase = true)
    }

    override fun pauseDownload(downloadInfo: DownloadInfo) {
        fetch.pause(downloadInfo.id)
        notificationHelper.cancelNotification(downloadInfo.id)
    }

    override fun resumeDownload(downloadInfo: DownloadInfo) {
        fetch.resume(downloadInfo.id)
    }

    override fun cancelDownload(downloadInfo: DownloadInfo) {
        fetch.cancel(downloadInfo.id)
        lastProgressUpdateMap.remove(downloadInfo.id)
        speedSmoothingMap.remove(downloadInfo.id)
    }

    override fun retryDownload(downloadInfo: DownloadInfo) {
        val error = downloadInfo.error

        // Manual trigger for URL Refresh on expired links
        if (error.httpResponse?.code == 403 ||
            error == Error.REQUEST_NOT_SUCCESSFUL ||
            error == Error.HTTP_NOT_FOUND ||
            error == Error.UNKNOWN) {

            Log.i("DownloadManager", "Retry triggering URL refresh for error: $error")
            ioScope.launch {
                try {
                    refreshDownload(downloadInfo.original)
                } catch (e: Exception) {
                    Log.e("DownloadManager", "URL refresh failed on retry. Falling back to standard retry.", e)
                    fetch.retry(downloadInfo.id)
                }
            }
        } else {
            fetch.retry(downloadInfo.id)
        }
    }

    override fun deleteDownload(downloadInfo: DownloadInfo) {
        fetch.delete(downloadInfo.id)
        lastProgressUpdateMap.remove(downloadInfo.id)
        speedSmoothingMap.remove(downloadInfo.id)
    }

    private fun updateDownloadsList() {
        fetch.getDownloads { downloads ->
            // Use smoothed speed if available, preventing UI from jumping
            _allDownloads.value = downloads.map { it.toDownloadInfo(smoothedSpeed = speedSmoothingMap[it.id]) }
        }
    }

    private fun updateSingleDownloadState(download: Download, newFilePath: String? = null, smoothedSpeed: Long? = null) {
        val currentDownloads = _allDownloads.value.toMutableList()
        val index = currentDownloads.indexOfFirst { it.id == download.id }

        // Prefer passed smoothed speed, then map value, then fallback to null (which uses raw)
        val displaySpeed = smoothedSpeed ?: speedSmoothingMap[download.id]

        val updatedInfo = download.toDownloadInfo(
            newFilePath = newFilePath ?: download.file,
            smoothedSpeed = displaySpeed
        )

        if (index != -1) {
            currentDownloads[index] = updatedInfo
        } else {
            currentDownloads.add(updatedInfo)
        }
        _allDownloads.value = currentDownloads.sortedByDescending { it.original.created }
    }

    // --- FetchListener Callbacks ---

    override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
        updateDownloadsList()
        notificationHelper.showProgressNotification(download.toDownloadInfo())
    }

    override fun onStarted(download: Download, downloadBlocks: List<DownloadBlock>, totalBlocks: Int) {
        // Reset smoothing
        speedSmoothingMap[download.id] = 0L
        updateSingleDownloadState(download)
        notificationHelper.showProgressNotification(download.toDownloadInfo())
        lastProgressUpdateMap[download.id] = System.currentTimeMillis()
    }

    override fun onProgress(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long) {
        // SPEED SMOOTHING ALGORITHM
        // Applies a low-pass filter (Exponential Moving Average)
        // 80% previous speed + 20% current instantaneous speed
        // This removes the "double speed" spikes and aligns with the system status bar.
        val oldSpeed = speedSmoothingMap[download.id] ?: downloadedBytesPerSecond
        val smoothedSpeed = ((oldSpeed * 0.8) + (downloadedBytesPerSecond * 0.2)).toLong()

        speedSmoothingMap[download.id] = smoothedSpeed

        updateSingleDownloadState(download, smoothedSpeed = smoothedSpeed)

        val now = System.currentTimeMillis()
        val lastUpdate = lastProgressUpdateMap[download.id] ?: 0L

        if (now - lastUpdate >= PROGRESS_UPDATE_DELAY_MS) {
            notificationHelper.showProgressNotification(download.toDownloadInfo(smoothedSpeed = smoothedSpeed))
            lastProgressUpdateMap[download.id] = now
        }
    }

    override fun onCompleted(download: Download) {
        ioScope.launch {
            lastProgressUpdateMap.remove(download.id)
            speedSmoothingMap.remove(download.id)

            val completedDownloadInfo = download.toDownloadInfo(newFilePath = download.file)
            updateSingleDownloadState(download, newFilePath = download.file)
            notificationHelper.showCompletedNotification(completedDownloadInfo)
            Log.i("DownloadManager", "Download completed: ${download.file}")
        }
    }

    override fun onError(download: Download, error: Error, throwable: Throwable?) {
        lastProgressUpdateMap.remove(download.id)
        speedSmoothingMap.remove(download.id)

        handleDefaultError(download, error, throwable)
    }

    private fun handleDefaultError(download: Download, error: Error, throwable: Throwable?) {
        updateSingleDownloadState(download)
        notificationHelper.showErrorNotification(download.toDownloadInfo())
        Log.e("DownloadManager", "Download error for ${download.file}: $error - ${throwable?.message}")
    }

    private suspend fun refreshDownload(oldDownload: Download) {
        val otaUpdate = oldDownload.toDownloadInfo().otaUpdate
        if (otaUpdate == null) {
            Log.e("DownloadManager", "Cannot refresh: OtaUpdate info not found in download extras.")
            handleDefaultError(oldDownload, oldDownload.error, null)
            return
        }

        Log.d("DownloadManager", "Refreshing URL for: ${otaUpdate.downloadUrl}")
        val newResolvedUrl = OtaResolver.resolveUrl(otaUpdate.downloadUrl)
        if (newResolvedUrl == oldDownload.url) {
            Log.w("DownloadManager", "URL refresh resulted in the same URL. Aborting refresh.")
            handleDefaultError(oldDownload, oldDownload.error, null)
            return
        }
        Log.i("DownloadManager", "Refreshed URL successfully: $newResolvedUrl")

        val newRequest = Request(newResolvedUrl, oldDownload.file).apply {
            priority = oldDownload.priority
            networkType = oldDownload.networkType

            // Re-apply critical headers
            addHeader("Accept-Encoding", "identity")
            addHeader("User-Agent", "okhttp/3.12.12")
            addHeader("userId", "oplus-ota|16002018")
            addHeader("Accept", "*/*")
            addHeader("Connection", "Keep-Alive")
            addHeader("Cache-Control", "no-cache")

            extras = oldDownload.extras
        }

        fetch.updateRequest(oldDownload.id, newRequest, true, { download ->
            Log.i("DownloadManager", "Successfully updated download request for ${download.file}. It should now resume.")
            fetch.retry(oldDownload.id)
        }, { error ->
            Log.e("DownloadManager", "Failed to update request: $error")
            handleDefaultError(oldDownload, error, null)
        })
    }

    override fun onCancelled(download: Download) {
        updateSingleDownloadState(download)
        notificationHelper.cancelNotification(download.id)
        lastProgressUpdateMap.remove(download.id)
        speedSmoothingMap.remove(download.id)
    }

    override fun onRemoved(download: Download) {
        updateDownloadsList()
        notificationHelper.cancelNotification(download.id)
        lastProgressUpdateMap.remove(download.id)
        speedSmoothingMap.remove(download.id)
    }

    override fun onDeleted(download: Download) {
        updateDownloadsList()
        notificationHelper.cancelNotification(download.id)
        lastProgressUpdateMap.remove(download.id)
        speedSmoothingMap.remove(download.id)
    }

    override fun onAdded(download: Download) = updateDownloadsList()
    override fun onPaused(download: Download) = updateSingleDownloadState(download)
    override fun onResumed(download: Download) = updateSingleDownloadState(download)
    override fun onWaitingNetwork(download: Download) = updateSingleDownloadState(download)
    override fun onDownloadBlockUpdated(download: Download, downloadBlock: DownloadBlock, totalBlocks: Int) { /* No-op */ }

    private fun Download.toDownloadInfo(newFilePath: String? = null, smoothedSpeed: Long? = null): DownloadInfo {
        val otaUpdateString = extras.getString("otaUpdate", "")
        val deviceName = extras.getString("deviceName", "")
        val regionName = extras.getString("regionName", "")

        val currentActualFilePath = newFilePath ?: this.file

        // Use smoothed speed if available (prevents spikes), otherwise raw speed
        val effectiveSpeed = smoothedSpeed ?: this.downloadedBytesPerSecond

        return DownloadInfo(
            id = this.id,
            file = currentActualFilePath,
            fileName = File(currentActualFilePath).name,
            progress = this.progress,
            status = this.status,
            error = this.error,
            eta = this.etaInMilliSeconds,
            speed = effectiveSpeed, // Pass smoothed speed to UI
            totalBytes = this.total,
            original = this,
            otaUpdate = OtaUpdate.fromString(otaUpdateString),
            deviceName = deviceName,
            regionName = regionName
        )
    }
}