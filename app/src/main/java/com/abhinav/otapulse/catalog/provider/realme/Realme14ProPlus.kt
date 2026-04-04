package com.abhinav.otapulse.catalog.provider.realme

import com.abhinav.otapulse.catalog.provider.DeviceProvider
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.core.model.RegionVariant

class Realme14ProPlus : DeviceProvider {

    // Base list of regional variants to avoid duplication.
    // The triple contains: Display Name, Product Model, and the base part of the firmware string.
    private val baseVariants = listOf(
        Triple("TR", "RMX5051TR", "RMX5051NV51"),
        Triple("RU", "RMX5051RU", "RMX5051NV37"),
        Triple("MEA", "RMX5051", "RMX5051NVA6"),
        Triple("SA", "RMX5051", "RMX5051NV83"),
        Triple("IN", "RMX5051IN", "RMX5051NV1B"),
        Triple("EU", "RMX5051EEA", "RMX5051NV44"),
        Triple("TH", "RMX5051", "RMX5051NV39"),
        Triple("LATAM", "RMX5051", "RMX5051NV9A"),
        Triple("BR", "RMX5051", "RMX5051NV9E"),
        Triple("CN", "RMX5050", "RMX5050NV97")
    )

    // Helper function to generate the full list of variants for a specific version letter (e.g., "A").
    private fun generateRegionalVariants(versionLetter: String): List<RegionVariant> {
        return baseVariants.map { (displayName, productModel, firmwareBase) ->
            val region = when (displayName) {
                "IN" -> "EU"
                "CN" -> "CN"
                "RU", "MEA", "SA", "EU", "TH" -> "EU"
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
                name = "realme 14 Pro+ 5G",
                ruiVersion = 5, // Use the latest ruiVersion
                imageUrl = "https://static.c.realme.com/IN/index-category/569555261224050947.jpg",
                firmwareGroups = mapOf(
                    "Android 15" to generateRegionalVariants("A"),
                    "Android 16" to generateRegionalVariants("F")
                )
            )
        )
    }
}
