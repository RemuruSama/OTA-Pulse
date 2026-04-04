package com.abhinav.otapulse.catalog.provider.realme

import com.abhinav.otapulse.catalog.provider.DeviceProvider
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.core.model.RegionVariant

class Realme11ProPlus_5G : DeviceProvider {

    // Base list of regional variants to avoid duplication
    // The triple contains: Display Name, Product Model, and the base part of the firmware string
    private val baseVariants = listOf(
        Triple("TW", "RMX3741", "RMX3741NV1A"),
        Triple("TR", "RMX3741", "RMX3741NV51"),
        Triple("RU", "RMX3741", "RMX3741NV37"),
        Triple("MEA", "RMX3741", "RMX3741NVA6"),
        Triple("MY", "RMX3741", "RMX3741NV38"),
        Triple("SA", "RMX3741", "RMX3741NV83"),
        Triple("ID", "RMX3741", "RMX3741NV33"),
        Triple("IN", "RMX3741", "RMX3741NV1B"),
        Triple("EU", "RMX3741", "RMX3741NV44"),
        Triple("TH", "RMX3741", "RMX3741NV39"),
        Triple("LATAM", "RMX3741", "RMX3741NV9A"),
        Triple("BR", "RMX3741", "RMX3741NV9E"),
        Triple("CN", "RMX3740", "RMX3740NV97")
    )

    // Helper function to generate the full list of variants for a specific version letter (e.g., "A", "C", "F")
    private fun generateRegionalVariants(versionLetter: String): List<RegionVariant> {
        return baseVariants.map { (displayName, productModel, firmwareBase) ->
            val region = when (displayName) {
                "IN" -> "IN"
                "CN" -> "CN"
                "EU", "RU", "MEA", "SA", "TH", "MY", "ID" -> "EU"
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
                name = "realme 11 Pro+ 5G",
                ruiVersion = 6, // Use the latest ruiVersion
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=11PP_5G",
                firmwareGroups = mapOf(
                    "Android 13" to generateRegionalVariants("A"),
                    "Android 14" to generateRegionalVariants("C"),
                    "Android 15" to generateRegionalVariants("F")
                )
            )
        )
    }
}
