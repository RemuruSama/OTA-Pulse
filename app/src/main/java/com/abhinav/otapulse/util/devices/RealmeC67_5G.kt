package com.abhinav.otapulse.util.devices

import com.abhinav.otapulse.util.DeviceProvider
import com.abhinav.otapulse.util.PredefinedDevice
import com.abhinav.otapulse.util.RegionVariant

class RealmeC67_5G : DeviceProvider {

    // Base list of regional variants to avoid duplication.
    // The triple contains: Display Name, Product Model, and the base part of the firmware string.
    private val baseVariants = listOf(
        Triple("IN", "RMX3782", "RMX3782NV1B"),
    )

    // Helper function to generate the full list of variants for a specific version letter (e.g., "C", "F").
    private fun generateRegionalVariants(versionLetter: String): List<RegionVariant> {
        return baseVariants.map { (displayName, productModel, firmwareBase) ->
            val region = when (displayName) {
                "IN" -> "IN"
                else -> "GL"
            }
            RegionVariant(
                displayName = displayName,
                productModel = productModel,
                firmwareVersion = "${firmwareBase}_11.${versionLetter}.01_0001_100001010000",
                region = region
            )
        }
    }

    override fun getDevices(): List<PredefinedDevice> {
        return listOf(
            PredefinedDevice(
                name = "realme C67 5G",
                ruiVersion = 6, // Use the latest ruiVersion
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=C67_5G",
                firmwareGroups = mapOf(
                    "Android 14" to generateRegionalVariants("C"),
                    "Android 15" to generateRegionalVariants("F")
                )
            )
        )
    }
}