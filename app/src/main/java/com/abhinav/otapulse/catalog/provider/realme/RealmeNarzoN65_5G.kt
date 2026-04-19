package com.abhinav.otapulse.catalog.provider.realme

import com.abhinav.otapulse.catalog.provider.DeviceProvider
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.core.model.RegionVariant

class RealmeNarzoN65_5G : DeviceProvider {

    // Base list of regional variants to avoid duplication.
    // The triple contains: Display Name, Product Model, and the base part of the firmware string.
    private val baseVariants = listOf(
        Triple("TW", "RMX3997", "RMX3997NV1A"),
        Triple("MEA", "RMX3997", "RMX3997NVA6"),
        Triple("MY", "RMX3997", "RMX3997NV38"),
        Triple("SA", "RMX3997", "RMX3997NV83"),
        Triple("ID", "RMX3997", "RMX3997NV33"),
        Triple("IN", "RMX3997IN", "RMX3997NV1B"),
        Triple("EU", "RMX3997EEA", "RMX3997NV44"),
        Triple("TH", "RMX3997", "RMX3997NV39"),
        Triple("LATAM", "RMX3997", "RMX3997NV9A"),
        Triple("BR", "RMX3997", "RMX3997NV9E"),
    )

    // Helper function to generate the full list of variants for a specific version letter (e.g., "A", "C", "F").
    private fun generateRegionalVariants(versionLetter: String): List<RegionVariant> {
        return baseVariants.map { (displayName, productModel, firmwareBase) ->
            val region = when (displayName) {
                "IN" -> "EU"
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
                name = "realme NARZO N65 5G",
                ruiVersion = 5, // Use the latest ruiVersion
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=N65_5G",
                firmwareGroups = mapOf(
                    "Android 14" to generateRegionalVariants("A"),
                    "Android 15" to generateRegionalVariants("C"),
                    "Android 16" to generateRegionalVariants("F")
                )
            )
        )
    }
}
