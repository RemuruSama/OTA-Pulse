package com.abhinav.otapulse.catalog.provider.realme

import com.abhinav.otapulse.catalog.provider.DeviceProvider
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.core.model.RegionVariant

class Realme13ProPlus : DeviceProvider {

    // Base list of regional variants to avoid duplication.
    // The triple contains: Display Name, Product Model, and the base part of the firmware string.
    private val baseVariants = listOf(
        Triple("TW", "RMX3921", "RMX3921NV1A"),
        Triple("PH", "RMX3921", "RMX3921NV3E"),
        Triple("MY", "RMX3921", "RMX3921NV38"),
        Triple("ID", "RMX3921", "RMX3921NV33"),
        Triple("LATAM", "RMX3921", "RMX3921NV9A"),
        Triple("MEA", "RMX3921", "RMX3921NVA6"),
        Triple("IN", "RMX3921IN", "RMX3921NV1B"),
        Triple("CN", "RMX3920", "RMX3920NV97")
    )

    // Helper function to generate the full list of variants for a specific version letter (e.g., "A", "C").
    private fun generateRegionalVariants(versionLetter: String): List<RegionVariant> {
        return baseVariants.map { (displayName, productModel, firmwareBase) ->
            val region = when (displayName) {
                "IN" -> "EU"
                "CN" -> "CN"
                "MEA", "MY", "ID" -> "EU"
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
                name = "realme 13 Pro+ 5G",
                ruiVersion = 5, // Use the latest ruiVersion
                imageUrl = "https://static.c.realme.com/IN/index-category/290607060824114034.jpg",
                firmwareGroups = mapOf(
                    "Android 14" to generateRegionalVariants("A"),
                    "Android 15" to generateRegionalVariants("C"),
                    "Android 16" to generateRegionalVariants("F")
                )
            )
        )
    }
}
