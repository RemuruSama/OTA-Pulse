package com.abhinav.otapulse.util.devices

import com.abhinav.otapulse.util.DeviceProvider
import com.abhinav.otapulse.util.PredefinedDevice
import com.abhinav.otapulse.util.RegionVariant

class RealmeGT7Pro : DeviceProvider {

    // Base list of regional variants to avoid duplication.
    // The triple contains: Display Name, Product Model, and the base part of the firmware string.
    private val baseVariants = listOf(
        Triple("TR", "RMX5011TR", "RMX5011NV51"),
        Triple("RU", "RMX5011RU", "RMX5011NV37"),
        Triple("MEA", "RMX5011", "RMX5011NVA6"),
        // Triple("SA", "RMX5011", "RMX5011NV83"),
        Triple("IN", "RMX5011IN", "RMX5011NV1B"),
        Triple("EU", "RMX5011EEA", "RMX5011NV44"),
        Triple("TH", "RMX5011", "RMX5011NV39"),
        // Triple("LATAM", "RMX5011", "RMX5011NV9A"),
        // Triple("BR", "RMX5011", "RMX5011NV9E"),
        Triple("CN", "RMX5010", "RMX5010NV97")
    )

    // Helper function to generate the full list of variants for a specific version letter (e.g., "A").
    private fun generateRegionalVariants(versionLetter: String): List<RegionVariant> {
        return baseVariants.map { (displayName, productModel, firmwareBase) ->
            val region = when (displayName) {
                "IN" -> "IN"
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
                name = "realme GT7 Pro",
                ruiVersion = 5, // Use the latest ruiVersion
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=RGT7P",
                firmwareGroups = mapOf(
                    "Android 15" to generateRegionalVariants("A"),
                    "Android 16" to generateRegionalVariants("C")

                )
            )
        )
    }
}