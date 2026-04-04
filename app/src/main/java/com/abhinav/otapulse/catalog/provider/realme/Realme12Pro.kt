package com.abhinav.otapulse.catalog.provider.realme

import com.abhinav.otapulse.catalog.provider.DeviceProvider
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.core.model.RegionVariant

class Realme12Pro : DeviceProvider {

    // Base list of regional variants to avoid duplication
    // The triple contains: Display Name, Product Model, and the base part of the firmware string
    private val baseVariants = listOf(
        Triple("TR", "RMX3842TR", "RMX3842NV51"), //
        Triple("RU", "RMX3842RU", "RMX3842NV37"), //
        Triple("MEA", "RMX3842", "RMX3842NVA6"), //
        Triple("SA", "RMX3842", "RMX3842NV83"), //
        Triple("IN", "RMX3842IN", "RMX3842NV1B"), //
        Triple("EU", "RMX3842EEA", "RMX3842NV44"), //
        Triple("BR", "RMX3842", "RMX3842NV9E"), //
        Triple("CN", "RMX3843", "RMX3843NV97") //
    )

    // Helper function to generate the full list of variants for a specific version letter (e.g., "A", "C")
    private fun generateRegionalVariants(versionLetter: String): List<RegionVariant> {
        return baseVariants.map { (displayName, productModel, firmwareBase) ->
            val region = when (displayName) { //
                "IN" -> "EU" //
                "CN" -> "CN" //
                "EU", "RU", "MEA", "SA" -> "EU" //
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
                name = "realme 12 Pro", //
                ruiVersion = 5, // Use the latest ruiVersion
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=R12P", //
                firmwareGroups = mapOf(
                    "Android 14" to generateRegionalVariants("A"), //
                    "Android 15" to generateRegionalVariants("C"), //
                    "Android 16" to generateRegionalVariants("F")
                )
            )
        )
    }
}
