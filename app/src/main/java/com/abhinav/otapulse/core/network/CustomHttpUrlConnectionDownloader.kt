package com.abhinav.otapulse.core.network

import android.util.Log
import com.tonyodev.fetch2.HttpUrlConnectionDownloader
import com.tonyodev.fetch2core.Downloader
import com.tonyodev.fetch2core.InterruptMonitor

class CustomHttpUrlConnectionDownloader(
    fileDownloaderType: Downloader.FileDownloaderType
) : HttpUrlConnectionDownloader(fileDownloaderType) {

    override fun execute(request: Downloader.ServerRequest, interruptMonitor: InterruptMonitor): Downloader.Response? {
        try {
            // Strip ETag headers so CDNs don't reject Range fallback
            val headersField = request::class.java.getDeclaredField("headers")
            headersField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val originalHeaders = headersField.get(request) as Map<String, String>
            
            val newHeaders = originalHeaders.toMutableMap()
            newHeaders.remove("If-Range")
            newHeaders.remove("If-Match")
            headersField.set(request, newHeaders)
        } catch (e: Exception) {
            Log.e("CustomDownloader", "Failed to strip If-Range via reflection", e)
        }

        var response = super.execute(request, interruptMonitor)

        // If the signature expired (403, 410, or Range Not Satisfiable 416)
        if (response != null && (response.code == 403 || response.code == 410 || response.code == 416)) {
            Log.i("CustomDownloader", "URL expired (Code ${response.code}). Transparently refreshing...")
            try {
                val extras = request.extras
                val otaUpdateStr = extras?.getString("otaUpdate", "")
                if (!otaUpdateStr.isNullOrBlank()) {
                    val otaUpdate = com.abhinav.otapulse.core.model.OtaUpdate.fromString(otaUpdateStr)
                    
                    if (otaUpdate != null) {
                        val newUrlInfo = kotlinx.coroutines.runBlocking {
                            com.abhinav.otapulse.core.network.OtaResolver.resolveUrl(otaUpdate.downloadUrl)
                        }

                        if (newUrlInfo.url != request.url) {
                            Log.i("CustomDownloader", "Successfully retrieved fresh CDN URL. Swapping transparently!")
                            val urlField = request::class.java.getDeclaredField("url")
                            urlField.isAccessible = true
                            urlField.set(request, newUrlInfo.url)

                            // Execute the request again with the magically replaced URL
                            // Fetch2 has no idea the URL changed, so it never truncates the file!
                            response = super.execute(request, interruptMonitor)
                        } else {
                            Log.w("CustomDownloader", "Transparent refresh yielded the exact same URL.")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CustomDownloader", "Transparent URL refresh failed", e)
            }
        }

        return response
    }
}
