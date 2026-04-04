package com.abhinav.otapulse.arb.parser

import android.util.Log
import com.abhinav.otapulse.ota.engine.OtaExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates the complete ARB extraction flow using the robust OtaExtractor engine.
 */
@Singleton
class ArbChecker @Inject constructor(
    private val otaExtractor: OtaExtractor
) {

    companion object {
        private const val TAG = "ArbChecker"
        private const val TIMEOUT_MS = 60_000L  // 60 seconds (generous for network/parsing)
        
        // Priority order for candidate partitions likely containing ARB metadata
        private val CANDIDATE_PARTITIONS = listOf("xbl_config", "xbl", "abl", "xbl_ramdump")
    }

    /**
     * Extract ARB status from an OTA download URL.
     *
     * @param otaDownloadUrl The URL to the OTA ZIP file
     * @return ArbResult with arb index + version, or null on failure
     */
    suspend fun checkArb(otaDownloadUrl: String): XblConfigParser.ArbResult? =
        withContext(Dispatchers.IO) {
            try {
                withTimeout(TIMEOUT_MS) {
                    doCheck(otaDownloadUrl)
                }
            } catch (e: Exception) {
                Log.w(TAG, "ARB check failed for $otaDownloadUrl: ${e.message}")
                null
            }
        }

    private suspend fun doCheck(url: String): XblConfigParser.ArbResult? {
        // 1. Open Session (this fetches metadata and manifest)
        val session = otaExtractor.open(url)
        val manifestPartitions = session.manifest.partitionsList.map { it.partitionName }
        
        // 2. Iterate through candidate partitions until one is found and successfully parsed
        for (partitionName in CANDIDATE_PARTITIONS) {
            if (!manifestPartitions.contains(partitionName)) {
                Log.v(TAG, "Partition $partitionName not present in manifest, skipping...")
                continue
            }

            Log.d(TAG, "Attempting ARB extraction from partition: $partitionName")
            
            try {
                // 3. Extract to memory
                val output = ByteArrayOutputStream()
                otaExtractor.extract(session, partitionName, output)
                
                val decompressedData = output.toByteArray()
                if (decompressedData.isEmpty()) {
                    Log.w(TAG, "Extracted $partitionName is empty, trying next candidate...")
                    continue
                }
                
                Log.d(TAG, "Successfully extracted $partitionName: ${decompressedData.size} bytes")

                // 4. Parse ELF64 and extract ARB
                val parser = XblConfigParser()
                val result = parser.parse(decompressedData)
                
                if (result != null) {
                    Log.i(TAG, "Obtained valid ARB result from $partitionName: $result")
                    return result
                } else {
                    Log.w(TAG, "Failed to parse ARB data from $partitionName, trying next candidate...")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error while processing partition $partitionName: ${e.message}")
            }
        }

        Log.w(TAG, "No valid ARB metadata found in candidate partitions: $CANDIDATE_PARTITIONS")
        return null
    }
}
