package com.abhinav.otapulse.core.common

import java.text.DecimalFormat

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

    private fun formatUnit(value: Double, unit: String): String {
        val pattern = if (value % 1.0 == 0.0) "#,##0" else "#,##0.#"
        return "${DecimalFormat(pattern).format(value)} $unit"
    }
}
