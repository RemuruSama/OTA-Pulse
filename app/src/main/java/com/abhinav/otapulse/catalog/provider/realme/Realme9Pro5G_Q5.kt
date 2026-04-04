package com.abhinav.otapulse.catalog.provider.realme

import com.abhinav.otapulse.catalog.provider.DeviceProvider
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.core.model.RegionVariant

class Realme9Pro5G_Q5 : DeviceProvider {

    // Base list of regional variants to avoid duplication.
    // The triple contains: Display Name, Product Model, and the base part of the firmware string.
    private val baseVariants = listOf(
        Triple("TW", "RMX3472", "RMX3472NV1A"),
        Triple("TR", "RMX3472", "RMX3472NV51"),
        Triple("RU", "RMX3472", "RMX3472NV37"),
        Triple("MEA", "RMX3472", "RMX3472NVA6"),
        Triple("MY", "RMX3472", "RMX3472NV38"),
        Triple("SA", "RMX3472", "RMX3472NV83"),
        Triple("ID", "RMX3472", "RMX3472NV33"),
        Triple("EU", "RMX3472", "RMX3472NV44"),
        Triple("TH", "RMX3472", "RMX3472NV39"),
        Triple("LATAM", "RMX3472", "RMX3472NV9A"),
        Triple("IN", "RMX3471", "RMX3471NV1B"),
        Triple("CN", "RMX3478", "RMX3478NV97")
    )

    // Helper function to generate the full list of variants for a specific version letter (e.g., "A", "C", "F").
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
                name = "realme 9 Pro 5G/Q5",
                ruiVersion = 5, // Use the latest ruiVersion
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=R9P5G_Q5",
                firmwareGroups = mapOf(
                    "Android 12" to generateRegionalVariants("A"),
                    "Android 13" to generateRegionalVariants("C"),
                    "Android 14" to generateRegionalVariants("F")
                )
            )
        )
    }
}
