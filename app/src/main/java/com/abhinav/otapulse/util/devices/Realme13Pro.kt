package com.abhinav.otapulse.util.devices

import com.abhinav.otapulse.util.DeviceProvider
import com.abhinav.otapulse.util.PredefinedDevice
import com.abhinav.otapulse.util.RegionVariant

class Realme13Pro : DeviceProvider {

    // Base list of regional variants to avoid duplication.
    // The triple contains: Display Name, Product Model, and the base part of the firmware string.
    private val baseVariants = listOf(
        Triple("IN", "RMX3990IN", "RMX3990NV1B"),
        Triple("CN", "RMX3989", "RMX3989NV97"),
        Triple("ID", "RMX3988", "RMX3988NV33"),
        Triple("PH", "RMX3988", "RMX3988NV3E"),
        Triple("MY", "RMX3988", "RMX3988NV38"),
        Triple("LATAM", "RMX3988", "RMX3988NV9A"),
        Triple("MEA", "RMX3988", "RMX3988NVA6"),
        Triple("TW", "RMX3988", "RMX3988NV1A")
    )

    // Helper function to generate the full list of variants for a specific version letter (e.g., "A", "C").
    private fun generateRegionalVariants(versionLetter: String): List<RegionVariant> {
        return baseVariants.map { (displayName, productModel, firmwareBase) ->
            val region = when (displayName) {
                "IN" -> "IN"
                "CN" -> "CN"
                "MEA", "MY", "ID" -> "EU"
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
                name = "realme 13 Pro",
                ruiVersion = 5, // Use the latest ruiVersion
                imageUrl = "https://static.c.realme.com/IN/index-category/138020060824111637.jpg",
                firmwareGroups = mapOf(
                    "Android 14" to generateRegionalVariants("A"),
                    "Android 15" to generateRegionalVariants("C"),
                    "Android 16" to generateRegionalVariants("F")
                )
            )
        )
    }
}