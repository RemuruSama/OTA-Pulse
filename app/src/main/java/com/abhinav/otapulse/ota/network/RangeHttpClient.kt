package com.abhinav.otapulse.ota.network

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.util.concurrent.TimeUnit

class RangeHttpClient(
    connectTimeoutSec: Long = 30,
    readTimeoutSec: Long = 120
) {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(connectTimeoutSec, TimeUnit.SECONDS)
        .readTimeout(readTimeoutSec, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("userId", "oplus-ota|16002018")
                .header("User-Agent", "okhttp/3.12.12")
                .header("Accept-Encoding", "identity")
                .build()
            chain.proceed(request)
        }
        .build()

    /** Fetch bytes [start]..[end] inclusive. Enforces HTTP 206 with retry logic. */
    fun fetchBytes(url: String, start: Long, end: Long): ByteArray {
        var lastException: Exception? = null
        var delay = 2000L // Start with 2s delay
        val maxRetries = 5

        for (attempt in 0..maxRetries) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("Range", "bytes=$start-$end")
                    .build()
                
                client.newCall(request).execute().use { response ->
                    if (response.code == 429 || response.code >= 500) {
                        val msg = "Server returned ${response.code} for $start-$end"
                        if (attempt < maxRetries) {
                            android.util.Log.w("RangeHttpClient", "$msg, retrying in ${delay}ms... (Attempt ${attempt + 1})")
                            Thread.sleep(delay)
                            delay *= 2 // Exponential backoff
                            return@use null // Continue loop via exception catch or null check
                        } else {
                            error(msg)
                        }
                    }

                    if (response.code != 206 && response.code != 200) {
                        error("Expected 206/200, got ${response.code}. Msg: ${response.message}")
                    }
                    return response.body!!.bytes()
                } ?: continue // Retry loop if null returned from use block
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries) {
                    Thread.sleep(delay)
                    delay *= 2
                } else {
                    throw e
                }
            }
        }
        throw lastException ?: IllegalStateException("Failed to fetch $start-$end after $maxRetries retries")
    }

    /** Open a streaming response for large ranges — caller must close the stream. */
    fun openStream(url: String, start: Long, end: Long): InputStream {
        val request = Request.Builder()
            .url(url)
            .header("Range", "bytes=$start-$end")
            .build()
        val response = client.newCall(request).execute()
        if (response.code != 206 && response.code != 200) {
            error("Expected HTTP 206/200 for Range $start-$end, got ${response.code}.")
        }
        return response.body!!.byteStream()
    }

    fun getContentLength(url: String): Long {
        // Some servers (like Aliyun OSS) require a GET request with Range=0-0 to get the total size
        // instead of a HEAD request if the URL is pre-signed for GET.
        val request = Request.Builder()
            .url(url)
            .header("Range", "bytes=0-0")
            .build()
        return client.newCall(request).execute().use { response ->
            val contentRange = response.header("Content-Range")
            if (contentRange != null && contentRange.contains("/")) {
                contentRange.substringAfterLast("/").toLongOrNull() ?: -1L
            } else {
                response.header("Content-Length")?.toLongOrNull() ?: -1L
            }
        }
    }
}
