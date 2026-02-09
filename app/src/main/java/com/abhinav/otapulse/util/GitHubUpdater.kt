package com.abhinav.otapulse.util

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request

data class UpdateInfo(
    val version: String,
    val downloadUrl: String,
    val changelog: String
)

object GitHubUpdater {
    private val client = OkHttpClient()
    private const val TAG = "GitHubUpdater"

    // Verify these match your repo exactly
    private const val REPO_OWNER = "RemuruSama"
    private const val REPO_NAME = "OTA-Pulse"

    fun checkForUpdate(currentVersion: String, onResult: (UpdateInfo?) -> Unit) {
        val url = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"
        Log.d(TAG, "Checking for updates from: $url")

        val request = Request.Builder().url(url).build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val jsonString = response.body?.string()

                if (!response.isSuccessful) {
                    Log.e(TAG, "Network Error: ${response.code}")
                    onResult(null)
                    return@Thread
                }

                if (jsonString != null) {
                    Log.d(TAG, "Response: $jsonString")

                    val json = Gson().fromJson(jsonString, JsonObject::class.java)
                    val latestTag = json.get("tag_name").asString

                    val cleanLatest = latestTag.removePrefix("v")
                    val cleanCurrent = currentVersion.removePrefix("v")

                    Log.d(TAG, "Comparing Local: $cleanCurrent vs Remote: $cleanLatest")

                    if (cleanLatest != cleanCurrent) {
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
                Log.e(TAG, "Exception: ${e.message}")
                e.printStackTrace()
                onResult(null)
            }
        }.start()
    }
}