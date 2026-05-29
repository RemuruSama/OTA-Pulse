package com.abhinav.otapulse.core.common

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Streaming MD5 verification for downloaded OTA files.
 * Uses an 8 KB buffer to avoid loading multi-GB files into memory.
 */
object Md5Verifier {

    private const val TAG = "Md5Verifier"
    private const val BUFFER_SIZE = 8192

    /**
     * Computes the MD5 hash of [file] and compares it against [expectedMd5].
     *
     * - Returns [VerificationResult.SKIPPED] if [expectedMd5] is blank.
     * - Returns [VerificationResult.ERROR] if the file doesn't exist or an IO error occurs.
     * - Returns [VerificationResult.VERIFIED] or [VerificationResult.FAILED] based on comparison.
     *
     * This is a suspending function that runs on [Dispatchers.IO] and supports cancellation.
     */
    suspend fun verify(file: File, expectedMd5: String): VerificationResult = withContext(Dispatchers.IO) {
        if (expectedMd5.isBlank()) {
            Log.d(TAG, "No MD5 hash provided — skipping verification for ${file.name}")
            return@withContext VerificationResult.SKIPPED
        }

        if (!file.exists() || !file.isFile) {
            Log.w(TAG, "File not found for verification: ${file.absolutePath}")
            return@withContext VerificationResult.ERROR
        }

        try {
            val digest = MessageDigest.getInstance("MD5")
            val buffer = ByteArray(BUFFER_SIZE)

            file.inputStream().buffered().use { input ->
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    ensureActive() // Support coroutine cancellation
                    digest.update(buffer, 0, bytesRead)
                }
            }

            val computedMd5 = digest.digest().joinToString("") { "%02x".format(it) }
            val matches = computedMd5.equals(expectedMd5, ignoreCase = true)

            if (matches) {
                Log.i(TAG, "MD5 verified for ${file.name}: $computedMd5")
                VerificationResult.VERIFIED
            } else {
                Log.w(TAG, "MD5 mismatch for ${file.name}: expected=$expectedMd5, computed=$computedMd5")
                VerificationResult.FAILED
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during MD5 verification for ${file.name}", e)
            VerificationResult.ERROR
        }
    }
}

enum class VerificationResult {
    VERIFIED,
    FAILED,
    SKIPPED,
    ERROR
}
