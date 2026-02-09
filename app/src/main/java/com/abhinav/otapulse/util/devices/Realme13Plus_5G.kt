package com.abhinav.otapulse.util.devices

import com.abhinav.otapulse.util.DeviceProvider
import com.abhinav.otapulse.util.PredefinedDevice
import com.abhinav.otapulse.util.RegionVariant

class Realme13Plus_5G : DeviceProvider {

    // Base list of regional variants to avoid duplication.
    // The triple contains: Display Name, Product Model, and the base part of the firmware string.
    private val baseVariants = listOf(
        Triple("TW", "RMX5000", "RMX5000NV1A"),
        Triple("MEA", "RMX5000", "RMX5000NVA6"),
        Triple("MY", "RMX5000", "RMX5000NV38"),
        Triple("ID", "RMX5000", "RMX5000NV33"),
        Triple("IN", "RMX5000IN", "RMX5000NV1B"),
        Triple("TH", "RMX5000", "RMX5000NV39"),
        Triple("LATAM", "RMX5000", "RMX5000NV9A"),
        Triple("BR", "RMX5000", "RMX5000NV9E"),
    )

    // Helper function to generate the full list of variants for a specific version letter (e.g., "A", "C").
    private fun generateRegionalVariants(versionLetter: String): List<RegionVariant> {
        return baseVariants.map { (displayName, productModel, firmwareBase) ->
            val region = when (displayName) {
                "MEA", "SA", "TH", "MY", "ID", "IN" -> "EU"
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
                name = "Realme 13+ 5G",
                ruiVersion = 6, // Use the latest ruiVersion
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=13P_5G",
                firmwareGroups = mapOf(
                    "Android 14" to generateRegionalVariants("A"),
                    "Android 15" to generateRegionalVariants("C"),
                    "Android 16" to generateRegionalVariants("F")

                )
            )
        )
    }
}