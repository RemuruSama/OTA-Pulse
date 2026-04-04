package com.abhinav.otapulse.catalog.provider.realme

import com.abhinav.otapulse.catalog.provider.DeviceProvider
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.core.model.RegionVariant

class RealmeC25s : DeviceProvider {

    // Base list of regional variants to avoid duplication.
    // The triple contains: Display Name, Product Model, and the base part of the firmware string.
    private val baseVariants = listOf(
        Triple("IN", "RMX3197", "RMX3197NV1B"),
    )

    // Helper function to generate the full list of variants for a specific version letter (e.g., "A").
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
                name = "realme C25s",
                ruiVersion = 4, // Use the latest ruiVersion
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=C25s",
                firmwareGroups = mapOf(
                    "Android 13" to generateRegionalVariants("F")
                )
            )
        )
    }
}
