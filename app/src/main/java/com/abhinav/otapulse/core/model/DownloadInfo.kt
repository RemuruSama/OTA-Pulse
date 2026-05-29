package com.abhinav.otapulse.core.model

import com.abhinav.otapulse.core.download.DownloadError
import com.abhinav.otapulse.core.download.DownloadRecord
import com.abhinav.otapulse.core.download.DownloadStatus

data class DownloadInfo(
    val id: Int,
    val file: String,
    val fileName: String,
    val downloadedBytes: Long,
    val progress: Int,
    val status: DownloadStatus,
    val error: DownloadError,
    val eta: Long,
    val speed: Long,
    val totalBytes: Long,
    val original: DownloadRecord,
    val otaUpdate: OtaUpdate?,
    val deviceName: String,
    val regionName: String,
    val md5Status: Md5Status = Md5Status.NONE
)

/**
 * Represents the state of post-download MD5 integrity verification.
 */
enum class Md5Status {
    /** Not yet checked (downloading, queued, etc.) */
    NONE,
    /** Hash computation is in progress */
    VERIFYING,
    /** Hash matches the expected MD5 from OTA metadata */
    VERIFIED,
    /** Hash does not match — file may be corrupted */
    FAILED,
    /** No MD5 hash available in OTA metadata (e.g. direct downloads) */
    SKIPPED,
    /** File not found or IO error during verification */
    ERROR
}

sealed class DownloadState {
    object Idle : DownloadState()
    data class Queued(val download: DownloadRecord) : DownloadState()
    data class Downloading(val download: DownloadRecord) : DownloadState()
    data class Paused(val download: DownloadRecord) : DownloadState()
    data class Completed(val download: DownloadRecord) : DownloadState()
    data class Failed(val download: DownloadRecord, val error: DownloadError) : DownloadState()
    data class Cancelled(val download: DownloadRecord) : DownloadState()
}
