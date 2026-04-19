package com.abhinav.otapulse.core.download

/**
 * Callback interface for download lifecycle events.
 * This is our replacement for `com.tonyodev.fetch2.FetchListener`.
 *
 * All callbacks are delivered on the main thread.
 */
interface DownloadListener {
    fun onAdded(record: DownloadRecord) {}
    fun onQueued(record: DownloadRecord, waitingOnNetwork: Boolean) {}
    fun onStarted(record: DownloadRecord) {}
    fun onProgress(record: DownloadRecord, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long) {}
    fun onCompleted(record: DownloadRecord) {}
    fun onError(record: DownloadRecord, error: DownloadError, throwable: Throwable?) {}
    fun onPaused(record: DownloadRecord) {}
    fun onResumed(record: DownloadRecord) {}
    fun onCancelled(record: DownloadRecord) {}
    fun onRemoved(record: DownloadRecord) {}
    fun onDeleted(record: DownloadRecord) {}
}
