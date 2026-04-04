package com.abhinav.otapulse.catalog.provider.oneplus

import com.abhinav.otapulse.catalog.provider.DeviceProvider
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.core.model.RegionVariant

class OnePlus15 : DeviceProvider {

    // Base list of regional variants to avoid duplication.
    // TODO: Update Product Models and Firmware Base strings with actual OnePlus 15 data.
    private val baseVariants = listOf(
        Triple("IN", "CPH2745IN", "CPH2745NV1B"),
        Triple("GLO", "CPH2747", "CPH2747NVA7"),
        Triple("EU", "CPH2747EEA", "CPH2747NV44"),
        Triple("CN", "PLK110", "PLK110NV97")
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
                firmwareVersion = "${firmwareBase}_11.${versionLetter}.01_0001_100001010000",
                region = region
            )
        }
    }

    override fun getDevices(): List<PredefinedDevice> {
        return listOf(
            PredefinedDevice(
                name = "OnePlus 15",
                ruiVersion = 7, // Incrementing RUI version based on device generation gap
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=OP15",
                firmwareGroups = mapOf(
                    "Android 16" to generateRegionalVariants("A")
                )
            )
        )
    }
}
