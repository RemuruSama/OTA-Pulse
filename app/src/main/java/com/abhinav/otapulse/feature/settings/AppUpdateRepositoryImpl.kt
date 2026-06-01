package com.abhinav.otapulse.feature.settings

import android.util.Log
import com.abhinav.otapulse.core.model.AppUpdateInfo
import com.abhinav.otapulse.feature.settings.AppUpdateRepository
import com.google.gson.Gson
import com.google.gson.JsonArray
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

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "OTAPulse/$currentVersion")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val jsonString = response.body.string()

                if (!response.isSuccessful) {
                    Log.e(TAG, "Network Error: ${response.code}")
                    return@withContext Result.failure(Exception("Network Error: ${response.code}"))
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
                        val apkAsset = selectApkAsset(assets)
                        if (apkAsset != null) {
                            val downloadUrl = apkAsset.get("browser_download_url").asString
                            val body = json.get("body").asString
                            val updateInfo = AppUpdateInfo(latestTag, downloadUrl, body)
                            return@withContext Result.success(updateInfo)
                        } else {
                            Log.e(TAG, "No APK asset attached to the release!")
                            return@withContext Result.failure(Exception("No APK asset attached to release"))
                        }
                    } else {
                        return@withContext Result.success(null)
                    }
                }
                return@withContext Result.failure(Exception("Empty response"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}")
            return@withContext Result.failure(e)
        }
    }

    private fun selectApkAsset(assets: JsonArray?): JsonObject? {
        if (assets == null || assets.size() == 0) return null
        return assets
            .map { it.asJsonObject }
            .firstOrNull { asset ->
                val name = asset.get("name")?.asString.orEmpty()
                val contentType = asset.get("content_type")?.asString.orEmpty()
                name.endsWith(".apk", ignoreCase = true) ||
                    contentType.equals("application/vnd.android.package-archive", ignoreCase = true)
            }
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

    override suspend fun fetchChangelog(versionTag: String): Result<String?> = withContext(Dispatchers.IO) {
        // Try tag with "v" prefix first, then without
        val tags = listOf("v$versionTag", versionTag)
        for (tag in tags) {
            val url = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/tags/$tag"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "OTAPulse/$versionTag")
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val json = Gson().fromJson(response.body.string(), JsonObject::class.java)
                        val body = json.get("body")?.asString
                        if (!body.isNullOrBlank()) {
                            return@withContext Result.success(body)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch changelog for $tag: ${e.message}")
            }
        }
        return@withContext Result.success(null)
    }
}
