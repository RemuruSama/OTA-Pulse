package com.abhinav.otapulse.core.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object OtaResolver {

    private const val TAG = "OtaResolver"

    data class ResolvedUrlInfo(val url: String, val contentDispositionFileName: String?)

    suspend fun resolveUrl(originalUrl: String): ResolvedUrlInfo = withContext(Dispatchers.IO) {
        if (!originalUrl.startsWith("http")) {
            return@withContext ResolvedUrlInfo(originalUrl, null)
        }

        var currentUrl = originalUrl.replace("\\u0026", "&")
        var contentDispositionFileName: String? = null
        var connection: HttpURLConnection? = null
        var redirectCount = 0
        val MAX_REDIRECTS = 10

        try {
            while (redirectCount < MAX_REDIRECTS) {
                val url = URL(currentUrl)
                connection = openConnection(url, "HEAD")
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_BAD_METHOD ||
                    connection.responseCode == HttpURLConnection.HTTP_FORBIDDEN ||
                    connection.responseCode == HttpURLConnection.HTTP_NOT_IMPLEMENTED
                ) {
                    connection.disconnect()
                    connection = openConnection(url, "GET").apply {
                        setRequestProperty("Range", "bytes=0-0")
                    }
                    connection.connect()
                }

                val responseCode = connection.responseCode
                
                // Check for Redirects
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                    responseCode == 307 || 
                    responseCode == 308) {
                    
                    val location = connection.getHeaderField("Location")
                    if (!location.isNullOrBlank()) {
                        redirectCount++
                        currentUrl = if (location.startsWith("/")) {
                             // Handle relative redirects (rare but possible)
                             val protocol = url.protocol
                             val host = url.host
                             "$protocol://$host$location"
                        } else {
                            location
                        }
                        Log.i(TAG, "Redirecting to: $currentUrl")
                        connection.disconnect()
                        continue
                    }
                }

                // If we are here, we are at the final destination (or error)
                // Extract Content-Disposition
                val contentDisposition = connection.getHeaderField("Content-Disposition")
                if (contentDisposition != null) {
                    val token = "filename="
                    val index = contentDisposition.indexOf(token)
                    if (index >= 0) {
                        var filename = contentDisposition.substring(index + token.length)
                        if (filename.startsWith("\"") && filename.endsWith("\"")) {
                            filename = filename.substring(1, filename.length - 1)
                        }
                        if (filename.isNotBlank()) {
                             contentDispositionFileName = filename
                        }
                    }
                }
                
                val contentType = connection.getContentType() ?: ""
                val contentLength = connection.getContentLengthLong()

                if ((contentType.contains("text/html", true) || contentType.contains("application/json", true))
                    && contentLength > 0 && contentLength < 1024) {
                    Log.w(TAG, "Resolved URL seems to be a fallback/error page (Type: $contentType, Size: $contentLength).")
                }

                break // Exit loop if not redirected
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve URL: ${e.message}", e)
            // Keep currentUrl as best effort
        } finally {
            connection?.disconnect()
        }

        return@withContext ResolvedUrlInfo(currentUrl, contentDispositionFileName)
    }

    private fun openConnection(url: URL, method: String): HttpURLConnection {
        return (url.openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            connectTimeout = 15000
            readTimeout = 15000
            requestMethod = method
            
            if (com.abhinav.otapulse.feature.downloads.data.DownloadManager.isDownloadCheckUrl(url.toString())) {
                setRequestProperty("userId", "oplus-ota|16002018")
                setRequestProperty("User-Agent", "okhttp/3.12.12")
            }
            setRequestProperty("Accept", "*/*")
            setRequestProperty("Accept-Encoding", "identity")
            setRequestProperty("Connection", "Keep-Alive")
            setRequestProperty("Cache-Control", "no-cache")
        }
    }
}
