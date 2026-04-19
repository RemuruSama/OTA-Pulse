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
    val regionName: String
)

sealed class DownloadState {
    object Idle : DownloadState()
    data class Queued(val download: DownloadRecord) : DownloadState()
    data class Downloading(val download: DownloadRecord) : DownloadState()
    data class Paused(val download: DownloadRecord) : DownloadState()
    data class Completed(val download: DownloadRecord) : DownloadState()
    data class Failed(val download: DownloadRecord, val error: DownloadError) : DownloadState()
    data class Cancelled(val download: DownloadRecord) : DownloadState()
}
