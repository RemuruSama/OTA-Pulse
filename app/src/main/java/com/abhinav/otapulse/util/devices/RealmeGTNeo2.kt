package com.abhinav.otapulse.util.devices

import com.abhinav.otapulse.util.DeviceProvider
import com.abhinav.otapulse.util.PredefinedDevice
import com.abhinav.otapulse.util.RegionVariant

class RealmeGTNeo2 : DeviceProvider {

    // Base list of regional variants to avoid duplication
    // The triple contains: Display Name, Product Model, and the base part of the firmware string
    private val baseVariants = listOf(
        Triple("TW", "RMX3370", "RMX3370NV1A"),
        Triple("TR", "RMX3370", "RMX3370NV51"),
        Triple("RU", "RMX3370", "RMX3370NV37"),
        Triple("MY", "RMX3370", "RMX3370NV38"),
        Triple("ID", "RMX3370", "RMX3370NV33"),
        Triple("IN", "RMX3370", "RMX3370NV1B"),
        Triple("EU", "RMX3370", "RMX3370NV44"),
        Triple("TH", "RMX3370", "RMX3370NV39"),
        Triple("LATAM", "RMX3370", "RMX3370NV9A"),
        Triple("CN", "RMX3370", "RMX3370NV97")
    )

    // Helper function to generate the full list of variants for a specific version letter (e.g., "F")
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
                name = "realme GT Neo 2",
                ruiVersion = 4, // Use the latest ruiVersion
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=RGTN2",
                firmwareGroups = mapOf(
                    "Android 13" to generateRegionalVariants("F")
                )
            )
        )
    }
}