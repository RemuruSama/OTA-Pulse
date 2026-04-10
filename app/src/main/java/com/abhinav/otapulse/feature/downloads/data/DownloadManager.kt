package com.abhinav.otapulse.feature.downloads.data

import android.content.Context
import android.os.Environment
import android.util.Log
import com.abhinav.otapulse.core.model.DownloadInfo
import com.abhinav.otapulse.core.model.DownloadState
import com.abhinav.otapulse.core.model.OtaUpdate
import com.abhinav.otapulse.core.model.toData
import com.abhinav.otapulse.core.model.toDownloadInfo
import com.abhinav.otapulse.feature.downloads.domain.DownloadRepository
import com.abhinav.otapulse.core.network.Component
import com.abhinav.otapulse.core.notifications.DownloadNotificationHelper
import com.abhinav.otapulse.core.network.OtaResolver
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
import java.util.concurrent.atomic.AtomicInteger
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

    // Track auto-refresh attempts per download to prevent infinite loops
    private val autoRefreshAttempts = ConcurrentHashMap<Int, AtomicInteger>()
    private val MAX_AUTO_REFRESH_ATTEMPTS = 3

    init {
        fetch.addListener(this)
        updateDownloadsList()
    }

    override fun getTargetFile(otaUpdate: OtaUpdate, deviceName: String, regionName: String): File {
        return resolveTargetFile(otaUpdate, deviceName, regionName, resolvedInfo = null)
    }

    override suspend fun getResolvedTargetFile(
        otaUpdate: OtaUpdate,
        deviceName: String,
        regionName: String
    ): File {
        val resolvedInfo = try {
            OtaResolver.resolveUrl(otaUpdate.downloadUrl)
        } catch (e: Exception) {
            Log.w(TAG, "URL resolution failed during target lookup, using original: ${e.message}")
            OtaResolver.ResolvedUrlInfo(otaUpdate.downloadUrl, null)
        }
        return resolveTargetFile(otaUpdate, deviceName, regionName, resolvedInfo)
    }

    private fun resolveTargetFile(
        otaUpdate: OtaUpdate,
        deviceName: String,
        regionName: String,
        resolvedInfo: OtaResolver.ResolvedUrlInfo?
    ): File {
        val baseDir = File(
            Environment.getExternalStorageDirectory(),
            Component.OTA_UPDATES_DIR
        )
        
        // Structure: OTAPulseDownloader/Firmware/<region>-<versionName>/
        val firmwareDir = File(baseDir, "Firmware")
        val versionName = sanitizeFolderSegment(otaUpdate.versionName ?: "Unknown")
        val regionFolderPrefix = sanitizeFolderSegment(regionName).takeIf { regionName.isNotBlank() }.orEmpty()
        val folderName = if (regionFolderPrefix.isBlank()) versionName else "$regionFolderPrefix-$versionName"
        val targetDir = File(firmwareDir, folderName).also {
            if (!it.exists()) it.mkdirs() 
        }

        // Extract filename from the OTA URL (query params stripped by URL.path)
        val raw = otaUpdate.fileName.takeIf { isValidFilename(it) }
            ?: extractFileNameFromUrl(otaUpdate.downloadUrl).takeIf { isValidFilename(it) }
            ?: "ota_${System.currentTimeMillis()}.zip"

        // Safety: ext4 filename limit is 255 bytes — truncate while preserving extension
        val finalFileName = if (raw.length > 200) {
            val ext = if ('.' in raw) ".${raw.substringAfterLast('.')}" else ""
            raw.substringBeforeLast('.').take(200 - ext.length) + ext
        } else raw

        val initialTargetFile = File(targetDir, finalFileName)
        val resolvedUrl = resolvedInfo?.url.orEmpty()

        return when {
            isValidFilename(resolvedFileNameFromUrl(resolvedUrl)) -> {
                File(initialTargetFile.parentFile, resolvedFileNameFromUrl(resolvedUrl))
            }
            isValidFilename(resolvedInfo?.contentDispositionFileName ?: "") -> {
                File(initialTargetFile.parentFile, resolvedInfo?.contentDispositionFileName!!)
            }
            else -> initialTargetFile
        }
    }


    override fun deleteFile(file: File): Boolean {
        return try {
            if (file.exists()) {
                val deleted = file.delete()
                if (deleted) {
                    Log.i(TAG, "File deleted successfully: ${file.absolutePath}")
                    cleanupEmptyParentDirectories(file)
                } else {
                    Log.e(TAG, "File.delete() returned false for: ${file.absolutePath}")
                }
                deleted
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete file: ${file.absolutePath}", e)
            false
        }
    }

    override fun enqueueDownload(otaUpdate: OtaUpdate, deviceName: String, regionName: String) {
        ioScope.launch {
            Log.d(TAG, "Starting download process for ${otaUpdate.fileName}")

            val resolvedInfo = try {
                OtaResolver.resolveUrl(otaUpdate.downloadUrl)
            } catch (e: Exception) {
                Log.w(TAG, "URL resolution failed, using original: ${e.message}")
                OtaResolver.ResolvedUrlInfo(otaUpdate.downloadUrl, null)
            }
            val finalTargetFile = resolveTargetFile(otaUpdate, deviceName, regionName, resolvedInfo)
            val resolvedUrl = resolvedInfo.url

            Log.i(TAG, "Target File Path: ${finalTargetFile.absolutePath}")


            val request = Request(resolvedUrl, finalTargetFile.absolutePath).apply {
                priority = Priority.HIGH
                networkType = NetworkType.ALL

                addHeader("Accept-Encoding", "identity")
                addHeader("User-Agent", "okhttp/3.12.12")
                addHeader("userId", "oplus-ota|16002018")
                addHeader("Accept", "*/*")
                addHeader("Connection", "Keep-Alive")
                addHeader("Cache-Control", "no-cache")

                val otaInfoForExtras = otaUpdate.copy(fileName = finalTargetFile.name)
                val otaUpdateJson = Gson().toJson(otaInfoForExtras)

                extras = mapOf(
                    "otaUpdate" to otaUpdateJson,
                    "deviceName" to deviceName,
                    "regionName" to regionName,
                    "originalFileName" to finalTargetFile.name
                ).toData()
            }

            fetch.enqueue(request, {
                Log.i(TAG, "Enqueued: ${finalTargetFile.absolutePath}")
            }, {
                Log.e(TAG, "Error enqueueing: ${it.throwable?.message}")
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

    private fun resolvedFileNameFromUrl(url: String): String {
        val fromResolved = try {
            java.net.URL(url).path.substringAfterLast('/').let {
                URLDecoder.decode(it, "UTF-8")
            }
        } catch (_: Exception) { "" }

        return if (fromResolved.length > 200) {
            val ext = if ('.' in fromResolved) ".${fromResolved.substringAfterLast('.')}" else ""
            fromResolved.substringBeforeLast('.').take(200 - ext.length) + ext
        } else fromResolved
    }

    private fun isValidFilename(name: String): Boolean {
        return name.isNotBlank() &&
               !name.equals(INVALID_FILENAME_DOWNLOAD_CHECK, ignoreCase = true) &&
               !name.equals(INVALID_FILENAME_UNKNOWN_VERSION, ignoreCase = true) &&
               !name.equals(INVALID_FILENAME_DOWNLOADED_FILE, ignoreCase = true)
    }

    private fun cleanupEmptyParentDirectories(file: File) {
        val baseDir = File(
            Environment.getExternalStorageDirectory(),
            Component.OTA_UPDATES_DIR
        ).absoluteFile

        var current = file.parentFile?.absoluteFile
        while (current != null && current != baseDir) {
            val relativePath = runCatching { current.relativeTo(baseDir).path }.getOrNull() ?: break
            if (relativePath.isBlank()) break

            val children = current.listFiles()
            if (children != null && children.isEmpty()) {
                if (current.delete()) {
                    Log.i(TAG, "Removed empty directory: ${current.absolutePath}")
                    current = current.parentFile?.absoluteFile
                } else {
                    Log.w(TAG, "Failed to remove empty directory: ${current.absolutePath}")
                    break
                }
            } else {
                break
            }
        }
    }

    private fun sanitizeFolderSegment(value: String): String {
        return value
            .trim()
            .replace(Regex("[\\\\/:*?\"<>|]+"), "_")
            .replace(Regex("\\s+"), " ")
            .trim('.')
            .ifBlank { "Unknown" }
    }

    override fun pauseDownload(downloadInfo: DownloadInfo) {
        fetch.pause(downloadInfo.id)
        notificationHelper.cancelNotification(downloadInfo.id)
    }

    override fun resumeDownload(downloadInfo: DownloadInfo) {
        // Reset the auto-refresh counter so the user gets a fresh 3 attempts after manually resuming
        autoRefreshAttempts.remove(downloadInfo.id)

        // The CustomHttpUrlConnectionDownloader handles expired signed URLs on the fly transparently.
        // We do NOT use fetch.updateRequest() here because Fetch2 natively wipes local files 
        // if the requested URL String visibly changes. 
        fetch.resume(downloadInfo.id)
    }

    override fun cancelDownload(downloadInfo: DownloadInfo) {
        fetch.cancel(downloadInfo.id)
        lastProgressUpdateMap.remove(downloadInfo.id)
        speedSmoothingMap.remove(downloadInfo.id)
        autoRefreshAttempts.remove(downloadInfo.id)
    }

    override fun retryDownload(downloadInfo: DownloadInfo) {
        // Reset auto-refresh counter on manual retry
        autoRefreshAttempts.remove(downloadInfo.id)
        fetch.retry(downloadInfo.id)
    }

    override fun deleteDownload(downloadInfo: DownloadInfo) {
        val targetFile = File(downloadInfo.file)
        fetch.delete(downloadInfo.id)
        if (targetFile.exists()) {
            val deleted = targetFile.delete()
            if (!deleted && targetFile.exists()) {
                Log.w(TAG, "Unable to delete file during download cleanup: ${targetFile.absolutePath}")
            }
        }
        if (!targetFile.exists()) {
            cleanupEmptyParentDirectories(targetFile)
        }
        lastProgressUpdateMap.remove(downloadInfo.id)
        speedSmoothingMap.remove(downloadInfo.id)
        autoRefreshAttempts.remove(downloadInfo.id)
    }

    /** Checks if the error indicates an expired, invalid, or range-rejected download URL */
    private fun isExpiredUrlError(error: Error): Boolean {
        val code = error.httpResponse?.code
        return code == 403 ||
               code == 410 ||
               code == 416 || // Range Not Satisfiable — stale signed URL rejected the Range header
               error == Error.REQUEST_NOT_SUCCESSFUL ||
               error == Error.HTTP_NOT_FOUND ||
               error == Error.UNKNOWN
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
        // Reset smoothing and auto-refresh counter on successful start
        speedSmoothingMap[download.id] = 0L
        autoRefreshAttempts.remove(download.id)
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
            Log.i(TAG, "Download completed: ${download.file}")
        }
    }

    override fun onError(download: Download, error: Error, throwable: Throwable?) {
        lastProgressUpdateMap.remove(download.id)
        speedSmoothingMap.remove(download.id)
        autoRefreshAttempts.remove(download.id)

        handleDefaultError(download, error, throwable)
    }

    private fun handleDefaultError(download: Download, error: Error, throwable: Throwable?) {
        updateSingleDownloadState(download)
        notificationHelper.showErrorNotification(download.toDownloadInfo())
        Log.e(TAG, "Download error for ${download.file}: $error - ${throwable?.message}")
    }



    override fun onCancelled(download: Download) {
        updateSingleDownloadState(download)
        notificationHelper.cancelNotification(download.id)
        lastProgressUpdateMap.remove(download.id)
        speedSmoothingMap.remove(download.id)
        autoRefreshAttempts.remove(download.id)
    }

    override fun onRemoved(download: Download) {
        updateDownloadsList()
        notificationHelper.cancelNotification(download.id)
        lastProgressUpdateMap.remove(download.id)
        speedSmoothingMap.remove(download.id)
        autoRefreshAttempts.remove(download.id)
    }

    override fun onDeleted(download: Download) {
        updateDownloadsList()
        notificationHelper.cancelNotification(download.id)
        lastProgressUpdateMap.remove(download.id)
        speedSmoothingMap.remove(download.id)
        autoRefreshAttempts.remove(download.id)
    }

    override fun onAdded(download: Download) = updateDownloadsList()
    override fun onPaused(download: Download) = updateSingleDownloadState(download)
    override fun onResumed(download: Download) = updateSingleDownloadState(download)
    override fun onWaitingNetwork(download: Download) = updateSingleDownloadState(download)
    override fun onDownloadBlockUpdated(download: Download, downloadBlock: DownloadBlock, totalBlocks: Int) { /* No-op */ }

    companion object {
        private const val TAG = "DownloadManager"

        // Sentinel filenames that indicate an invalid or placeholder download name
        private const val INVALID_FILENAME_DOWNLOAD_CHECK = "downloadCheck"
        private const val INVALID_FILENAME_UNKNOWN_VERSION = "External-Unknown Version.zip"
        private const val INVALID_FILENAME_DOWNLOADED_FILE = "downloaded_file.zip"
    }
}
