package com.abhinav.otapulse.core.common

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.abhinav.otapulse.core.model.OtaUpdate
import com.abhinav.otapulse.core.network.Component
import com.google.gson.GsonBuilder
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

object OtaJsonOutputHelper {

    fun getJsonOutput(otaUpdate: OtaUpdate): String {
        val source = otaUpdate.rawJson?.takeIf { it.isNotBlank() }
            ?: GsonBuilder().setPrettyPrinting().create().toJson(otaUpdate)
        return prettyPrintJson(source)
    }

    fun buildExportFileName(otaUpdate: OtaUpdate): String {
        val baseName = listOfNotNull(
            otaUpdate.versionName?.takeIf { it.isNotBlank() },
            otaUpdate.componentName.takeIf { it.isNotBlank() }
        ).firstOrNull().orEmpty()
            .replace(Regex("[^A-Za-z0-9._-]+"), "_")
            .trim('_')
            .take(64)
            .ifBlank { "ota_output" }

        return "${baseName}_${System.currentTimeMillis()}.json"
    }

    fun exportToDownloads(context: Context, otaUpdate: OtaUpdate): Result<String> {
        val fileName = buildExportFileName(otaUpdate)
        return runCatching {
            val targetFile = resolveJsonTargetFile(otaUpdate, fileName)
            targetFile.parentFile?.mkdirs()
            targetFile.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(getJsonOutput(otaUpdate))
            }
            fileName
        }
    }

    fun saveToUri(context: Context, uri: Uri, otaUpdate: OtaUpdate): Result<Unit> =
        runCatching {
            writeToUri(context, uri, otaUpdate)
        }

    private fun prettyPrintJson(value: String): String {
        val trimmed = value.trim()
        return runCatching {
            when {
                trimmed.startsWith("{") -> JSONObject(trimmed).toString(2)
                trimmed.startsWith("[") -> JSONArray(trimmed).toString(2)
                else -> value
            }
        }.getOrElse { value }
    }

    private fun writeToUri(context: Context, uri: Uri, otaUpdate: OtaUpdate) {
        context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8).use { writer ->
            if (writer == null) throw IOException("Could not open export stream")
            writer.write(getJsonOutput(otaUpdate))
        }
    }

    private fun resolveJsonTargetFile(otaUpdate: OtaUpdate, fileName: String): File {
        val baseDir = File(
            Environment.getExternalStorageDirectory(),
            Component.OTA_UPDATES_DIR
        )
        val jsonDir = File(baseDir, "JSON")
        return File(jsonDir, fileName)
    }
}
