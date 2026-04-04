package com.abhinav.otapulse.ota.network

import okhttp3.Request

data class ServerCapabilities(
    val supportsRangeRequests: Boolean,
    val contentLength: Long,
    val acceptRangesHeader: String?,
    val etag: String?
)

class ServerCapabilityChecker(private val http: RangeHttpClient) {

    /**
     * Checks whether the server at [url] supports HTTP Range requests
     * and returns the content length and ETag for cache validation.
     *
     * First checks the `Accept-Ranges` response header. If absent,
     * performs a live probe request to verify Range support empirically.
     */
    fun check(url: String): ServerCapabilities {
        // Some servers require Range: bytes=0-0 to get the total size
        val request = Request.Builder()
            .url(url)
            .header("Range", "bytes=0-0")
            .build()
        val response = http.client.newCall(request).execute()

        response.use {
            val acceptRanges  = it.header("Accept-Ranges")
            val etag          = it.header("ETag")
            
            val contentRange = it.header("Content-Range")
            val contentLength = if (contentRange != null && contentRange.contains("/")) {
                contentRange.substringAfterLast("/").toLongOrNull() ?: -1L
            } else {
                it.header("Content-Length")?.toLongOrNull() ?: -1L
            }

            val rangeSupported = when {
                acceptRanges == "bytes" -> true
                it.code == 206          -> true
                acceptRanges == "none"  -> false
                else                    -> probeRangeSupport(url)
            }

            return ServerCapabilities(
                supportsRangeRequests = rangeSupported,
                contentLength         = contentLength,
                acceptRangesHeader    = acceptRanges,
                etag                  = etag
            )
        }
    }

    /** Sends a 4-byte Range probe to empirically verify server support. */
    private fun probeRangeSupport(url: String): Boolean {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("Range", "bytes=0-3")
                .build()
            val response = http.client.newCall(request).execute()
            response.use { it.code == 206 }
        } catch (e: Exception) {
            false
        }
    }
}
