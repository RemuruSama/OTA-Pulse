package com.abhinav.otapulse.catalog.provider.realme

import com.abhinav.otapulse.catalog.provider.DeviceProvider
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.core.model.RegionVariant

class Realme8 : DeviceProvider {

    // Base list of regional variants to avoid duplication.
    // The triple contains: Display Name, Product Model, and the base part of the firmware string.
    private val baseVariants = listOf(
        Triple("TR", "RMX3085", "RMX3085NV51"),
        Triple("RU", "RMX3085", "RMX3085NV37"),
        Triple("MEA", "RMX3085", "RMX3085NVA6"),
        Triple("SA", "RMX3085", "RMX3085NV83"),
        Triple("ID", "RMX3085", "RMX3085NV33"),
        Triple("IN", "RMX3085", "RMX3085NV1B"),
        Triple("EU", "RMX3085", "RMX3085NV44"),
        Triple("TH", "RMX3085", "RMX3085NV39")
    )

    // Helper function to generate the full list of variants for a specific version letter (e.g., "F").
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
                name = "realme 8",
                ruiVersion = 4, // Use the latest ruiVersion
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=R8",
                firmwareGroups = mapOf(
                    "Android 13" to generateRegionalVariants("F")
                )
            )
        )
    }
}
