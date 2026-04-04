package com.abhinav.otapulse.core.model

import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Status

data class DownloadInfo(
    val id: Int,
    val file: String,
    val fileName: String,
    val progress: Int,
    val status: Status,
    val error: Error,
    val eta: Long,
    val speed: Long,
    val totalBytes: Long,
    val original: Download,
    val otaUpdate: OtaUpdate?,
    val deviceName: String,
    val regionName: String
)

sealed class DownloadState {
    object Idle : DownloadState()
    data class Queued(val download: Download) : DownloadState()
    data class Downloading(val download: Download) : DownloadState()
    data class Paused(val download: Download) : DownloadState()
    data class Completed(val download: Download) : DownloadState()
    data class Failed(val download: Download, val error: Error) : DownloadState()
    data class Cancelled(val download: Download) : DownloadState()
}
