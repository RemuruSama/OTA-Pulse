package com.abhinav.otapulse.ui.downloads

import androidx.lifecycle.ViewModel
import com.abhinav.otapulse.domain.model.DownloadInfo
import com.abhinav.otapulse.domain.model.OtaUpdate
import com.abhinav.otapulse.domain.repository.DownloadRepository
import com.abhinav.otapulse.domain.usecase.GetDownloadsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import java.net.URLDecoder
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    getDownloadsUseCase: GetDownloadsUseCase,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    val allDownloads: StateFlow<List<DownloadInfo>> = getDownloadsUseCase()

    fun startDownloadWithUrl(newDownloadUrl: String) {
        // Improved filename extraction logic
        val fileName = try {
            val urlObj = java.net.URL(newDownloadUrl)
            // Get path part of URL (ignores query params)
            val path = urlObj.path
            // Extract filename from path
            val rawName = path.substringAfterLast('/')

            if (rawName.isBlank()) {
                "downloaded_file.zip"
            } else {
                // Decode URL encoded characters (e.g. %20 -> space)
                URLDecoder.decode(rawName, "UTF-8")
            }
        } catch (e: Exception) {
            // Fallback for malformed URLs or other errors
            newDownloadUrl.substringAfterLast('/').takeIf { it.isNotBlank() }?.substringBefore('?') ?: "downloaded_file.zip"
        }

        val otaUpdate = OtaUpdate(
            componentId = UUID.randomUUID().toString(),
            componentName = "Direct Download",
            componentVersion = "Unknown Version",
            size = "0",
            manualUrl = null,
            url = newDownloadUrl,
            md5 = "",
            versionName = "Unknown Version", // Signals DownloadManager to use fallback/original filename
            realAndroidVersion = null,
            realOsVersion = null,
            securityPatch = null,
            panelUrl = null,
            fileName = fileName, // Pass the extracted original filename here
            downloadUrl = newDownloadUrl
        )

        // "Direct Download" tells DownloadManager to treat this as a manual entry
        downloadRepository.enqueueDownload(otaUpdate, "Direct Download", "External")
    }

    fun pauseDownload(downloadInfo: DownloadInfo) = downloadRepository.pauseDownload(downloadInfo)
    fun resumeDownload(downloadInfo: DownloadInfo) = downloadRepository.resumeDownload(downloadInfo)
    fun cancelDownload(downloadInfo: DownloadInfo) = downloadRepository.cancelDownload(downloadInfo)
    fun retryDownload(downloadInfo: DownloadInfo) = downloadRepository.retryDownload(downloadInfo)
    fun deleteDownload(downloadInfo: DownloadInfo) = downloadRepository.deleteDownload(downloadInfo)
}