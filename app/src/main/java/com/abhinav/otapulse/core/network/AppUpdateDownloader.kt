package com.abhinav.otapulse.core.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

object AppUpdateDownloader {
    private const val TAG = "AppUpdateDownloader"
    private const val BUFFER_SIZE = 8192

    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(val progress: Int) : DownloadState()
        data class Success(val file: File) : DownloadState()
        data class Error(val exception: Exception) : DownloadState()
    }

    /**
     * Downloads a file from the given [url] and saves it to [targetFile].
     * Emits progress updates via a Kotlin Flow.
     */
    fun downloadApk(url: String, targetFile: File, client: OkHttpClient): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0))

        val request = Request.Builder()
            .url(url)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    emit(DownloadState.Error(Exception("HTTP error code: ${response.code}")))
                    return@flow
                }

                val body = response.body
                if (body == null) {
                    emit(DownloadState.Error(Exception("Empty response body")))
                    return@flow
                }

                val contentLength = body.contentLength()
                body.byteStream().use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var totalBytesRead = 0L
                        var bytesRead: Int
                        var lastEmitTime = System.currentTimeMillis()

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            // Check for cancellation
                            if (!currentCoroutineContext().isActive) {
                                return@flow
                            }

                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead

                            if (contentLength > 0) {
                                val progress = ((totalBytesRead * 100.0) / contentLength).toInt().coerceIn(0, 100)
                                val currentTime = System.currentTimeMillis()
                                // Throttle UI updates to roughly 10fps
                                if (currentTime - lastEmitTime > 100) {
                                    emit(DownloadState.Downloading(progress))
                                    lastEmitTime = currentTime
                                }
                            }
                        }
                    }
                }

                emit(DownloadState.Downloading(100))
                // Add a tiny delay to ensure 100% is displayed briefly before Success is handled
                delay(100) 
                emit(DownloadState.Success(targetFile))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download app update", e)
            emit(DownloadState.Error(e))
        }
    }.flowOn(Dispatchers.IO)
}
