package com.abhinav.otapulse.catalog.provider.realme

import com.abhinav.otapulse.catalog.provider.DeviceProvider
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.core.model.RegionVariant

class RealmeGT7T : DeviceProvider {

    private val baseVariants = listOf(
        Triple("TW", "RMX5085", "RMX5085NV1A"),
        Triple("TR", "RMX5085TR", "RMX5085NV51"),
        Triple("RU", "RMX5085RU", "RMX5085NV37"),
        Triple("MEA", "RMX5085", "RMX5085NVA6"),
        Triple("MY", "RMX5085", "RMX5085NV38"),
        Triple("SA", "RMX5085", "RMX5085NV83"),
        Triple("ID", "RMX5085", "RMX5085NV33"),
        Triple("IN", "RMX5085IN", "RMX5085NV1B"),
        Triple("EU", "RMX5085EEA", "RMX5085NV44"),
        Triple("TH", "RMX5085", "RMX5085NV39"),
        Triple("LATAM", "RMX5085", "RMX5085NV9A"),
        Triple("BR", "RMX5085", "RMX5085NV9E"),
    )

    private fun generateRegionalVariants(versionLetterProvider: (String) -> String): List<RegionVariant> {
        return baseVariants.map { (displayName, productModel, firmwareBase) ->
            val region = when (displayName) {
                "IN" -> "EU"
                "EU", "RU", "MEA", "TH" -> "EU"
                else -> "GL"
            }
            val versionLetter = versionLetterProvider(displayName)

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
                name = "realme GT 7T",
                ruiVersion = 5,
                imageUrl = "https://placehold.co/100x100/F3E5F5/6A1B9A?text=GT7T",
                firmwareGroups = mapOf(
                    "Android 15" to generateRegionalVariants { displayName ->
                        if (displayName in setOf("TW", "TR", "MY", "ID", "IN", "EU", "TH", "BR")) "C" else "A"
                    },
                    "Android 16" to generateRegionalVariants { "F" }
                )
            )
        )
    }
}
