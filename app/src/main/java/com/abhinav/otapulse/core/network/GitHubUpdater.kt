package com.abhinav.otapulse.core.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class UpdateInfo(
    val version: String,
    val downloadUrl: String,
    val changelog: String
)

object GitHubUpdater {
    private const val TAG = "GitHubUpdater"
    private const val REPO_OWNER = "RemuruSama"
    private const val REPO_NAME = "OTA-Pulse"

    /**
     * Checks for a newer app release on GitHub using a raw Thread (for non-coroutine callers).
     * Uses the provided [OkHttpClient] singleton instead of creating its own instance.
     *
     * For new coroutine-based callers, prefer [AppUpdateRepositoryImpl] instead.
     */
    fun checkForUpdate(
        currentVersion: String,
        httpClient: OkHttpClient = OkHttpClient(),
        onResult: (UpdateInfo?) -> Unit
    ) {
        val url = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"
        Log.d(TAG, "Checking for updates from: $url")

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "OTAPulse/$currentVersion")
            .build()

        Thread {
            try {
                val response = httpClient.newCall(request).execute()
                val jsonString = response.body.string()

                if (!response.isSuccessful) {
                    Log.e(TAG, "Network Error: ${response.code}")
                    onResult(null)
                    return@Thread
                }

                if (jsonString.isNotEmpty()) {
                    Log.d(TAG, "Response: $jsonString")
                    val json = Gson().fromJson(jsonString, JsonObject::class.java)
                    val latestTag = json.get("tag_name").asString

                    val cleanLatest = latestTag.removePrefix("v")
                    val cleanCurrent = currentVersion.removePrefix("v")

                    Log.d(TAG, "Comparing Local: $cleanCurrent vs Remote: $cleanLatest")

                    if (isNewerVersion(cleanLatest, cleanCurrent)) {
                        val assets = json.getAsJsonArray("assets")
                        if (assets.size() > 0) {
                            val downloadUrl = assets[0].asJsonObject.get("browser_download_url").asString
                            val body = json.get("body").asString
                            onResult(UpdateInfo(latestTag, downloadUrl, body))
                            return@Thread
                        } else {
                            Log.e(TAG, "No assets (APK) attached to the release!")
                        }
                    }
                }
                onResult(null)
            } catch (e: Exception) {
                Log.e(TAG, "Exception: ${e.message}", e)
                onResult(null)
            }
        }.start()
    }

    /**
     * Compares two semantic version strings (e.g., "2.0.1" vs "2.0.0").
     * Returns true if [remote] is strictly newer than [local].
     */
    private fun isNewerVersion(remote: String, local: String): Boolean {
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val localParts = local.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false // Equal versions
    }
}
