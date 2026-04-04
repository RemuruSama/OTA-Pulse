package com.abhinav.otapulse.core.model

/**
 * Sealed class representing structured OTA error types.
 * Provides type-safe error handling across the data and UI layers.
 */
sealed class OtaError : Exception() {
    data class NetworkError(val code: Int, override val message: String) : OtaError()
    data class ServerError(val code: Int, val errMsg: String) : OtaError() {
        override val message: String get() = "Server Error $code: $errMsg"
    }
    data class DecryptionError(override val message: String) : OtaError()
    data class EmptyResponse(override val message: String = "No update components found.") : OtaError()
    data class UpdateCheckFailed(val reason: String) : OtaError() {
        override val message: String get() = "Update check failed: $reason"
    }
    data class Unknown(override val cause: Throwable?) : OtaError() {
        override val message: String get() = cause?.message ?: "An unknown error occurred."
    }
}
