package com.abhinav.otapulse.catalog.provider.realme

import com.abhinav.otapulse.catalog.provider.DeviceProvider
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.core.model.RegionVariant

class RealmeP4x_5G : DeviceProvider {

    // Base list of regional variants to avoid duplication.
    // The triple contains: Display Name, Product Model, and the base part of the firmware string.
    private val baseVariants = listOf(
        Triple("IN", "RMX5108IN", "RMX5108NV1B"),
    )

    // Helper function to generate the full list of variants for a specific version letter (e.g., "A").
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
                name = "realme P4x 5G",
                ruiVersion = 6,
                imageUrl = "https://placehold.co/100x100/E3F2FD/1565C0?text=P4x_5G",
                firmwareGroups = mapOf(
                    "Android 15" to generateRegionalVariants("A"),
                    "Android 16" to generateRegionalVariants("C")
                )
            )
        )
    }
}
