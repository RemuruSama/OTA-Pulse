package com.abhinav.otapulse.catalog.provider.realme

import com.abhinav.otapulse.catalog.provider.DeviceProvider
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.core.model.RegionVariant

class RealmeNarzoN55 : DeviceProvider {

    // Base list of regional variants to avoid duplication.
    // The triple contains: Display Name, Product Model, and the base part of the firmware string.
    private val baseVariants = listOf(
        Triple("TR", "RMX3710", "RMX3710NV51"),
        Triple("RU", "RMX3710", "RMX3710NV37"),
        Triple("MEA", "RMX3710", "RMX3710NVA6"),
        Triple("SA", "RMX3710", "RMX3710NV83"),
        Triple("ID", "RMX3710", "RMX3710NV33"),
        Triple("IN", "RMX3710", "RMX3710NV1B"),
        Triple("EU", "RMX3710", "RMX3710NV44"),
        Triple("LATAM", "RMX3710", "RMX3710NV9A"),
        Triple("BR", "RMX3710", "RMX3710NV9E"),
    )

    // Helper function to generate the full list of variants for a specific version letter (e.g., "A", "C", "F").
    private fun generateRegionalVariants(versionLetter: String): List<RegionVariant> {
        return baseVariants.map { (displayName, productModel, firmwareBase) ->
            val region = when (displayName) {
                "EU", "RU", "MEA", "SA", "IN", "ID" -> "EU"
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
                name = "realme Narzo N55",
                ruiVersion = 5, // Use the latest ruiVersion
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=RN55",
                firmwareGroups = mapOf(
                    "Android 13" to generateRegionalVariants("A"),
                    "Android 14" to generateRegionalVariants("C"),
                    "Android 15" to generateRegionalVariants("F")
                )
            )
        )
    }
}
