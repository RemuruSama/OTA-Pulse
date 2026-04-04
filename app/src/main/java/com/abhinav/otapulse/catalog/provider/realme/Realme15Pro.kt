package com.abhinav.otapulse.catalog.provider.realme

import com.abhinav.otapulse.catalog.provider.DeviceProvider
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.core.model.RegionVariant

class Realme15Pro : DeviceProvider {

    private val baseVariants = listOf(
        Triple("TW", "RMX5101", "RMX5101NV1A"),
        Triple("TR", "RMX5101TR", "RMX5101NV51"),
        Triple("RU", "RMX5101RU", "RMX5101NV37"),
        Triple("MEA", "RMX5101", "RMX5101NVA6"),
        Triple("MY", "RMX5101", "RMX5101NV38"),
        Triple("SA", "RMX5101", "RMX5101NV83"),
        Triple("ID", "RMX5101", "RMX5101NV33"),
        Triple("IN", "RMX5101IN", "RMX5101NV1B"),
        Triple("EU", "RMX5101EEA", "RMX5101NV44"),
        Triple("TH", "RMX5101", "RMX5101NV39"),
        Triple("LATAM", "RMX5101", "RMX5101NV9A"),
        Triple("BR", "RMX5101", "RMX5101NV9E"),
        Triple("CN", "RMX5100", "RMX5100NV97")
    )

    private fun generateRegionalVariants(versionLetter: String): List<RegionVariant> {
        return baseVariants.map { (displayName, productModel, firmwareBase) ->
            val region = when (displayName) {
                "IN" -> "EU"
                "CN" -> "CN"
                "EU", "RU", "MEA", "TH" -> "EU"
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
                name = "realme 15 Pro",
                ruiVersion = 5,
                imageUrl = "https://placehold.co/100x100/FFFDE7/F9A825?text=15+Pro",
                firmwareGroups = mapOf(
                    "Android 15" to generateRegionalVariants("A"),
                    "Android 16" to generateRegionalVariants("C")
                )
            )
        )
    }
}
