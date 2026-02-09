package com.abhinav.otapulse.util.devices

import com.abhinav.otapulse.util.DeviceProvider
import com.abhinav.otapulse.util.PredefinedDevice
import com.abhinav.otapulse.util.RegionVariant

class RealmeP3_5G : DeviceProvider {

    // Base list of regional variants to avoid duplication.
    // The triple contains: Display Name, Product Model, and the base part of the firmware string.
    private val baseVariants = listOf(
        Triple("TW", "RMX5070", "RMX5070NV1A"),
        Triple("TR", "RMX5070", "RMX5070NV51"),
        Triple("RU", "RMX5070", "RMX5070NV37"),
        Triple("MEA", "RMX5070", "RMX5070NVA6"),
        Triple("MY", "RMX5070", "RMX5070NV38"),
        Triple("SA", "RMX5070", "RMX5070NV83"),
        Triple("ID", "RMX5070", "RMX5070NV33"),
        Triple("IN", "RMX5070IN", "RMX5070NV1B"),
        Triple("EU", "RMX5070", "RMX5070NV44"),
        Triple("TH", "RMX5070", "RMX5070NV39"),
        Triple("LATAM", "RMX5070", "RMX5070NV9A"),
        Triple("BR", "RMX5070", "RMX5070NV9E"),
    )

    // Helper function to generate the full list of variants for a specific version letter (e.g., "A").
    private fun generateRegionalVariants(versionLetter: String): List<RegionVariant> {
        return baseVariants.map { (displayName, productModel, firmwareBase) ->
            val region = when (displayName) {
                "EU", "RU", "MEA", "SA", "TH", "MY", "ID" , "IN" -> "EU"
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
                name = "realme P3 5G",
                ruiVersion = 6, // Use the latest ruiVersion
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=P3_5G",
                firmwareGroups = mapOf(
                    "Android 15" to generateRegionalVariants("A"),
                    "Android 16" to generateRegionalVariants("C")
                )
            )
        )
    }
}