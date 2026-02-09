package com.abhinav.otapulse.util.devices

import com.abhinav.otapulse.util.DeviceProvider
import com.abhinav.otapulse.util.PredefinedDevice
import com.abhinav.otapulse.util.RegionVariant

class Realme9i : DeviceProvider {

    // Base list of regional variants to avoid duplication.
    // The triple contains: Display Name, Product Model, and the base part of the firmware string.
    private val baseVariants = listOf(
        Triple("TW", "RMX3491", "RMX3491NV1A"),
        Triple("TR", "RMX3491", "RMX3491NV51"),
        Triple("RU", "RMX3491", "RMX3491NV37"),
        Triple("MEA", "RMX3491", "RMX3491NVA6"),
        Triple("MY", "RMX3491", "RMX3491NV38"),
        Triple("SA", "RMX3491", "RMX3491NV83"),
        Triple("ID", "RMX3491", "RMX3491NV33"),
        Triple("IN", "RMX3491", "RMX3491NV1B"),
        Triple("EU", "RMX3491", "RMX3491NV44"),
        Triple("TH", "RMX3491", "RMX3491NV39"),
        Triple("LATAM", "RMX3491", "RMX3491NV9A"),
        Triple("BR", "RMX3491", "RMX3491NV9E"),
    )

    // Helper function to generate the full list of variants for a specific version letter (e.g., "F").
    private fun generateRegionalVariants(versionLetter: String): List<RegionVariant> {
        return baseVariants.map { (displayName, productModel, firmwareBase) ->
            val region = when (displayName) {
                "IN" -> "IN"
                "CN" -> "CN"
                "EU", "RU", "MEA", "SA", "TH", "MY", "ID" -> "EU"
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
                name = "realme 9i",
                ruiVersion = 4, // Use the latest ruiVersion
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=R9i",
                firmwareGroups = mapOf(
                    "Android 13" to generateRegionalVariants("F")
                )
            )
        )
    }
}