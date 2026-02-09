package com.abhinav.otapulse.util.devices

import com.abhinav.otapulse.util.DeviceProvider
import com.abhinav.otapulse.util.PredefinedDevice
import com.abhinav.otapulse.util.RegionVariant

class Realme12ProPlus : DeviceProvider {

    // Base list of regional variants to avoid duplication.
    // The triple contains: Display Name, Product Model, and the base part of the firmware string.
    private val baseVariants = listOf(
        Triple("TW", "RMX3840", "RMX3840NV1A"),
        Triple("TR", "RMX3840TR", "RMX3840NV51"),
        Triple("RU", "RMX3840RU", "RMX3840NV37"),
        Triple("MEA", "RMX3840", "RMX3840NVA6"),
        Triple("MY", "RMX3840", "RMX3840NV38"),
        Triple("SA", "RMX3840", "RMX3840NV83"),
        Triple("ID", "RMX3840", "RMX3840NV33"),
        Triple("IN", "RMX3840IN", "RMX3840NV1B"),
        Triple("EU", "RMX3840EEA", "RMX3840NV44"),
        Triple("TH", "RMX3840", "RMX3840NV39"),
        Triple("LATAM", "RMX3840", "RMX3840NV9A"),
        Triple("BR", "RMX3840", "RMX3840NV9E"),
        Triple("CN", "RMX3841", "RMX3841NV97")
    )

    // Helper function to generate the full list of variants for a specific version letter (e.g., "A", "C").
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
                name = "realme 12 Pro+",
                ruiVersion = 5, // Use the latest ruiVersion
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=R12P+",
                firmwareGroups = mapOf(
                    "Android 14" to generateRegionalVariants("A"),
                    "Android 15" to generateRegionalVariants("C"),
                    "Android 16" to generateRegionalVariants("F")

                )
            )
        )
    }
}