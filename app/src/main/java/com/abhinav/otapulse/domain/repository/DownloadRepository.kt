package com.abhinav.otapulse.domain.repository

import com.abhinav.otapulse.domain.model.DownloadInfo
import com.abhinav.otapulse.domain.model.DownloadState
import com.abhinav.otapulse.domain.model.OtaUpdate
import kotlinx.coroutines.flow.StateFlow

interface DownloadRepository {
    val allDownloads: StateFlow<List<DownloadInfo>>
    val downloadStates: StateFlow<Map<String, DownloadState>>
    fun enqueueDownload(otaUpdate: OtaUpdate, deviceName: String, regionName: String)
    fun pauseDownload(downloadInfo: DownloadInfo)
    fun resumeDownload(downloadInfo: DownloadInfo)
    fun cancelDownload(downloadInfo: DownloadInfo)
    fun retryDownload(downloadInfo: DownloadInfo)
    fun deleteDownload(downloadInfo: DownloadInfo)
    fun getTargetFile(otaUpdate: OtaUpdate, deviceName: String, regionName: String): java.io.File
    fun deleteFile(file: java.io.File): Boolean
}