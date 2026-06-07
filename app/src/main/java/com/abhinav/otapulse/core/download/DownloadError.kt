package com.abhinav.otapulse.core.download

enum class DownloadError {
    NONE,
    NO_NETWORK_CONNECTION,
    CONNECTION_TIMED_OUT,
    /** Server returned a non-2xx status that's not specifically handled. */
    REQUEST_NOT_SUCCESSFUL,
    /** HTTP 404. */
    HTTP_NOT_FOUND,
    /** Generic IO problem (disk full, stream cut, etc.) */
    UNKNOWN_IO_ERROR,
    INSUFFICIENT_STORAGE,
    UNKNOWN;

    companion object {
        /** Map an HTTP status code to the closest [DownloadError]. */
        fun fromHttpCode(code: Int): DownloadError = when (code) {
            404 -> HTTP_NOT_FOUND
            in 400..499 -> REQUEST_NOT_SUCCESSFUL
            else -> UNKNOWN
        }
    }
}
