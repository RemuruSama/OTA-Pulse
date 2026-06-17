package com.abhinav.otapulse.catalog.repository

import android.content.SharedPreferences
import com.abhinav.otapulse.di.CustomDevicesPrefs
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.abhinav.otapulse.core.model.RegionVariant
import javax.inject.Inject
import javax.inject.Singleton

data class CustomDeviceImportResult(
    val importedCount: Int,
    val skippedCount: Int
)

@Singleton
class CustomDeviceManager @Inject constructor(
    @CustomDevicesPrefs private val sharedPreferences: SharedPreferences,
    private val gson: Gson
) {
    private val KEY_CUSTOM_DEVICES = "custom_devices"

    fun getCustomDevices(): List<PredefinedDevice> {
        val json = getCustomDevicesAsJson()
        return if (json.isNotEmpty()) {
            try {
                parseDevices(json).devices
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    /**
     * Returns the raw JSON string of custom devices for exporting.
     */
    fun getCustomDevicesAsJson(): String {
        return sharedPreferences.getString(KEY_CUSTOM_DEVICES, "") ?: ""
    }

    fun addDevice(device: PredefinedDevice) {
        val currentDevices = getCustomDevices().toMutableList()
        // Add the isCustom flag when adding a new device
        if (!currentDevices.any { it.name.equals(device.name, ignoreCase = true) }) {
            currentDevices.add(device.copy(isCustom = true))
        }
        val json = gson.toJson(currentDevices)
        sharedPreferences.edit().putString(KEY_CUSTOM_DEVICES, json).apply()
    }

    fun updateDevice(oldName: String, newDevice: PredefinedDevice) {
        val currentDevices = getCustomDevices().toMutableList()
        val index = currentDevices.indexOfFirst { it.name.equals(oldName, ignoreCase = true) }
        if (index != -1) {
            currentDevices[index] = newDevice.copy(isCustom = true)
            val json = gson.toJson(currentDevices)
            sharedPreferences.edit().putString(KEY_CUSTOM_DEVICES, json).apply()
        }
    }

    fun deleteDevice(deviceName: String) {
        val currentDevices = getCustomDevices().toMutableList()
        currentDevices.removeAll { it.name.equals(deviceName, ignoreCase = true) }
        val json = gson.toJson(currentDevices)
        sharedPreferences.edit().putString(KEY_CUSTOM_DEVICES, json).apply()
    }

    /**
     * Overwrites the current list of custom devices with data from a JSON string.
     * Returns true if successful, false otherwise.
     */
    fun overwriteDevicesFromJson(json: String): CustomDeviceImportResult {
        return try {
            val parseResult = parseDevices(json)
            sharedPreferences.edit()
                .putString(KEY_CUSTOM_DEVICES, gson.toJson(parseResult.devices))
                .apply()
            CustomDeviceImportResult(
                importedCount = parseResult.devices.size,
                skippedCount = parseResult.skippedCount
            )
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid file format.", e)
        }
    }

    private fun parseDevices(json: String): ParsedDevicesResult {
        val root = JsonParser.parseString(json)
        if (!root.isJsonArray) throw IllegalArgumentException("Expected a JSON array.")

        val devices = root.asJsonArray.mapNotNull { element ->
            element.asJsonObjectOrNull()?.toPredefinedDeviceOrNull()
        }
        return ParsedDevicesResult(
            devices = devices,
            skippedCount = root.asJsonArray.size() - devices.size
        )
    }

    private fun JsonObject.toPredefinedDeviceOrNull(): PredefinedDevice? {
        val name = getString("name")?.takeIf { it.isNotBlank() } ?: return null
        val ruiVersion = getInt("ruiVersion") ?: return null
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
            isCustom = getBoolean("isCustom") ?: true
        )
    }

    private fun JsonElement.toRegionVariants(): List<RegionVariant> {
        if (!isJsonArray) return emptyList()

        return asJsonArray.mapNotNull { variantElement ->
            val variant = variantElement.asJsonObjectOrNull() ?: return@mapNotNull null
            val displayName = variant.getString("displayName")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val productModel = variant.getString("productModel")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val firmwareVersion = variant.getString("firmwareVersion")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val region = variant.getString("region")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null

            RegionVariant(
                displayName = displayName,
                productModel = productModel,
                firmwareVersion = firmwareVersion,
                region = region,
                nvId = variant.getString("nvId"),
                language = variant.getString("language"),
                reqMode = variant.getString("reqMode") ?: "manual",
                gray = variant.getInt("gray") ?: 0
            )
        }
    }

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

    private data class ParsedDevicesResult(
        val devices: List<PredefinedDevice>,
        val skippedCount: Int
    )
}
