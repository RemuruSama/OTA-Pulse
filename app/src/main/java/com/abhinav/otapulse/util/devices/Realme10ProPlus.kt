package com.abhinav.otapulse.util.devices

import com.abhinav.otapulse.util.DeviceProvider
import com.abhinav.otapulse.util.PredefinedDevice
import com.abhinav.otapulse.util.RegionVariant

class Realme10ProPlus : DeviceProvider {

    // Base list of regional variants to avoid duplication
    // The triple contains: Display Name, Product Model, and the base part of the firmware string
    private val baseVariants = listOf(
        Triple("TW", "RMX3686", "RMX3686NV1A"),
        Triple("TR", "RMX3686", "RMX3686NV51"),
        Triple("RU", "RMX3686", "RMX3686NV37"),
        Triple("MEA", "RMX3686", "RMX3686NVA6"),
        Triple("MY", "RMX3686", "RMX3686NV38"),
        Triple("SA", "RMX3686", "RMX3686NV83"),
        Triple("ID", "RMX3686", "RMX3686NV33"),
        Triple("IN", "RMX3686", "RMX3686NV1B"),
        Triple("TH", "RMX3686", "RMX3686NV39"),
        Triple("LATAM", "RMX3686", "RMX3686NV9A"),
        Triple("BR", "RMX3686", "RMX3686NV9E"),
        Triple("CN", "RMX3687", "RMX3687NV97")
    )

    // Helper function to generate the full list of variants for a specific version letter (e.g., "A", "C", "F")
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
                name = "realme 10 Pro+",
                ruiVersion = 5, // Use the latest ruiVersion
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=R10PP",
                firmwareGroups = mapOf(
                    "Android 13" to generateRegionalVariants("A"),
                    "Android 14" to generateRegionalVariants("C"),
                    "Android 15" to generateRegionalVariants("F")
                )
            )
        )
    }
}