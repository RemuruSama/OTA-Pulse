package com.abhinav.otapulse.catalog.repository

import android.content.SharedPreferences
import com.abhinav.otapulse.di.CustomDevicesPrefs
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.google.gson.Gson
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

    private fun parseDevices(json: String): DeviceCatalogParser.ParseResult {
        return DeviceCatalogParser.parseDevices(json, isCustom = true)
    }
}
