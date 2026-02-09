package com.abhinav.otapulse.util.devices

import com.abhinav.otapulse.util.DeviceProvider
import com.abhinav.otapulse.util.PredefinedDevice
import com.abhinav.otapulse.util.RegionVariant

class OnePlusAce6 : DeviceProvider {

    // Base list of regional variants to avoid duplication.
    private val baseVariants = listOf(
        Triple("CN", "PLR110", "PLR110NV97")
    )

    // Helper function to generate the full list of variants for a specific version letter.
    private fun generateRegionalVariants(versionLetter: String): List<RegionVariant> {
        return baseVariants.map { (displayName, productModel, firmwareBase) ->
            val region = when (displayName) {
                "IN" -> "IN"
                "CN" -> "CN"
                "EU" -> "EU"
                else -> "GL"
            }
            RegionVariant(
                displayName = displayName,
                productModel = productModel,
                // Assumed firmware pattern, adjust if OnePlus changes naming conventions
                firmwareVersion = "${firmwareBase}_11.${versionLetter}.43_0001_202601080207",
                region = region
            )
        }
    }

    override fun getDevices(): List<PredefinedDevice> {
        return listOf(
            PredefinedDevice(
                name = "OnePlus Ace 6",
                ruiVersion = 4, // Incrementing RUI version based on device generation gap
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=OP15",
                firmwareGroups = mapOf(
                    "Android 16" to generateRegionalVariants("A")
                )
            )
        )
    }
}