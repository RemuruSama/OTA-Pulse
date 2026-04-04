package com.abhinav.otapulse.catalog.provider.realme

import com.abhinav.otapulse.catalog.provider.DeviceProvider
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.core.model.RegionVariant

class RealmeGTNeo3T : DeviceProvider {

    // Base list of regional variants to avoid duplication
    // The triple contains: Display Name, Product Model, and the base part of the firmware string
    private val baseVariants = listOf(
        Triple("TW", "RMX3371", "RMX3371NV1A"), //
        Triple("TR", "RMX3371", "RMX3371NV51"), //
        Triple("RU", "RMX3371", "RMX3371NV37"), //
        Triple("MEA", "RMX3371", "RMX3371NVA6"), //
        Triple("MY", "RMX3371", "RMX3371NV38"), //
        Triple("SA", "RMX3371", "RMX3371NV83"), //
        Triple("ID", "RMX3371", "RMX3371NV33"), //
        Triple("IN", "RMX3371", "RMX3371NV1B"), //
        Triple("EU", "RMX3371", "RMX3371NV44"), //
        Triple("TH", "RMX3371", "RMX3371NV39"), //
        Triple("LATAM", "RMX3371", "RMX3371NV9A"), //
        Triple("BR", "RMX3371", "RMX3371NV9E"), //
        Triple("CN", "RMX3372", "RMX3372NV97") //
    )

    // Helper function to generate the full list of variants for a specific version letter (e.g., "A", "C", "F")
    private fun generateRegionalVariants(versionLetter: String): List<RegionVariant> {
        return baseVariants.map { (displayName, productModel, firmwareBase) ->
            val region = when (displayName) { //
                "IN" -> "IN" //
                "CN" -> "CN" //
                "EU", "RU", "MEA", "SA", "TH", "MY", "ID" -> "EU" //
                else -> "GL" //
            }
            RegionVariant(
                displayName = displayName, //
                productModel = productModel, //
                firmwareVersion = "${firmwareBase}_11.${versionLetter}.01_0001_100001010000", //
                region = region //
            )
        }
    }

    override fun getDevices(): List<PredefinedDevice> {
        return listOf(
            PredefinedDevice(
                name = "realme GT Neo 3T", //
                ruiVersion = 5, // Use the latest ruiVersion
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=RGTN3T", //
                firmwareGroups = mapOf(
                    "Android 12" to generateRegionalVariants("A"), //
                    "Android 13" to generateRegionalVariants("C"), //
                    "Android 14" to generateRegionalVariants("F") //
                )
            )
        )
    }
}
