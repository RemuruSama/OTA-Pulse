package com.abhinav.otapulse.catalog.provider.realme

import com.abhinav.otapulse.catalog.provider.DeviceProvider
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.core.model.RegionVariant

class Realme8Pro : DeviceProvider {

    // Base list of regional variants to avoid duplication.
    // The triple contains: Display Name, Product Model, and the base part of the firmware string.
    private val baseVariants = listOf(
        Triple("RU", "RMX3081", "RMX3081NV37"),
        Triple("MEA", "RMX3081", "RMX3081NVA6"),
        Triple("MY", "RMX3081", "RMX3081NV38"),
        Triple("SA", "RMX3081", "RMX3081NV83"),
        Triple("ID", "RMX3081", "RMX3081NV33"),
        Triple("IN", "RMX3081", "RMX3081NV1B"),
        Triple("EU", "RMX3081", "RMX3081NV44"),
        Triple("LATAM", "RMX3081", "RMX3081NV9A"),
        Triple("BR", "RMX3081", "RMX3081NV9E"),
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
                name = "realme 8 Pro",
                ruiVersion = 4, // Use the latest ruiVersion
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=R8P",
                firmwareGroups = mapOf(
                    "Android 13" to generateRegionalVariants("F")
                )
            )
        )
    }
}
