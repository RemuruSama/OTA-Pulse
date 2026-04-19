package com.abhinav.otapulse.core.download

enum class DownloadStatus {
    /** Not yet started; exists in queue. */
    ADDED,
    /** Waiting for a worker slot or network. */
    QUEUED,
    /** Actively downloading bytes. */
    DOWNLOADING,
    /** User-paused; byte offset is persisted. */
    PAUSED,
    /** Download finished and file is intact. */
    COMPLETED,
    /** Download failed; see [DownloadError]. */
    FAILED,
    /** User explicitly cancelled. */
    CANCELLED,
    /** Unknown / sentinel (should not appear in practice). */
    NONE
}
