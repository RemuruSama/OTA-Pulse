package com.abhinav.otapulse.util.devices

import com.abhinav.otapulse.util.DeviceProvider
import com.abhinav.otapulse.util.PredefinedDevice
import com.abhinav.otapulse.util.RegionVariant

class Realme11_5G : DeviceProvider {

    // Base list of regional variants to avoid duplication.
    // The triple contains: Display Name, Product Model, and the base part of the firmware string.
    private val baseVariants = listOf(
        Triple("TW", "RMX3780", "RMX3780NV1A"),
        Triple("MEA", "RMX3780", "RMX3780NVA6"),
        Triple("MY", "RMX3780", "RMX3780NV38"),
        Triple("SA", "RMX3780", "RMX3780NV83"),
        Triple("IN", "RMX3780IN", "RMX3780NV1B"),
        Triple("EU", "RMX3780", "RMX3780NV44"),
        Triple("TH", "RMX3780", "RMX3780NV39"),
        Triple("LATAM", "RMX3780", "RMX3780NV9A"),
        Triple("BR", "RMX3780", "RMX3780NV9E"),
    )

    // Helper function to generate the full list of variants for a specific version letter (e.g., "A", "C", "F").
    private fun generateRegionalVariants(versionLetter: String): List<RegionVariant> {
        return baseVariants.map { (displayName, productModel, firmwareBase) ->
            val region = when (displayName) {
                "EU", "RU", "MEA", "SA", "IN", "MY", "ID" -> "EU"
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
                name = "realme 11 5G",
                ruiVersion = 6, // Use the latest ruiVersion
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=11_5G",
                firmwareGroups = mapOf(
                    "Android 13" to generateRegionalVariants("A"),
                    "Android 14" to generateRegionalVariants("C"),
                    "Android 15" to generateRegionalVariants("F")
                )
            )
        )
    }
}