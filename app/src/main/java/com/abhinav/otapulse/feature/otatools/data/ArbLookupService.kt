package com.abhinav.otapulse.feature.otatools.data

import android.util.Log
import com.abhinav.otapulse.arb.parser.ArbChecker
import com.abhinav.otapulse.core.network.OtaResolver
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extracts verified ARB (Anti-Rollback) status directly from the OTA payload
 * using HTTP Range Requests — no external database dependency.
 *
 * Always resolves the download URL first (follows redirects) to ensure
 * Range Requests hit the final CDN endpoint, not a redirect URL.
 *
 * Flow: OTA download URL → resolve redirects → payload.bin → xbl_config.img → ELF64 → ARB index
 */
@Singleton
class ArbLookupService @Inject constructor(private val arbChecker: ArbChecker) {

    companion object {
        private const val TAG = "ArbLookupService"
    }

    data class ArbInfo(
        val arbIndex: Int,
        val major: Int,
        val minor: Int,
        val deviceName: String? = null,
        val versionName: String? = null,
        val status: String? = null
    ) {
        val isSafe: Boolean get() = arbIndex == 0

        /**
         * Formatted display string for UI, e.g. "ARB 0 (Safe)"
         */
        fun toDisplayString(): String {
            return if (isSafe) "Safe" else "Protected"
        }
    }

    // Cache results keyed by original URL to avoid re-resolving + re-extracting
    private val cache = mutableMapOf<String, ArbInfo?>()

    /**
     * Extract ARB info from the OTA download URL.
     * Resolves the URL first (follows redirects) before running Range Requests.
     * Returns null if extraction fails or device doesn't have xbl_config.
     */
    suspend fun lookupByUrl(downloadUrl: String): ArbInfo? {
        if (downloadUrl.isBlank()) return null

        // Check cache (keyed on original URL)
        cache[downloadUrl]?.let { return it }

        return try {
            // Resolve the URL to follow all redirects → final CDN endpoint
            val resolved = OtaResolver.resolveUrl(downloadUrl)
            Log.d(TAG, "Resolved URL for ARB: ${resolved.url}")

            val result = arbChecker.checkArb(resolved.url)
            val arbInfo = result?.let {
                ArbInfo(
                    arbIndex = it.arb,
                    major = it.major,
                    minor = it.minor
                )
            }
            cache[downloadUrl] = arbInfo
            arbInfo
        } catch (e: Exception) {
            Log.w(TAG, "ARB lookup failed for $downloadUrl", e)
            null
        }
    }
}
