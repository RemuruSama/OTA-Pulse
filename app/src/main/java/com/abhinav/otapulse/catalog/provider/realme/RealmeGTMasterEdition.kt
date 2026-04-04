package com.abhinav.otapulse.catalog.provider.realme

import com.abhinav.otapulse.catalog.provider.DeviceProvider
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.core.model.RegionVariant

class RealmeGTMasterEdition : DeviceProvider {

    // Base list of regional variants to avoid duplication
    // The triple contains: Display Name, Product Model, and the base part of the firmware string
    private val baseVariants = listOf(
        Triple("TW", "RMX3363", "RMX3363NV1A"),
        Triple("RU", "RMX3363", "RMX3363NV37"),
        Triple("MEA", "RMX3363", "RMX3363NVA6"),
        Triple("MY", "RMX3363", "RMX3363NV38"),
        Triple("SA", "RMX3363", "RMX3363NV83"),
        Triple("ID", "RMX3363", "RMX3363NV33"),
        Triple("IN", "RMX3360", "RMX3360NV1B"),
        Triple("EU", "RMX3363", "RMX3363NV44"),
        Triple("TH", "RMX3363", "RMX3363NV39"),
        Triple("LATAM", "RMX3363", "RMX3363NV9A"),
        Triple("BR", "RMX3363", "RMX3363NV9E"),
        Triple("CN", "RMX3366", "RMX3366NV97")
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
                name = "realme GT Master Edition",
                ruiVersion = 4, // Use the latest ruiVersion
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=RGTME",
                firmwareGroups = mapOf(
                    "Android 11" to generateRegionalVariants("A"),
                    "Android 12" to generateRegionalVariants("C"),
                    "Android 13" to generateRegionalVariants("F")
                )
            )
        )
    }
}
