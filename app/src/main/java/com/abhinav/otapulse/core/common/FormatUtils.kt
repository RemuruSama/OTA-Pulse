package com.abhinav.otapulse.core.common

import com.abhinav.otapulse.core.model.OtaUpdate
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit

object FormatUtils {

    /**
     * Formats a byte size into a human-readable string (B, KB, MB, GB).
     */
    fun formatSize(bytes: Long): String {
        val safeBytes = bytes.coerceAtLeast(0)
        return when {
            safeBytes >= 1_073_741_824L -> formatUnit(safeBytes / 1_073_741_824.0, "GB")
            safeBytes >= 1_048_576L -> formatUnit(safeBytes / 1_048_576.0, "MB")
            safeBytes >= 1024L -> formatUnit(safeBytes / 1024.0, "KB")
            else -> "$safeBytes B"
        }
    }

    /**
     * Formats download speed in bytes per second.
     */
    fun formatDownloadSpeed(bytesPerSecond: Long): String {
        if (bytesPerSecond < 0) return ""
        val (value, unit) = when {
            bytesPerSecond < 1024 -> Pair(bytesPerSecond.toDouble(), "B/s")
            bytesPerSecond < 1024 * 1024 -> Pair(bytesPerSecond / 1024.0, "KB/s")
            else -> Pair(bytesPerSecond / (1024.0 * 1024.0), "MB/s")
        }
        val decimalFormat = if (unit == "B/s") DecimalFormat("#,##0") else DecimalFormat("#,##0.0")
        return "${decimalFormat.format(value)} $unit"
    }

    /**
     * Categorizes partition size for UI highlighting (e.g., LARGE for big binary partitions).
     */
    fun getSizeCategory(sizeBytes: Long): String = when {
        sizeBytes >= 50L * 1_048_576L -> "LARGE"
        sizeBytes >= 1_048_576L -> "MED"
        else -> "SMALL"
    }

    /**
     * Returns a color representing the size category (Green for Small, Orange for Medium, Red for Large).
     */
    fun getSizeColor(sizeBytes: Long): Int = when (getSizeCategory(sizeBytes)) {
        "LARGE" -> 0xFFB71C1C.toInt()
        "MED"   -> 0xFFF57F17.toInt()
        else    -> 0xFF2E7D32.toInt()
    }

    fun formatTimestamp(millis: Long): String {
        if (millis <= 0) return "N/A"
        return try {
            val date = java.util.Date(if (millis < 10000000000L) millis * 1000L else millis)
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(date)
        } catch (e: Exception) {
            "N/A"
        }
    }

    /**
     * Extracts and formats the build date from the OTA update's target version or version strings.
     * Falls back to publishedTime if no timestamp is embedded in the version strings.
     */
    fun formatBuildDate(ota: OtaUpdate): String {
        val candidates = listOfNotNull(
            ota.otaTargetVersion,
            ota.realOtaVersion,
            ota.versionName,
            ota.realVersionName,
            ota.colorOSVersion,
            ota.osVersion
        )
        for (candidate in candidates) {
            try {
                val regex = Regex("_(20\\d{2})(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])(?:([01]\\d|2[0-3])([0-5]\\d)([0-5]\\d)?)?")
                val match = regex.findAll(candidate).lastOrNull()
                if (match != null) {
                    val year = match.groupValues[1]
                    val month = match.groupValues[2]
                    val day = match.groupValues[3]
                    val hour = match.groupValues[5]
                    val minute = match.groupValues[6]
                    return if (hour.isNotEmpty() && minute.isNotEmpty()) {
                        "$year-$month-$day $hour:$minute"
                    } else {
                        "$year-$month-$day"
                    }
                }
            } catch (e: Exception) {
                // continue to next candidate
            }
        }
        return formatTimestamp(ota.publishedTime)
    }

    /**
     * Formats estimated time remaining in milliseconds into a user-friendly string (e.g., "2m 15s left", "45s left", "1h 10m left").
     */
    fun formatEta(etaInMilliSeconds: Long): String {
        if (etaInMilliSeconds <= 0) return "--"
        val hours = TimeUnit.MILLISECONDS.toHours(etaInMilliSeconds)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(etaInMilliSeconds) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(etaInMilliSeconds) % 60

        return when {
            hours > 0 -> String.format("%dh %02dm left", hours, minutes)
            minutes > 0 -> String.format("%02dm %02ds left", minutes, seconds)
            else -> String.format("%ds left", seconds)
        }
    }

    private fun formatUnit(value: Double, unit: String): String {
        val pattern = if (value % 1.0 == 0.0) "#,##0" else "#,##0.#"
        return "${DecimalFormat(pattern).format(value)} $unit"
    }
}
