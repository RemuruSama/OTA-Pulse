package com.abhinav.otapulse.util.devices

import com.abhinav.otapulse.util.DeviceProvider
import com.abhinav.otapulse.util.PredefinedDevice
import com.abhinav.otapulse.util.RegionVariant

class RealmeNarzo50 : DeviceProvider {

    // Base list of regional variants to avoid duplication.
    // The triple contains: Display Name, Product Model, and the base part of the firmware string.
    private val baseVariants = listOf(
        Triple("TW", "RMX3286", "RMX3286NV1A"),
        Triple("RU", "RMX3286", "RMX3286NV37"),
        Triple("MEA", "RMX3286", "RMX3286NVA6"),
        Triple("SA", "RMX3286", "RMX3286NV83"),
        Triple("ID", "RMX3286", "RMX3286NV33"),
        Triple("IN", "RMX3286", "RMX3286NV1B"),
        Triple("EU", "RMX3286", "RMX3286NV44"),
        Triple("TH", "RMX3286", "RMX3286NV39"),
        Triple("LATAM", "RMX3286", "RMX3286NV9A"),
    )

    // Helper function to generate the full list of variants for a specific version letter (e.g., "F").
    private fun generateRegionalVariants(versionLetter: String): List<RegionVariant> {
        return baseVariants.map { (displayName, productModel, firmwareBase) ->
            val region = when (displayName) {
                "EU", "RU", "MEA", "SA", "TH", "IN", "ID" -> "EU"
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
                name = "realme Narzo 50",
                ruiVersion = 4, // Use the latest ruiVersion
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=RN50",
                firmwareGroups = mapOf(
                    "Android 13" to generateRegionalVariants("F")
                )
            )
        )
    }
}