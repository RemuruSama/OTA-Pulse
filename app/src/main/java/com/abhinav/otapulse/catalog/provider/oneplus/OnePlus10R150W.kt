package com.abhinav.otapulse.catalog.provider.oneplus

import com.abhinav.otapulse.catalog.provider.DeviceProvider
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.core.model.RegionVariant

class OnePlus10R150W : DeviceProvider {

    // Base list of regional variants to avoid duplication
    // The triple contains: Display Name, Product Model, and the base part of the firmware string
    private val baseVariants = listOf(
        Triple("IN", "CPH2411", "CPH2411NV1B") //
    )

    // Helper function to generate the full list of variants for a specific version letter (e.g., "C", "F")
    private fun generateRegionalVariants(versionLetter: String): List<RegionVariant> {
        return baseVariants.map { (displayName, productModel, firmwareBase) ->
            val region = when (displayName) { //
                "IN" -> "IN" //
                else -> "GL" //
            }
            RegionVariant(
                displayName = displayName, //
                productModel = productModel, //
                firmwareVersion = "${firmwareBase}_11.${versionLetter}.01_0001_100001010000", //
                region = region //
            )
        }
    }

    override fun getDevices(): List<PredefinedDevice> {
        return listOf(
            PredefinedDevice(
                name = "OnePlus 10R 150W", //
                ruiVersion = 5, // Use the latest ruiVersion
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=OP10R150W", //
                firmwareGroups = mapOf(
                    "Android 13" to generateRegionalVariants("C"), //
                    "Android 14" to generateRegionalVariants("F"), //
                    "Android 15" to generateRegionalVariants("F") //
                )
            )
        )
    }
}
