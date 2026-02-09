package com.abhinav.otapulse.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object OtaResolver {

    private const val TAG = "OtaResolver"

    suspend fun resolveUrl(originalUrl: String): String = withContext(Dispatchers.IO) {
        if (!originalUrl.startsWith("http")) {
            return@withContext originalUrl
        }

        val cleanUrl = originalUrl.replace("\\u0026", "&")
        var resolvedUrl = cleanUrl

        var connection: HttpURLConnection? = null
        try {
            val url = URL(cleanUrl)
            connection = url.openConnection() as HttpURLConnection

            connection.instanceFollowRedirects = true
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.requestMethod = "GET"

            connection.setRequestProperty("userId", "oplus-ota|16002018")
            connection.setRequestProperty("User-Agent", "okhttp/3.12.12")
            connection.setRequestProperty("Accept", "*/*")

            // CRITICAL: "identity" must match DownloadManager.
            // If we use "gzip", the server creates a different session type, failing the Resume check.
            connection.setRequestProperty("Accept-Encoding", "identity")

            connection.setRequestProperty("Connection", "Keep-Alive")
            connection.setRequestProperty("Cache-Control", "no-cache")

            connection.connect()

            val finalUrl = connection.url.toString()

            val contentType = connection.getContentType() ?: ""
            val contentLength = connection.getContentLengthLong()

            if ((contentType.contains("text/html", true) || contentType.contains("application/json", true))
                && contentLength > 0 && contentLength < 1024) {
                Log.w(TAG, "Resolved URL seems to be a fallback/error page (Type: $contentType, Size: $contentLength).")
            }

            if (finalUrl != cleanUrl) {
                Log.i(TAG, "URL Resolved: $cleanUrl -> $finalUrl")
                resolvedUrl = finalUrl
            } else {
                Log.d(TAG, "URL did not change after resolution.")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve URL: ${e.message}", e)
            resolvedUrl = cleanUrl
        } finally {
            connection?.disconnect()
        }

        return@withContext resolvedUrl
    }
}