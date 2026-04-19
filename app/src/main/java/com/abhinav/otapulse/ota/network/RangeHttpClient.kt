package com.abhinav.otapulse.ota.network

import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class RangeHttpClient(
    connectTimeoutSec: Long = 30,
    readTimeoutSec: Long = 120
) {
    private val connectTimeoutMillis = (connectTimeoutSec * 1000).toInt()
    private val readTimeoutMillis = (readTimeoutSec * 1000).toInt()

    private fun openConnection(urlStr: String, method: String = "GET"): HttpURLConnection {
        return (URL(urlStr).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = connectTimeoutMillis
            readTimeout = readTimeoutMillis
            requestMethod = method
            setRequestProperty("Accept-Encoding", "identity")

            if (com.abhinav.otapulse.feature.downloads.data.DownloadManager.isDownloadCheckUrl(urlStr)) {
                setRequestProperty("userId", "oplus-ota|16002018")
                setRequestProperty("User-Agent", "okhttp/3.12.12")
            }
        }
    }

    /** Fetch bytes [start]..[end] inclusive. Enforces HTTP 206 with retry logic. */
    fun fetchBytes(url: String, start: Long, end: Long): ByteArray {
        var lastException: Exception? = null
        var delay = 2000L // Start with 2s delay
        val maxRetries = 5

        for (attempt in 0..maxRetries) {
            var conn: HttpURLConnection? = null
            try {
                conn = openConnection(url)
                conn.setRequestProperty("Range", "bytes=$start-$end")
                conn.connect()
                
                val code = conn.responseCode
                if (code == 429 || code >= 500) {
                    val msg = "Server returned $code for $start-$end"
                    if (attempt < maxRetries) {
                        android.util.Log.w("RangeHttpClient", "$msg, retrying in ${delay}ms... (Attempt ${attempt + 1})")
                        Thread.sleep(delay)
                        delay *= 2
                        continue
                    } else {
                        error(msg)
                    }
                }

                if (code != 206 && code != 200) {
                    error("Expected 206/200, got $code. Msg: ${conn.responseMessage}")
                }
                
                return conn.inputStream.readBytes()
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries) {
                    Thread.sleep(delay)
                    delay *= 2
                } else {
                    throw e
                }
            } finally {
                conn?.disconnect()
            }
        }
        throw lastException ?: IllegalStateException("Failed to fetch $start-$end after $maxRetries retries")
    }

    /** Open a streaming response for large ranges — caller must close the stream. */
    fun openStream(url: String, start: Long, end: Long): InputStream {
        val conn = openConnection(url)
        conn.setRequestProperty("Range", "bytes=$start-$end")
        conn.connect()
        val code = conn.responseCode
        if (code != 206 && code != 200) {
            conn.disconnect()
            error("Expected HTTP 206/200 for Range $start-$end, got $code.")
        }
        // Wrapping input stream to ensure connection is disconnected when stream is closed
        return object : java.io.FilterInputStream(conn.inputStream) {
            override fun close() {
                super.close()
                conn.disconnect()
            }
        }
    }

    fun getContentLength(url: String): Long {
        // Some servers (like Aliyun OSS) require a GET request with Range=0-0 to get the total size
        // instead of a HEAD request if the URL is pre-signed for GET.
        var conn: HttpURLConnection? = null
        try {
            conn = openConnection(url)
            conn.setRequestProperty("Range", "bytes=0-0")
            conn.connect()
            
            val contentRange = conn.getHeaderField("Content-Range")
            if (contentRange != null && contentRange.contains("/")) {
                return contentRange.substringAfterLast("/").toLongOrNull() ?: -1L
            } else {
                return conn.getHeaderField("Content-Length")?.toLongOrNull() ?: -1L
            }
        } finally {
            conn?.disconnect()
        }
    }
}
