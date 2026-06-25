package com.abhinav.otapulse.core.model

import com.abhinav.otapulse.core.download.DownloadRecord
import java.io.File

/**
 * Maps an [OkHttpDownloadEngine] [DownloadRecord] to our domain [DownloadInfo] model.
 *
 * @param newFilePath   Optional override for the file path (used during completion or file movement).
 * @param smoothedSpeed Optional override for the download speed (used for UI smoothing).
 */
fun DownloadRecord.toDownloadInfo(
    newFilePath: String? = null,
    smoothedSpeed: Long? = null,
    md5Status: Md5Status = Md5Status.NONE
): DownloadInfo {
    val otaUpdateString = extras["otaUpdate"] ?: ""
    val deviceName = extras["deviceName"] ?: ""
    val regionName = extras["regionName"] ?: ""
    val isFromHomeUpdate = (extras["isFromHomeUpdate"] ?: "false").toBoolean()

    val currentActualFilePath = newFilePath ?: this.file

    // Use smoothed speed if available (prevents spikes), otherwise raw speed.
    val effectiveSpeed = smoothedSpeed ?: this.downloadedBytesPerSecond

    return DownloadInfo(
        id = this.id,
        file = currentActualFilePath,
        fileName = File(currentActualFilePath).name,
        downloadedBytes = this.downloaded,
        progress = this.progress,
        status = this.status,
        error = this.error,
        eta = this.etaInMilliSeconds,
        speed = effectiveSpeed,
        totalBytes = this.total,
        original = this,
        otaUpdate = if (otaUpdateString.isBlank()) null else OtaUpdate.fromString(otaUpdateString),
        deviceName = deviceName,
        regionName = regionName,
        md5Status = md5Status,
        isFromHomeUpdate = isFromHomeUpdate
    )
}

