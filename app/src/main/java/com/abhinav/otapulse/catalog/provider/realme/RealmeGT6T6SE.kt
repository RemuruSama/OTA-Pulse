package com.abhinav.otapulse.catalog.provider.realme

import com.abhinav.otapulse.catalog.provider.DeviceProvider
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.core.model.RegionVariant

class RealmeGT6T6SE : DeviceProvider {

    // Base list of regional variants to avoid duplication.
    // The triple contains: Display Name, Product Model, and the base part of the firmware string.
    private val baseVariants = listOf(
        Triple("TR", "RMX3853TR", "RMX3853NV51"),
        Triple("RU", "RMX3853RU", "RMX3853NV37"),
        Triple("MEA", "RMX3853", "RMX3853NVA6"),
        Triple("SA", "RMX3853", "RMX3853NV83"),
        Triple("IN", "RMX3853IN", "RMX3853NV1B"),
        Triple("EU", "RMX3853EEA", "RMX3853NV44"),
        Triple("TH", "RMX3853", "RMX3853NV39"),
        Triple("LATAM", "RMX3853", "RMX3853NV9A"),
        Triple("BR", "RMX3853", "RMX3853NV9E"),
        Triple("CN", "RMX3850", "RMX3850NV97")
    )

    // Helper function to generate the full list of variants for a specific version letter (e.g., "A", "C").
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
                name = "realme GT6T/6SE",
                ruiVersion = 5, // Use the latest ruiVersion
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=RGT6T6SE",
                firmwareGroups = mapOf(
                    "Android 14" to generateRegionalVariants("A"),
                    "Android 15" to generateRegionalVariants("C"),
                    "Android 16" to generateRegionalVariants("F")

                )
            )
        )
    }
}
