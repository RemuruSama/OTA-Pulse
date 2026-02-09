package com.abhinav.otapulse.util.devices

import com.abhinav.otapulse.util.DeviceProvider
import com.abhinav.otapulse.util.PredefinedDevice
import com.abhinav.otapulse.util.RegionVariant

class Realme11Pro_5G : DeviceProvider {

    // Base list of regional variants to avoid duplication.
    // The triple contains: Display Name, Product Model, and the base part of the firmware string.
    private val baseVariants = listOf(
        Triple("TW", "RMX3771", "RMX3771NV1A"),
        Triple("TR", "RMX3771", "RMX3771NV51"),
        Triple("RU", "RMX3771", "RMX3771NV37"),
        Triple("MEA", "RMX3771", "RMX3771NVA6"),
        Triple("MY", "RMX3771", "RMX3771NV38"),
        Triple("SA", "RMX3771", "RMX3771NV83"),
        Triple("ID", "RMX3771", "RMX3771NV33"),
        Triple("IN", "RMX3771", "RMX3771NV1B"),
        Triple("EU", "RMX3771", "RMX3771NV44"),
        Triple("TH", "RMX3771", "RMX3771NV39"),
        Triple("LATAM", "RMX3771", "RMX3771NV9A"),
        Triple("CN", "RMX3770", "RMX3770NV97")
    )

    // Helper function to generate the full list of variants for a specific version letter (e.g., "A", "C", "F").
    private fun generateRegionalVariants(versionLetter: String): List<RegionVariant> {
        return baseVariants.map { (displayName, productModel, firmwareBase) ->
            val region = when (displayName) {
                "CN" -> "CN"
                "EU", "RU", "MEA", "SA", "TH", "MY", "ID", "IN" -> "EU"
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
                name = "realme 11 Pro 5G",
                ruiVersion = 6, // Use the latest ruiVersion
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=11P_5G",
                firmwareGroups = mapOf(
                    "Android 13" to generateRegionalVariants("A"),
                    "Android 14" to generateRegionalVariants("C"),
                    "Android 15" to generateRegionalVariants("F")
                )
            )
        )
    }
}