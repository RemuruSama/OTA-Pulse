package com.abhinav.otapulse.data.repository

import android.util.Log
import com.abhinav.otapulse.domain.model.AppUpdateInfo
import com.abhinav.otapulse.domain.repository.AppUpdateRepository
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

class AppUpdateRepositoryImpl @Inject constructor(
    private val client: OkHttpClient
) : AppUpdateRepository {

    private val TAG = "AppUpdateRepository"
    private val REPO_OWNER = "RemuruSama"
    private val REPO_NAME = "OTA-Pulse"

    override suspend fun checkForUpdate(currentVersion: String): Result<AppUpdateInfo?> = withContext(Dispatchers.IO) {
        val url = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"
        Log.d(TAG, "Checking for updates from: $url")

        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()
            val jsonString = response.body?.string()

            if (!response.isSuccessful) {
                Log.e(TAG, "Network Error: ${response.code}")
                return@withContext Result.failure(Exception("Network Error: ${response.code}"))
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
                        val updateInfo = AppUpdateInfo(latestTag, downloadUrl, body)
                        return@withContext Result.success(updateInfo)
                    } else {
                        Log.e(TAG, "No assets (APK) attached to the release!")
                        return@withContext Result.failure(Exception("No assets attached to release"))
                    }
                } else {
                    return@withContext Result.success(null) // No update available
                }
            }
            return@withContext Result.failure(Exception("Empty response"))
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}")
            return@withContext Result.failure(e)
        }
    }
}
