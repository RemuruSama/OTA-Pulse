package com.abhinav.otapulse.feature.downloads.data

import android.content.Context
import android.os.Environment
import android.util.Log
import com.abhinav.otapulse.core.download.DownloadError
import com.abhinav.otapulse.core.download.DownloadListener
import com.abhinav.otapulse.core.download.DownloadRecord
import com.abhinav.otapulse.core.download.DownloadStatus
import com.abhinav.otapulse.core.download.OkHttpDownloadEngine
import com.abhinav.otapulse.core.model.DownloadInfo
import com.abhinav.otapulse.core.model.DownloadState
import com.abhinav.otapulse.core.model.OtaUpdate
import com.abhinav.otapulse.core.model.toDownloadInfo
import com.abhinav.otapulse.core.model.toExtras
import com.abhinav.otapulse.feature.downloads.domain.DownloadRepository
import com.abhinav.otapulse.core.network.Component
import com.abhinav.otapulse.core.notifications.DownloadNotificationHelper
import com.abhinav.otapulse.core.network.OtaResolver
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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
    private val engine: OkHttpDownloadEngine,
    private val notificationHelper: DownloadNotificationHelper
) : DownloadRepository, DownloadListener {

    private val _allDownloads = MutableStateFlow<List<DownloadInfo>>(emptyList())
    override val allDownloads: StateFlow<List<DownloadInfo>> = _allDownloads.asStateFlow()

    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    override val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Thread-Safe Map for Rate Limiting to prevent SystemUI crashes
    private val lastProgressUpdateMap = ConcurrentHashMap<Int, Long>()
    private val PROGRESS_UPDATE_DELAY_MS = 800L // Only update notification every 800ms

    // Speed Smoothing Map
    // Stores the calculated "smoothed" speed for each download ID to prevent visual jumping
    private val speedSmoothingMap = ConcurrentHashMap<Int, Long>()

    // Track automatic retry attempts per download to prevent infinite loops.
    private val autoRetryAttempts = ConcurrentHashMap<Int, AtomicInteger>()
    private val MAX_AUTO_RETRY_ATTEMPTS = 3
    private val AUTO_RETRY_BASE_DELAY_MS = 1_500L

    init {
        engine.addListener(this)
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
        val resolvedInfo = if (isDownloadCheckUrl(otaUpdate.downloadUrl)) {
            try {
                OtaResolver.resolveUrl(otaUpdate.downloadUrl)
            } catch (e: Exception) {
                Log.w(TAG, "URL resolution failed during target lookup, using original: ${e.message}")
                OtaResolver.ResolvedUrlInfo(otaUpdate.downloadUrl, null)
            }
        } else {
            // Direct CDN URL — no resolution needed, use as-is
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

            // Only resolve downloadCheck URLs — direct CDN URLs need no resolution.
            val resolvedInfo = if (isDownloadCheckUrl(otaUpdate.downloadUrl)) {
                try {
                    OtaResolver.resolveUrl(otaUpdate.downloadUrl)
                } catch (e: Exception) {
                    Log.w(TAG, "URL resolution failed, using original: ${e.message}")
                    OtaResolver.ResolvedUrlInfo(otaUpdate.downloadUrl, null)
                }
            } else {
                OtaResolver.ResolvedUrlInfo(otaUpdate.downloadUrl, null)
            }
            val finalTargetFile = resolveTargetFile(otaUpdate, deviceName, regionName, resolvedInfo)
            val resolvedUrl = resolvedInfo.url

            Log.i(TAG, "Target File Path: ${finalTargetFile.absolutePath}")

            val otaInfoForExtras = otaUpdate.copy(fileName = finalTargetFile.name)
            val otaUpdateJson = Gson().toJson(otaInfoForExtras)

            val extras = mapOf(
                "otaUpdate" to otaUpdateJson,
                "deviceName" to deviceName,
                "regionName" to regionName,
                "originalFileName" to finalTargetFile.name
            ).toExtras()

            engine.enqueue(resolvedUrl, finalTargetFile.absolutePath, extras)
            Log.i(TAG, "Enqueued: ${finalTargetFile.absolutePath}")
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
        engine.pause(downloadInfo.id)
        notificationHelper.cancelNotification(downloadInfo.id)
    }

    override fun resumeDownload(downloadInfo: DownloadInfo) {
        // Reset the auto-retry counter so the user gets a fresh retry window after manually resuming.
        autoRetryAttempts.remove(downloadInfo.id)
        engine.resume(downloadInfo.id)
    }

    override fun cancelDownload(downloadInfo: DownloadInfo) {
        engine.cancel(downloadInfo.id)
        lastProgressUpdateMap.remove(downloadInfo.id)
        speedSmoothingMap.remove(downloadInfo.id)
        autoRetryAttempts.remove(downloadInfo.id)
    }

    override fun retryDownload(downloadInfo: DownloadInfo) {
        // Reset auto-retry counter on manual retry.
        autoRetryAttempts.remove(downloadInfo.id)
        engine.retry(downloadInfo.id)
    }

    override fun deleteDownload(downloadInfo: DownloadInfo) {
        val targetFile = File(downloadInfo.file)
        engine.delete(downloadInfo.id)
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
        autoRetryAttempts.remove(downloadInfo.id)
    }

    /** Checks if the error indicates an expired, invalid, or range-rejected download URL */
    private fun isExpiredUrlError(error: DownloadError): Boolean {
        return error == DownloadError.REQUEST_NOT_SUCCESSFUL ||
               error == DownloadError.HTTP_NOT_FOUND ||
               error == DownloadError.UNKNOWN
    }

    private fun shouldAutoRetry(error: DownloadError): Boolean {
        return isExpiredUrlError(error) ||
            error == DownloadError.NO_NETWORK_CONNECTION ||
            error == DownloadError.CONNECTION_TIMED_OUT ||
            error == DownloadError.UNKNOWN_IO_ERROR ||
            error == DownloadError.UNKNOWN
    }

    private fun scheduleAutoRetry(record: DownloadRecord, error: DownloadError): Boolean {
        if (!shouldAutoRetry(error)) return false

        val attempts = autoRetryAttempts.getOrPut(record.id) { AtomicInteger(0) }
        val nextAttempt = attempts.incrementAndGet()
        if (nextAttempt > MAX_AUTO_RETRY_ATTEMPTS) {
            attempts.set(MAX_AUTO_RETRY_ATTEMPTS)
            return false
        }

        val retryDelayMs = AUTO_RETRY_BASE_DELAY_MS * nextAttempt
        Log.w(
            TAG,
            "Auto retry scheduled for download ${record.id}: " +
                "attempt $nextAttempt/$MAX_AUTO_RETRY_ATTEMPTS in ${retryDelayMs}ms after $error"
        )

        updateSingleDownloadState(record)
        notificationHelper.cancelNotification(record.id)

        ioScope.launch {
            delay(retryDelayMs)
            engine.retry(record.id)
        }
        return true
    }

    private fun updateDownloadsList() {
        val downloads = engine.getDownloads()
        _allDownloads.value = downloads.map { it.toDownloadInfo(smoothedSpeed = speedSmoothingMap[it.id]) }
    }

    private fun updateSingleDownloadState(record: DownloadRecord, newFilePath: String? = null, smoothedSpeed: Long? = null) {
        val currentDownloads = _allDownloads.value.toMutableList()
        val index = currentDownloads.indexOfFirst { it.id == record.id }

        // Prefer passed smoothed speed, then map value, then fallback to null (which uses raw)
        val displaySpeed = smoothedSpeed ?: speedSmoothingMap[record.id]

        val updatedInfo = record.toDownloadInfo(
            newFilePath = newFilePath ?: record.file,
            smoothedSpeed = displaySpeed
        )

        if (index != -1) {
            currentDownloads[index] = updatedInfo
        } else {
            currentDownloads.add(updatedInfo)
        }
        _allDownloads.value = currentDownloads.sortedByDescending { it.original.created }
    }

    // --- DownloadListener Callbacks ---

    override fun onQueued(record: DownloadRecord, waitingOnNetwork: Boolean) {
        updateDownloadsList()
        notificationHelper.showProgressNotification(record.toDownloadInfo())
    }

    override fun onStarted(record: DownloadRecord) {
        // Reset smoothing and auto-retry counter on successful start.
        speedSmoothingMap[record.id] = 0L
        autoRetryAttempts.remove(record.id)
        updateSingleDownloadState(record)
        notificationHelper.showProgressNotification(record.toDownloadInfo())
        lastProgressUpdateMap[record.id] = System.currentTimeMillis()
    }

    override fun onProgress(record: DownloadRecord, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long) {
        // SPEED SMOOTHING ALGORITHM
        // Applies a low-pass filter (Exponential Moving Average)
        // 80% previous speed + 20% current instantaneous speed
        val oldSpeed = speedSmoothingMap[record.id] ?: downloadedBytesPerSecond
        val smoothedSpeed = ((oldSpeed * 0.8) + (downloadedBytesPerSecond * 0.2)).toLong()

        speedSmoothingMap[record.id] = smoothedSpeed

        updateSingleDownloadState(record, smoothedSpeed = smoothedSpeed)

        val now = System.currentTimeMillis()
        val lastUpdate = lastProgressUpdateMap[record.id] ?: 0L

        if (now - lastUpdate >= PROGRESS_UPDATE_DELAY_MS) {
            notificationHelper.showProgressNotification(record.toDownloadInfo(smoothedSpeed = smoothedSpeed))
            lastProgressUpdateMap[record.id] = now
        }
    }

    override fun onCompleted(record: DownloadRecord) {
        ioScope.launch {
            lastProgressUpdateMap.remove(record.id)
            speedSmoothingMap.remove(record.id)
            autoRetryAttempts.remove(record.id)

            val completedDownloadInfo = record.toDownloadInfo(newFilePath = record.file)
            updateSingleDownloadState(record, newFilePath = record.file)
            notificationHelper.showCompletedNotification(completedDownloadInfo)
            Log.i(TAG, "Download completed: ${record.file}")
        }
    }

    override fun onError(record: DownloadRecord, error: DownloadError, throwable: Throwable?) {
        lastProgressUpdateMap.remove(record.id)
        speedSmoothingMap.remove(record.id)

        if (scheduleAutoRetry(record, error)) {
            return
        }

        autoRetryAttempts.remove(record.id)
        handleDefaultError(record, error, throwable)
    }

    private fun handleDefaultError(record: DownloadRecord, error: DownloadError, throwable: Throwable?) {
        updateSingleDownloadState(record)
        notificationHelper.showErrorNotification(record.toDownloadInfo())
        Log.e(TAG, "Download error for ${record.file}: $error - ${throwable?.message}")
    }

    override fun onCancelled(record: DownloadRecord) {
        updateSingleDownloadState(record)
        notificationHelper.cancelNotification(record.id)
        lastProgressUpdateMap.remove(record.id)
        speedSmoothingMap.remove(record.id)
        autoRetryAttempts.remove(record.id)
    }

    override fun onRemoved(record: DownloadRecord) {
        updateDownloadsList()
        notificationHelper.cancelNotification(record.id)
        lastProgressUpdateMap.remove(record.id)
        speedSmoothingMap.remove(record.id)
        autoRetryAttempts.remove(record.id)
    }

    override fun onDeleted(record: DownloadRecord) {
        updateDownloadsList()
        notificationHelper.cancelNotification(record.id)
        lastProgressUpdateMap.remove(record.id)
        speedSmoothingMap.remove(record.id)
        autoRetryAttempts.remove(record.id)
    }

    override fun onAdded(record: DownloadRecord) = updateDownloadsList()
    
    override fun onPaused(record: DownloadRecord) {
        updateSingleDownloadState(record)
        notificationHelper.showProgressNotification(record.toDownloadInfo(smoothedSpeed = speedSmoothingMap[record.id]))
    }

    override fun onResumed(record: DownloadRecord) {
        updateSingleDownloadState(record)
        notificationHelper.showProgressNotification(record.toDownloadInfo(smoothedSpeed = speedSmoothingMap[record.id]))
    }

    companion object {
        private const val TAG = "DownloadManager"

        // Sentinel filenames that indicate an invalid or placeholder download name
        private const val INVALID_FILENAME_DOWNLOAD_CHECK = "downloadCheck"
        private const val INVALID_FILENAME_UNKNOWN_VERSION = "External-Unknown Version.zip"
        private const val INVALID_FILENAME_DOWNLOADED_FILE = "downloaded_file.zip"

        /**
         * Returns true for `downloadCheck` API URLs that OtaResolver can follow to get a
         * fresh direct CDN URL. Direct CDN URLs (e.g. allawnfs.com signed URLs) return false.
         */
        fun isDownloadCheckUrl(url: String): Boolean =
            url.contains("/downloadCheck", ignoreCase = true)
    }
}
