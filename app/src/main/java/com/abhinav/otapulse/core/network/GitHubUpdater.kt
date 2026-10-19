package com.abhinav.otapulse.core.network

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

    private val updaterScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)

    /**
     * Checks for a newer app release on GitHub asynchronously.
     */
    suspend fun checkForUpdate(
        currentVersion: String,
        httpClient: OkHttpClient = OkHttpClient()
    ): UpdateInfo? = withContext(Dispatchers.IO) {
        checkUpdateInternal(currentVersion, httpClient)
    }

    /**
     * Checks for a newer app release on GitHub using coroutine dispatch (for callback callers).
     * Uses the provided [OkHttpClient] singleton instead of creating its own instance.
     */
    fun checkForUpdate(
        currentVersion: String,
        httpClient: OkHttpClient = OkHttpClient(),
        onResult: (UpdateInfo?) -> Unit
    ) {
        updaterScope.launch {
            val result = checkUpdateInternal(currentVersion, httpClient)
            dispatchResult(result, onResult)
        }
    }

    private fun checkUpdateInternal(
        currentVersion: String,
        httpClient: OkHttpClient
    ): UpdateInfo? {
        val url = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"
        Log.d(TAG, "Checking for updates from: $url")

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "OTAPulse/$currentVersion")
            .build()

        try {
            val (isSuccess, code, jsonString) = httpClient.newCall(request).execute().use { response ->
                Triple(response.isSuccessful, response.code, response.body.string())
            }

            if (!isSuccess) {
                Log.e(TAG, "Network Error: $code")
                return checkForUpdateFallback(currentVersion, httpClient)
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
                        return UpdateInfo(latestTag, downloadUrl, body)
                    } else {
                        Log.e(TAG, "No assets (APK) attached to the release!")
                    }
                } else {
                    return null
                }
            }
            return checkForUpdateFallback(currentVersion, httpClient)
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}", e)
            return checkForUpdateFallback(currentVersion, httpClient)
        }
    }

    private fun checkForUpdateFallback(
        currentVersion: String,
        httpClient: OkHttpClient
    ): UpdateInfo? {
        try {
            val webUrl = "https://github.com/$REPO_OWNER/$REPO_NAME/releases/latest"
            val request = Request.Builder()
                .url(webUrl)
                .header("User-Agent", "OTAPulse/$currentVersion")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val tag = response.request.url.pathSegments.lastOrNull().orEmpty()
                    if (tag.isNotEmpty() && !tag.equals("latest", ignoreCase = true)) {
                        val cleanLatest = tag.removePrefix("v")
                        val cleanCurrent = currentVersion.removePrefix("v")
                        if (isNewerVersion(cleanLatest, cleanCurrent)) {
                            val assetsUrl = "https://github.com/$REPO_OWNER/$REPO_NAME/releases/expanded_assets/$tag"
                            val assetsReq = Request.Builder().url(assetsUrl).header("User-Agent", "OTAPulse/$currentVersion").build()
                            val downloadUrl = runCatching {
                                httpClient.newCall(assetsReq).execute().use { assetsResp ->
                                    val html = assetsResp.body.string()
                                    val match = Regex("""href="([^"]+\.apk)"""", RegexOption.IGNORE_CASE).find(html)
                                    match?.groupValues?.get(1)?.let { if (it.startsWith("/")) "https://github.com$it" else it }
                                }
                            }.getOrNull() ?: "https://github.com/$REPO_OWNER/$REPO_NAME/releases/download/$tag/otapulse_update_$cleanLatest.apk"

                            return UpdateInfo(tag, downloadUrl, "New release $tag available on GitHub.")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fallback check failed: ${e.message}")
        }
        return null
    }

    private fun dispatchResult(result: UpdateInfo?, onResult: (UpdateInfo?) -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            onResult(result)
        } else {
            Handler(Looper.getMainLooper()).post { onResult(result) }
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
}

