package com.abhinav.otapulse.catalog.provider.realme

import com.abhinav.otapulse.catalog.provider.DeviceProvider
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.core.model.RegionVariant

class RealmeGT5G : DeviceProvider {

    // Base list of regional variants to avoid duplication
    // The triple contains: Display Name, Product Model, and the base part of the firmware string
    private val baseVariants = listOf(
        Triple("TW", "RMX2202", "RMX2202NV1A"), //
        Triple("RU", "RMX2202RU", "RMX2202NV37"), //
        Triple("IN", "RMX2202", "RMX2202NV1B"), //
        Triple("EU", "RMX2202EEA", "RMX2202NV44"), //
        Triple("TH", "RMX2202", "RMX2202NV39"), //
        Triple("CN", "RMX2202", "RMX2202NV97") //
    )

    // Helper function to generate the full list of variants for a specific version letter (e.g., "A", "C", "F", "H")
    private fun generateRegionalVariants(versionLetter: String): List<RegionVariant> {
        return baseVariants.map { (displayName, productModel, firmwareBase) ->
            val region = when (displayName) { //
                "IN" -> "IN" //
                "CN" -> "CN" //
                "EU", "RU", "TH" -> "EU" //
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
                name = "realme GT 5G", //
                ruiVersion = 5, // Use the latest ruiVersion
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=RGT5G", //
                firmwareGroups = mapOf(
                    "Android 11" to generateRegionalVariants("A"), //
                    "Android 12" to generateRegionalVariants("C"), //
                    "Android 13" to generateRegionalVariants("F"), //
                    "Android 14" to generateRegionalVariants("H") //
                )
            )
        )
    }
}
