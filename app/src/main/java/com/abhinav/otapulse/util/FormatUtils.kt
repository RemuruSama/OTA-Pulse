package com.abhinav.otapulse.util

import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

object FormatUtils {
    fun formatSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(size / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
    }

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
}