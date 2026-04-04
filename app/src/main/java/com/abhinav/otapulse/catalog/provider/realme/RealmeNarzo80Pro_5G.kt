package com.abhinav.otapulse.catalog.provider.realme

import com.abhinav.otapulse.catalog.provider.DeviceProvider
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.core.model.RegionVariant

class RealmeNarzo80Pro_5G : DeviceProvider {

    // Base list of regional variants to avoid duplication.
    // The triple contains: Display Name, Product Model, and the base part of the firmware string.
    private val baseVariants = listOf(
        Triple("IN", "RMX5033IN", "RMX5033NV1B"),
    )

    // Helper function to generate the full list of variants for a specific version letter (e.g., "C").
    private fun generateRegionalVariants(versionLetter: String): List<RegionVariant> {
        return baseVariants.map { (displayName, productModel, firmwareBase) ->
            val region = when (displayName) {
                "IN" -> "EU"
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
                name = "realme Narzo 80 Pro 5G",
                ruiVersion = 5, // Use the latest ruiVersion
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=RN80P_5G",
                firmwareGroups = mapOf(
                    "Android 15" to generateRegionalVariants("C"),
                    "Android 16" to generateRegionalVariants("F")
                )
            )
        )
    }
}
