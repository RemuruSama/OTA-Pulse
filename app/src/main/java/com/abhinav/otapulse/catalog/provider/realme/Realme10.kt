package com.abhinav.otapulse.catalog.provider.realme

import com.abhinav.otapulse.catalog.provider.DeviceProvider
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.core.model.RegionVariant

class Realme10 : DeviceProvider {

    // Base list of regional variants to avoid duplication.
    // The triple contains: Display Name, Product Model, and the base part of the firmware string.
    private val baseVariants = listOf(
        Triple("TR", "RMX3630", "RMX3630NV51"),
        Triple("RU", "RMX3630", "RMX3630NV37"),
        Triple("MEA", "RMX3630", "RMX3630NVA6"),
        Triple("MY", "RMX3630", "RMX3630NV38"),
        Triple("SA", "RMX3630", "RMX3630NV83"),
        Triple("ID", "RMX3630", "RMX3630NV33"),
        Triple("IN", "RMX3630", "RMX3630NV1B"),
        Triple("EU", "RMX3630", "RMX3630NV44"),
        Triple("TH", "RMX3630", "RMX3630NV39"),
        Triple("LATAM", "RMX3630", "RMX3630NV9A"),
        Triple("BR", "RMX3630", "RMX3630NV9E"),
    )

    // Helper function to generate the full list of variants for a specific version letter (e.g., "A", "C", "F").
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
                name = "realme 10",
                ruiVersion = 5, // Use the latest ruiVersion
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=R10",
                firmwareGroups = mapOf(
                    "Android 12" to generateRegionalVariants("A"),
                    "Android 13" to generateRegionalVariants("C"),
                    "Android 14" to generateRegionalVariants("F")
                )
            )
        )
    }
}
