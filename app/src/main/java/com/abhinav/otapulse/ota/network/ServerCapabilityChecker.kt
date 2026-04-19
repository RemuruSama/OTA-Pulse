package com.abhinav.otapulse.ota.network

import java.net.HttpURLConnection
import java.net.URL

data class ServerCapabilities(
    val supportsRangeRequests: Boolean,
    val contentLength: Long,
    val acceptRangesHeader: String?,
    val etag: String?
)

class ServerCapabilityChecker {

    private fun openConnection(urlStr: String, method: String = "GET"): HttpURLConnection {
        return (URL(urlStr).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 30_000
            readTimeout = 30_000
            requestMethod = method
            setRequestProperty("Accept-Encoding", "identity")

            if (com.abhinav.otapulse.feature.downloads.data.DownloadManager.isDownloadCheckUrl(urlStr)) {
                setRequestProperty("userId", "oplus-ota|16002018")
                setRequestProperty("User-Agent", "okhttp/3.12.12")
            }
        }
    }

    /**
     * Checks whether the server at [url] supports HTTP Range requests
     * and returns the content length and ETag for cache validation.
     *
     * First checks the `Accept-Ranges` response header. If absent,
     * performs a live probe request to verify Range support empirically.
     */
    fun check(url: String): ServerCapabilities {
        var conn: HttpURLConnection? = null
        try {
            conn = openConnection(url)
            // Some servers require Range: bytes=0-0 to get the total size
            conn.setRequestProperty("Range", "bytes=0-0")
            conn.connect()
            
            val code = conn.responseCode
            val acceptRanges = conn.getHeaderField("Accept-Ranges")
            val etag = conn.getHeaderField("ETag")
            
            val contentRange = conn.getHeaderField("Content-Range")
            val contentLength = if (contentRange != null && contentRange.contains("/")) {
                contentRange.substringAfterLast("/").toLongOrNull() ?: -1L
            } else {
                conn.getHeaderField("Content-Length")?.toLongOrNull() ?: -1L
            }

            val rangeSupported = when {
                acceptRanges == "bytes" -> true
                code == 206             -> true
                acceptRanges == "none"  -> false
                else                    -> probeRangeSupport(url)
            }

            return ServerCapabilities(
                supportsRangeRequests = rangeSupported,
                contentLength         = contentLength,
                acceptRangesHeader    = acceptRanges,
                etag                  = etag
            )
        } finally {
            conn?.disconnect()
        }
    }

    /** Sends a 4-byte Range probe to empirically verify server support. */
    private fun probeRangeSupport(url: String): Boolean {
        var conn: HttpURLConnection? = null
        return try {
            conn = openConnection(url)
            conn.setRequestProperty("Range", "bytes=0-3")
            conn.connect()
            conn.responseCode == 206
        } catch (e: Exception) {
            false
        } finally {
            conn?.disconnect()
        }
    }
}
