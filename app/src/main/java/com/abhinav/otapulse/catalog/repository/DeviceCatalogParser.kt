package com.abhinav.otapulse.catalog.repository

import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.catalog.model.RegionData
import com.abhinav.otapulse.core.model.RegionVariant
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Shared parser for device catalog JSON.
 *
 * Supports both standard and smart compact JSON formats:
 *
 * **Device-level**:
 * - `name`: required string
 * - `ruiVersion`: optional int (auto-derived from firmwareGroups when omitted)
 * - `variants`: optional base list of [RegionVariant] objects shared across groups
 * - `firmwareGroups`: required map or array of firmware groups
 * - `imei` (""), `beta` (false), `isFavorite` (false), `isCustom` (false)
 *
 * **Variant-level**:
 * - `displayName` (aliases: `displayName`, `region`, `code`, `name`): required
 * - `productModel` (aliases: `productModel`, `model`, `productName`): required
 * - `productName`: optional (defaults to `productModel`)
 * - `versionLetter`: optional (defaults to group letter or "A")
 * - `reqMode`: optional (defaults to group reqMode or "manual")
 * - `region`, `nvId`, `firmwareVersion`: optional (auto-computed from region & model)
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

        val baseVariants = getAsJsonArray("variants")?.toRegionVariants()

        val rawFirmwareGroups = get("firmwareGroups")
        val firmwareGroups: Map<String, List<RegionVariant>> = when {
            rawFirmwareGroups == null -> emptyMap()
            rawFirmwareGroups.isJsonObject -> {
                rawFirmwareGroups.asJsonObject.entrySet().associate { (groupName, groupElement) ->
                    val variants = when {
                        groupElement.isJsonArray -> groupElement.toRegionVariants()
                        groupElement.isJsonPrimitive && groupElement.asJsonPrimitive.isString -> {
                            val versionLetter = groupElement.asString
                            baseVariants?.map { it.withVersionLetter(versionLetter) } ?: emptyList()
                        }
                        groupElement.isJsonObject -> {
                            val groupObj = groupElement.asJsonObject
                            val versionLetter = groupObj.getString("versionLetter") ?: "A"
                            val reqMode = groupObj.getString("reqMode") ?: "manual"
                            baseVariants?.map { it.withOverrides(versionLetter, reqMode) } ?: emptyList()
                        }
                        else -> emptyList()
                    }
                    groupName to variants
                }.filterValues { it.isNotEmpty() }
            }
            rawFirmwareGroups.isJsonArray -> {
                // Array of group names applying base variants directly (e.g. ["Android 16"])
                rawFirmwareGroups.asJsonArray.mapNotNull { it.asStringOrNull() }
                    .associateWith { baseVariants.orEmpty() }
                    .filterValues { it.isNotEmpty() }
            }
            else -> emptyMap()
        }

        if (firmwareGroups.isEmpty()) return null

        val ruiVersion = getInt("ruiVersion") ?: deriveRuiVersion(firmwareGroups)

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

    private fun deriveRuiVersion(firmwareGroups: Map<String, List<RegionVariant>>): Int {
        val ruiRegex = Regex("""(?:Realme\s*UI|RUI)\s*(\d+)""", RegexOption.IGNORE_CASE)
        val cosRegex = Regex("""(?:ColorOS|COS)\s*(\d+)""", RegexOption.IGNORE_CASE)
        val androidRegex = Regex("""(?:Android)\s*(\d+)""", RegexOption.IGNORE_CASE)

        val versions = firmwareGroups.keys.mapNotNull { groupName ->
            ruiRegex.find(groupName)?.groupValues?.get(1)?.toIntOrNull()?.let { rui ->
                return@mapNotNull rui.coerceIn(1, 7)
            }
            cosRegex.find(groupName)?.groupValues?.get(1)?.toIntOrNull()?.let { cos ->
                return@mapNotNull when {
                    cos >= 16 -> 6
                    cos == 15 -> 6
                    cos == 14 -> 5
                    cos == 13 -> 4
                    cos == 12 -> 3
                    cos == 11 -> 2
                    cos <= 7 -> 1
                    else -> 6
                }
            }
            androidRegex.find(groupName)?.groupValues?.get(1)?.toIntOrNull()?.let { android ->
                return@mapNotNull when {
                    android >= 17 -> 7
                    android == 16 || android == 15 -> 6
                    android == 14 -> 5
                    android == 13 -> 4
                    android == 12 -> 3
                    android == 11 -> 2
                    android <= 10 -> 1
                    else -> 6
                }
            }
            Regex("""\d+""").find(groupName)?.value?.toIntOrNull()?.let { num ->
                when {
                    num in 1..7 -> num // Direct RUI version number (e.g. "4")
                    num >= 17 -> 7
                    num == 16 || num == 15 -> 6
                    num == 14 -> 5
                    num == 13 -> 4
                    num == 12 -> 3
                    num == 11 -> 2
                    num <= 10 -> 1
                    else -> 6
                }
            }
        }

        return versions.maxOrNull() ?: 6
    }

    private fun RegionVariant.withVersionLetter(newVersionLetter: String): RegionVariant {
        return copy(
            firmwareVersion = buildFirmwareVersion(productModel, nvId, newVersionLetter)
        )
    }

    private fun RegionVariant.withOverrides(newVersionLetter: String, newReqMode: String): RegionVariant {
        return copy(
            reqMode = newReqMode,
            firmwareVersion = buildFirmwareVersion(productModel, nvId, newVersionLetter)
        )
    }

    private fun buildFirmwareVersion(productModel: String, nvId: String?, versionLetter: String): String {
        val cleanNvId = nvId.orEmpty()
        return "${productModel}${cleanNvId}_11.${versionLetter}.01_0001_100001010000"
    }

    private fun JsonElement.toRegionVariants(): List<RegionVariant> {
        if (!isJsonArray) return emptyList()

        return asJsonArray.mapNotNull { variantElement ->
            val variant = variantElement.asJsonObjectOrNull() ?: return@mapNotNull null
            val displayName = variant.getString("displayName")
                ?: variant.getString("region")
                ?: variant.getString("code")
                ?: variant.getString("name")
                ?: return@mapNotNull null

            if (displayName.isBlank()) return@mapNotNull null

            val productModel = variant.getString("productModel")
                ?: variant.getString("model")
                ?: variant.getString("productName")
                ?: return@mapNotNull null

            if (productModel.isBlank()) return@mapNotNull null

            val productName = variant.getString("productName")
                ?.takeIf { it.isNotBlank() } ?: productModel

            val versionLetter = variant.getString("versionLetter") ?: "A"

            val regionData = RegionData.regions.find {
                it.displayName.equals(displayName, ignoreCase = true) ||
                (displayName.contains("Genshin", ignoreCase = true) && it.displayName == "Genshin Impact")
            }
            val fallbackRegion = regionData?.serverCode ?: "EU"
            val nvid = variant.getString("nvId") ?: regionData?.nvid

            val region = variant.getString("region")
                ?.takeIf { it.isNotBlank() } ?: fallbackRegion

            val firmwareVersion = variant.getString("firmwareVersion")
                ?.takeIf { it.isNotBlank() } ?: buildFirmwareVersion(productModel, nvid, versionLetter)

            RegionVariant(
                displayName = displayName,
                productModel = productModel,
                productName = productName,
                firmwareVersion = firmwareVersion,
                region = region,
                nvId = nvid,
                language = variant.getString("language"),
                reqMode = variant.getString("reqMode") ?: "manual",
                gray = variant.getInt("gray") ?: 0
            )
        }
    }

    // ── JSON helpers ──────────────────────────────────────────────

    private fun JsonElement.asJsonObjectOrNull(): JsonObject? =
        if (isJsonObject) asJsonObject else null

    private fun JsonElement.asStringOrNull(): String? =
        if (isJsonPrimitive && asJsonPrimitive.isString) asString else null

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

