package com.abhinav.otapulse.feature.downloads.domain

import com.abhinav.otapulse.core.model.DownloadInfo
import com.abhinav.otapulse.core.model.DownloadState
import com.abhinav.otapulse.core.model.OtaUpdate
import kotlinx.coroutines.flow.StateFlow

interface DownloadRepository {
    val allDownloads: StateFlow<List<DownloadInfo>>
    val downloadStates: StateFlow<Map<String, DownloadState>>
    fun enqueueDownload(otaUpdate: OtaUpdate, deviceName: String, regionName: String, isFromHomeUpdate: Boolean = false)
    fun pauseDownload(downloadInfo: DownloadInfo)
    fun resumeDownload(downloadInfo: DownloadInfo)
    fun cancelDownload(downloadInfo: DownloadInfo)
    fun retryDownload(downloadInfo: DownloadInfo)
    fun deleteDownload(downloadInfo: DownloadInfo)
    fun getTargetFile(otaUpdate: OtaUpdate, deviceName: String, regionName: String): java.io.File
    suspend fun getResolvedTargetFile(otaUpdate: OtaUpdate, deviceName: String, regionName: String): java.io.File
    fun deleteFile(file: java.io.File): Boolean
}
