package com.abhinav.otapulse.core.download

/**
 * Immutable snapshot of a single download entry.
 * This is our replacement for `com.tonyodev.fetch2.Download`.
 */
data class DownloadRecord(
    /** Stable numeric ID derived from the URL hash. */
    val id: Int,
    /** The resolved CDN URL used for this download. */
    val url: String,
    /** Absolute path of the target file on disk. */
    val file: String,
    val status: DownloadStatus,
    val error: DownloadError,
    /** Bytes written to disk so far. */
    val downloaded: Long,
    /** Content-Length reported by the server (-1 if unknown). */
    val total: Long,
    /** Instantaneous download speed in bytes/second. */
    val downloadedBytesPerSecond: Long,
    /** Estimated time remaining in milliseconds (-1 if unknown). */
    val etaInMilliSeconds: Long,
    /** Progress 0-100. */
    val progress: Int,
    /** Creation timestamp (epoch ms). */
    val created: Long,
    /**
     * Arbitrary key-value metadata attached to the download
     * (equivalent to `Fetch2's Extras`).
     */
    val extras: Map<String, String>
)
