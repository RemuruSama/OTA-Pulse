package com.abhinav.otapulse.catalog.provider.oneplus

import com.abhinav.otapulse.catalog.provider.DeviceProvider
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.core.model.RegionVariant

class OnePlusNordCE5 : DeviceProvider {

    // Base list of regional variants to avoid duplication.
    // The triple contains: Display Name, Product Model, and the base part of the firmware string.
    private val baseVariants = listOf(
        Triple("IN", "CPH2717IN", "CPH2717NV1B"),
        Triple("GLO", "CPH2719", "CPH2719NVA7"),
        Triple("EU", "CPH2719EEA", "CPH2719NV44")
    )

    // Helper function to generate the full list of variants for a specific version letter (e.g., "A", "C").
    private fun generateRegionalVariants(versionLetter: String): List<RegionVariant> {
        return baseVariants.map { (displayName, productModel, firmwareBase) ->
            val region = when (displayName) {
                "IN" -> "IN"
                "EU" -> "EU"
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
                name = "OnePlus Nord CE 5",
                ruiVersion = 5, // Use the latest ruiVersion
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=OPN4",
                firmwareGroups = mapOf(
                    "Android 15" to generateRegionalVariants("A"),
                    "Android 16" to generateRegionalVariants("C")
                )
            )
        )
    }
}
