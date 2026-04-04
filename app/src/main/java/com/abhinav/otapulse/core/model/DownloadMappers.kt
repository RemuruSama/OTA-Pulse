package com.abhinav.otapulse.core.model

import com.tonyodev.fetch2.Download
import java.io.File

/**
 * Maps a Fetch [Download] object to our domain [DownloadInfo] model.
 * 
 * @param newFilePath Optional override for the file path (used during completion or file movement).
 * @param smoothedSpeed Optional override for the download speed (used for UI smoothing).
 */
fun Download.toDownloadInfo(
    newFilePath: String? = null,
    smoothedSpeed: Long? = null
): DownloadInfo {
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
        speed = effectiveSpeed,
        totalBytes = this.total,
        original = this,
        otaUpdate = if (otaUpdateString.isBlank()) null else OtaUpdate.fromString(otaUpdateString),
        deviceName = deviceName,
        regionName = regionName
    )
}
