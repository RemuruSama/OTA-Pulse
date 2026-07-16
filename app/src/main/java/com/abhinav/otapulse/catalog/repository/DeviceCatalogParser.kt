package com.abhinav.otapulse.catalog.repository

import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.core.model.RegionVariant
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Shared parser for device catalog JSON.
 *
 * Supports a simplified JSON format where only required fields are needed:
 *
 * **Device-level required**: `name`, `ruiVersion`, `firmwareGroups`
 * **Device-level optional** (with defaults): `imei`(""), `beta`(false),
 *   `isFavorite`(false), `isLoadingDetails`(false), `isCustom`(false)
 *
 * **Variant-level required**: `displayName`, `productModel`, `productName`
 * **Variant-level optional** (with defaults): `versionLetter`("A"), `reqMode`("manual"), `gray`(0),
 *   `nvId`(null), `language`(null)
 */
object DeviceCatalogParser {

    data class ParseResult(
        val devices: List<PredefinedDevice>,
        val skippedCount: Int
    )

    /**
     * Parses a JSON array string into a list of [PredefinedDevice].
     *
     * @param json        The JSON array string.
     * @param isCustom    Default value for the `isCustom` flag when not present in JSON.
     *                    Use `true` for user-imported devices, `false` for catalog devices.
     */
    fun parseDevices(json: String, isCustom: Boolean = false): ParseResult {
        val root = JsonParser.parseString(json)
        if (!root.isJsonArray) throw IllegalArgumentException("Expected a JSON array.")

        val devices = root.asJsonArray.mapNotNull { element ->
            element.asJsonObjectOrNull()?.toPredefinedDeviceOrNull(isCustom)
        }
        return ParseResult(
            devices = devices,
            skippedCount = root.asJsonArray.size() - devices.size
        )
    }

    private fun JsonObject.toPredefinedDeviceOrNull(defaultIsCustom: Boolean): PredefinedDevice? {
        val name = getString("name")?.takeIf { it.isNotBlank() } ?: return null
        val ruiVersion = getInt("ruiVersion") ?: 6
        val firmwareGroups = getAsJsonObject("firmwareGroups")
            ?.entrySet()
            ?.associate { (groupName, variantsElement) ->
                groupName to variantsElement.toRegionVariants()
            }
            ?.filterValues { it.isNotEmpty() }
            .orEmpty()

        if (firmwareGroups.isEmpty()) return null

        return PredefinedDevice(
            name = name,
            ruiVersion = ruiVersion,
            imei = getString("imei").orEmpty(),
            beta = getBoolean("beta") ?: false,
            imageResId = getInt("imageResId"),
            firmwareGroups = firmwareGroups,
            isFavorite = getBoolean("isFavorite") ?: false,
            isLoadingDetails = getBoolean("isLoadingDetails") ?: false,
            isCustom = getBoolean("isCustom") ?: defaultIsCustom
        )
    }

    private fun JsonElement.toRegionVariants(): List<RegionVariant> {
        if (!isJsonArray) return emptyList()

        return asJsonArray.mapNotNull { variantElement ->
            val variant = variantElement.asJsonObjectOrNull() ?: return@mapNotNull null
            val displayName = variant.getString("displayName")
                ?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            
            // Allow parsing legacy fields if they exist, but fallback to new schema
            val productName = variant.getString("productName")
                ?: variant.getString("productModel") // Fallback for old format
                ?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                
            val productModel = variant.getString("productModel")
                ?.takeIf { it.isNotBlank() } ?: productName // Fallback for old format
            
            val versionLetter = variant.getString("versionLetter") ?: "A"
            
            val regionData = com.abhinav.otapulse.catalog.model.RegionData.regions.find {
                it.displayName.equals(displayName, ignoreCase = true) ||
                (displayName.contains("Genshin", ignoreCase = true) && it.displayName == "Genshin Impact")
            }
            val fallbackRegion = regionData?.serverCode ?: "EU"
            val nvid = regionData?.nvid ?: variant.getString("nvId") ?: ""
            
            val region = variant.getString("region")
                ?.takeIf { it.isNotBlank() } ?: fallbackRegion
                
            val firmwareVersion = variant.getString("firmwareVersion")
                ?.takeIf { it.isNotBlank() } ?: "${productModel}${nvid}_11.${versionLetter}.01_0001_100001010000"

            RegionVariant(
                displayName = displayName,
                productModel = productModel,
                productName = productName,
                firmwareVersion = firmwareVersion,
                region = region,
                nvId = variant.getString("nvId") ?: regionData?.nvid,
                language = variant.getString("language"),
                reqMode = variant.getString("reqMode") ?: "manual",
                gray = variant.getInt("gray") ?: 0
            )
        }
    }

    // ── JSON helpers ──────────────────────────────────────────────

    private fun JsonElement.asJsonObjectOrNull(): JsonObject? =
        if (isJsonObject) asJsonObject else null

    private fun JsonObject.getString(key: String): String? {
        val element = get(key) ?: return null
        return if (element.isJsonNull) null else element.asString
    }

    private fun JsonObject.getInt(key: String): Int? {
        val element = get(key) ?: return null
        return if (element.isJsonNull) null else runCatching { element.asInt }.getOrNull()
    }

    private fun JsonObject.getBoolean(key: String): Boolean? {
        val element = get(key) ?: return null
        return if (element.isJsonNull) null else runCatching { element.asBoolean }.getOrNull()
    }
}
