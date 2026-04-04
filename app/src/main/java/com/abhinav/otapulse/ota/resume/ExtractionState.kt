package com.abhinav.otapulse.ota.resume

import android.content.Context
import com.google.gson.Gson
import java.io.File

data class ExtractionState(
    val partitionName: String,
    val lastCompletedOpIndex: Int,
    val bytesWritten: Long,
    val totalBytes: Long
) {
    val progressPercent: Int get() =
        if (totalBytes > 0) ((bytesWritten * 100) / totalBytes).toInt() else 0

    val formattedProgress: String get() = "$progressPercent% (${formatBytes(bytesWritten)} / ${formatBytes(totalBytes)})"

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1_073_741_824L -> "%.2f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576L     -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1024L          -> "%.1f KB".format(bytes / 1024.0)
        else                    -> "$bytes B"
    }
}

/**
 * Persists extraction progress to internal storage so extractions can
 * resume after an app restart, network interruption, or crash.
 *
 * State files are stored in filesDir/ota_state/<partitionName>.json
 * and deleted automatically on successful completion.
 */
class ExtractionStateStore(context: Context) {

    private val gson = Gson()
    private val dir  = File(context.filesDir, "ota_state").also { it.mkdirs() }

    fun save(state: ExtractionState) {
        try {
            val file = File(dir, "${state.partitionName}.json")
            file.writeText(gson.toJson(state))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun load(partitionName: String): ExtractionState? {
        val file = File(dir, "$partitionName.json")
        return if (file.exists()) {
            runCatching { 
                gson.fromJson(file.readText(), ExtractionState::class.java) 
            }.getOrNull()
        } else null
    }

    fun clear(partitionName: String) {
        File(dir, "$partitionName.json").delete()
    }

    fun clearAll() {
        dir.listFiles()?.forEach { it.delete() }
    }

    fun hasSavedState(partitionName: String): Boolean =
        File(dir, "$partitionName.json").exists()
}
