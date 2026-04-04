package com.abhinav.otapulse.catalog.provider.realme

import com.abhinav.otapulse.catalog.provider.DeviceProvider
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.core.model.RegionVariant

class Realme9_4G : DeviceProvider {

    // Base list of regional variants to avoid duplication.
    // The triple contains: Display Name, Product Model, and the base part of the firmware string.
    private val baseVariants = listOf(
        Triple("TR", "RMX3521", "RMX3521NV51"),
        Triple("RU", "RMX3521", "RMX3521NV37"),
        Triple("MEA", "RMX3521", "RMX3521NVA6"),
        Triple("MY", "RMX3521", "RMX3521NV38"),
        Triple("SA", "RMX3521", "RMX3521NV83"),
        Triple("ID", "RMX3521", "RMX3521NV33"),
        Triple("IN", "RMX3521", "RMX3521NV1B"),
        Triple("EU", "RMX3521", "RMX3521NV44"),
        Triple("TH", "RMX3521", "RMX3521NV39"),
        Triple("LATAM", "RMX3521", "RMX3521NV9A"),
        Triple("BR", "RMX3521", "RMX3521NV9E"),
    )

    // Helper function to generate the full list of variants for a specific version letter (e.g., "C", "F").
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
                name = "realme 9 4G",
                ruiVersion = 5, // Use the latest ruiVersion
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=R9_4G",
                firmwareGroups = mapOf(
                    "Android 13" to generateRegionalVariants("C"),
                    "Android 14" to generateRegionalVariants("F")
                )
            )
        )
    }
}
