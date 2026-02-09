package com.abhinav.otapulse.util.devices

import com.abhinav.otapulse.util.DeviceProvider
import com.abhinav.otapulse.util.PredefinedDevice
import com.abhinav.otapulse.util.RegionVariant

class RealmeGT2Pro : DeviceProvider {

    // Base list of regional variants to avoid duplication
    // The triple contains: Display Name, Product Model, and the base part of the firmware string
    private val baseVariants = listOf(
        Triple("TW", "RMX3301", "RMX3301NV1A"),
        Triple("TR", "RMX3301", "RMX3301NV51"),
        Triple("RU", "RMX3301", "RMX3301NV37"),
        Triple("MEA", "RMX3301", "RMX3301NVA6"),
        Triple("MY", "RMX3301", "RMX3301NV38"),
        Triple("SA", "RMX3301", "RMX3301NV83"),
        Triple("ID", "RMX3301", "RMX3301NV33"),
        Triple("IN", "RMX3301", "RMX3301NV1B"),
        Triple("EU", "RMX3301", "RMX3301NV44"),
        Triple("TH", "RMX3301", "RMX3301NV39"),
        Triple("LATAM", "RMX3301", "RMX3301NV9A"),
        Triple("BR", "RMX3301", "RMX3301NV9E"),
        Triple("CN", "RMX3300", "RMX3300NV97")
    )

    // Helper function to generate the full list of variants for a specific version letter (e.g., "A", "C", "F", "H")
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
                name = "realme GT 2 Pro",
                ruiVersion = 5, // Use the latest ruiVersion
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=RGT2P",
                firmwareGroups = mapOf(
                    "Android 12" to generateRegionalVariants("A"),
                    "Android 13" to generateRegionalVariants("C"),
                    "Android 14" to generateRegionalVariants("F"),
                    "Android 15" to generateRegionalVariants("H")
                )
            )
        )
    }
}