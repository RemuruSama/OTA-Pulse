/*
 * Copyright (C) 2026 OTA Pulse
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.abhinav.otapulse.core.common

import com.abhinav.otapulse.catalog.model.RegionData
import com.abhinav.otapulse.core.model.OtaUpdate

/**
 * Shared utility functions for constructing OTA query versions, resolving regional
 * search priority, and accurately comparing firmware build versions across generations.
 */
object OtaUtils {

    /**
     * Major OS generation letters for ColorOS / OxygenOS / Realme UI.
     * A: Android 12 (ColorOS 12)
     * C: Android 13 (ColorOS 13)
     * F: Android 14 (ColorOS 14)
     * H: Android 15 (ColorOS 15)
     * J: Android 16 (ColorOS 16)
     */
    val GENERATION_LETTERS = listOf("A", "C", "F", "H", "J")

    private val SUFFIXES_TO_STRIP = listOf(
        "EEA", "IN", "RU", "TR", "CN", "EU", "TW", "MEA", "SA",
        "SG", "TH", "LATAM", "BR", "MY", "ID", "KZ", "OCA", "VN", "GLO"
    ).distinct()

    private val BASE_SEARCH_ORDER = listOf("EU", "GL", "IN", "CN")

    private val LETTER_RANK_MAP = mapOf(
        'A' to 10,
        'B' to 15,
        'C' to 20,
        'D' to 25,
        'E' to 30,
        'F' to 40,
        'G' to 45,
        'H' to 50,
        'I' to 55,
        'J' to 60,
        'K' to 70
    )

    /**
     * Extracts the clean hardware base model string from a raw model name or identifier
     * by stripping regional suffixes (EEA, IN, etc.), version markers, and NV tags.
     */
    fun getBaseOtaModel(rawId: String): String {
        var baseModel = rawId.substringBefore("_11").substringBefore(".")
        for (suffix in SUFFIXES_TO_STRIP) {
            if (baseModel.endsWith(suffix, ignoreCase = true)) {
                baseModel = baseModel.dropLast(suffix.length)
                break
            }
        }
        return baseModel.replace(Regex("NV[0-9A-Z]{2}$", RegexOption.IGNORE_CASE), "")
    }

    /**
     * Extracts NV ID suffix if formatted as NVxx (4 characters, e.g. NV01, NV44).
     */
    fun getNvSuffix(nvId: String): String {
        val trimmed = nvId.trim()
        return if (trimmed.startsWith("NV", ignoreCase = true) && trimmed.length == 4) {
            trimmed.uppercase()
        } else {
            ""
        }
    }

    /**
     * Constructs a standard OTA firmware query string formatted as:
     * "${cleanBase}${nvSuffix}_11.${letter}.01_0001_100001010000"
     */
    fun constructOtaString(rawModel: String, nvId: String = "", letter: String = "A"): String {
        val cleanBase = getBaseOtaModel(rawModel)
        if (cleanBase.isBlank()) return ""
        val nvSuffix = getNvSuffix(nvId)
        val validLetter = letter.trim().ifBlank { "A" }.uppercase()
        return "${cleanBase}${nvSuffix}_11.${validLetter}.01_0001_100001010000"
    }

    /**
     * Infers the display region code (e.g. IN, CN, EU, GLO) from a device NV ID.
     */
    fun inferRegionFromNvId(nvId: String): String {
        val normalizedNvId = nvId.trim()
        val nvRegion = RegionData.regions.firstOrNull {
            it.nvid.equals(normalizedNvId, ignoreCase = true)
        }?.displayName
        return nvRegion ?: "GLO"
    }

    /**
     * Returns the prioritized server search order based on the user's detected/selected
     * region and NV identifier.
     */
    fun getServerSearchOrder(region: String, nvId: String = ""): List<String> {
        val baseOrder = when (region.trim().uppercase()) {
            "IN" -> listOf("IN", "GL", "EU", "CN")
            "CN" -> listOf("CN", "GL", "EU", "IN")
            "EU", "RU", "TR", "EEA" -> listOf("EU", "GL", "IN", "CN")
            else -> BASE_SEARCH_ORDER
        }

        return if (nvId.trim() == "10010111") {
            listOf("CN") + (baseOrder - "CN")
        } else {
            baseOrder
        }
    }

    /**
     * Returns major generation letters prioritized starting with the device's current
     * generation letter, followed by newer generations, then older generations.
     */
    fun prioritizeLetters(currentLetter: String): List<String> {
        val letter = currentLetter.trim().uppercase()
        if (letter !in GENERATION_LETTERS) {
            return GENERATION_LETTERS
        }
        val currentRank = LETTER_RANK_MAP[letter.firstOrNull()] ?: 0
        return GENERATION_LETTERS.sortedWith(
            compareBy(
                // Prioritize current and newer generations first
                { if ((LETTER_RANK_MAP[it.firstOrNull()] ?: 0) >= currentRank) 0 else 1 },
                // Then sort chronologically
                { LETTER_RANK_MAP[it.firstOrNull()] ?: 0 }
            )
        )
    }

    /**
     * Resolves the canonical OTA version string for comparison purposes.
     * Prefers [OtaUpdate.realOtaVersion] (exact match to ro.build.version.ota),
     * and falls back to componentVersion.
     */
    fun OtaUpdate.resolvedOtaVersion(): String =
        realOtaVersion
            ?: componentVersion.substringBefore(".")
                .let { base -> if (base.count { it == '_' } >= 3) base else componentVersion }

    /**
     * Accurately determines if [serverOtaVersion] is newer than [currentOtaVersion].
     *
     * Comparison logic:
     * 1. If strings match exactly, returns false (already up to date).
     * 2. Checks generation letters (A < C < F < H < J). If generation letters differ and are recognized,
     *    returns true if server letter rank is greater, false if smaller.
     * 3. If in the same generation:
     *    - Compares 12-digit build timestamps (YYYYMMDDHHMM).
     *    - Compares numeric build numbers (e.g. 54 > 50 in H.54 vs H.50).
     * 4. Checks [serverPublishedTime] vs [deviceBuildTime] if timestamps within version strings are absent.
     * 5. Returns false if server version is older or cannot be proven newer, preventing false notifications.
     */
    fun isServerVersionNewer(
        serverOtaVersion: String,
        currentOtaVersion: String,
        serverPublishedTime: Long = 0L,
        deviceBuildTime: Long = 0L
    ): Boolean {
        val server = serverOtaVersion.trim()
        val current = currentOtaVersion.trim()

        if (server.isBlank()) return false
        if (current.isBlank()) return true
        if (server.equals(current, ignoreCase = true)) return false

        val serverLetter = extractLetter(server)
        val currentLetter = extractLetter(current)

        if (serverLetter != null && currentLetter != null) {
            val serverRank = LETTER_RANK_MAP[serverLetter]
            val currentRank = LETTER_RANK_MAP[currentLetter]

            if (serverRank != null && currentRank != null && serverRank != currentRank) {
                return serverRank > currentRank
            }
        }

        // Compare 12-digit build timestamp in the last segment (YYYYMMDDHHMM)
        val serverTs = extractTimestamp(server)
        val currentTs = extractTimestamp(current)
        if (serverTs != null && currentTs != null && serverTs != currentTs) {
            return serverTs > currentTs
        }

        // Compare build number after letter (e.g. H.54 -> 54 vs H.50 -> 50)
        val serverBuildNum = extractBuildNumber(server)
        val currentBuildNum = extractBuildNumber(current)
        if (serverBuildNum != null && currentBuildNum != null && serverBuildNum != currentBuildNum) {
            return serverBuildNum > currentBuildNum
        }

        // Compare server published time vs device build time if available
        if (serverPublishedTime > 0L && deviceBuildTime > 0L) {
            if (serverPublishedTime > deviceBuildTime) return true
            if (serverPublishedTime < deviceBuildTime) return false
        }

        // Fallback: Check if server version contains dummy timestamp 100001010000 (never newer)
        if (server.contains("100001010000")) return false

        return false
    }

    private fun extractLetter(version: String): Char? {
        val patterns = listOf(
            Regex("""_11[._]([A-Za-z])\."""),
            Regex("""\b11[._]([A-Za-z])\."""),
            Regex("""(?:\.|\b)([A-Za-z])\.\d+""")
        )
        for (pattern in patterns) {
            val match = pattern.find(version)?.groupValues?.getOrNull(1)
            if (!match.isNullOrBlank()) {
                return match.first().uppercaseChar()
            }
        }
        return null
    }

    private fun extractTimestamp(version: String): Long? {
        val match = Regex("""(?:_|\.)(20\d{10})(?:$|[._])""").find(version)?.groupValues?.getOrNull(1)
        return match?.toLongOrNull()
    }

    private fun extractBuildNumber(version: String): Int? {
        val match = Regex("""[._][A-Za-z]\.(\d+)""").find(version)?.groupValues?.getOrNull(1)
        return match?.toIntOrNull()
    }
}
