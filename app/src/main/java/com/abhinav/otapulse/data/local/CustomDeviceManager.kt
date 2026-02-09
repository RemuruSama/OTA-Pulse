package com.abhinav.otapulse.data.local

import android.content.SharedPreferences
import com.abhinav.otapulse.di.CustomDevicesPrefs
import com.abhinav.otapulse.util.PredefinedDevice
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject
import javax.inject.Singleton

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
                val type = object : TypeToken<List<PredefinedDevice>>() {}.type
                gson.fromJson(json, type)
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
        sharedPreferences.edit().putString(KEY_CUSTOM_DEVICES, json).commit()
    }

    fun updateDevice(oldName: String, newDevice: PredefinedDevice) {
        val currentDevices = getCustomDevices().toMutableList()
        val index = currentDevices.indexOfFirst { it.name.equals(oldName, ignoreCase = true) }
        if (index != -1) {
            currentDevices[index] = newDevice.copy(isCustom = true)
            val json = gson.toJson(currentDevices)
            sharedPreferences.edit().putString(KEY_CUSTOM_DEVICES, json).commit()
        }
    }

    fun deleteDevice(deviceName: String) {
        val currentDevices = getCustomDevices().toMutableList()
        currentDevices.removeAll { it.name.equals(deviceName, ignoreCase = true) }
        val json = gson.toJson(currentDevices)
        sharedPreferences.edit().putString(KEY_CUSTOM_DEVICES, json).commit()
    }

    /**
     * Overwrites the current list of custom devices with data from a JSON string.
     * Returns true if successful, false otherwise.
     */
    fun overwriteDevicesFromJson(json: String): Boolean {
        return try {
            val type = object : TypeToken<List<PredefinedDevice>>() {}.type
            // This line validates that the JSON is in the correct format
            gson.fromJson<List<PredefinedDevice>>(json, type)
            // If validation passes, save the new JSON string
            sharedPreferences.edit().putString(KEY_CUSTOM_DEVICES, json).commit()
            true
        } catch (e: Exception) {
            false
        }
    }
}