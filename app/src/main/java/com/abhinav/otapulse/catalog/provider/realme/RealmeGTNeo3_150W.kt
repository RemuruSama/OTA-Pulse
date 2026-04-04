package com.abhinav.otapulse.catalog.provider.realme

import com.abhinav.otapulse.catalog.provider.DeviceProvider
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.core.model.RegionVariant

class RealmeGTNeo3_150W : DeviceProvider {

    // Base list of regional variants to avoid duplication
    // The triple contains: Display Name, Product Model, and the base part of the firmware string
    private val baseVariants = listOf(
        Triple("ID", "RMX3563", "RMX3563NV33"), //
        Triple("IN", "RMX3563", "RMX3563NV1B"), //
        Triple("EU", "RMX3563", "RMX3563NV44"), //
        Triple("CN", "RMX3562", "RMX3563NV97") //
    )

    // Helper function to generate the full list of variants for a specific version letter (e.g., "A", "C", "F")
    private fun generateRegionalVariants(versionLetter: String): List<RegionVariant> {
        return baseVariants.map { (displayName, productModel, firmwareBase) ->
            val region = when (displayName) { //
                "IN" -> "IN" //
                "CN" -> "CN" //
                "EU", "RU", "MEA", "SA", "TH", "MY", "ID" -> "EU" //
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
                name = "realme GT Neo 3 150W", //
                ruiVersion = 5, // Use the latest ruiVersion
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=RGTN3_150W", //
                firmwareGroups = mapOf(
                    "Android 12" to generateRegionalVariants("A"), //
                    "Android 13" to generateRegionalVariants("C"), //
                    "Android 14" to generateRegionalVariants("F") //
                )
            )
        )
    }
}
