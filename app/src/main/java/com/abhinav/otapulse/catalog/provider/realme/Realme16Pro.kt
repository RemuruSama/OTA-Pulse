package com.abhinav.otapulse.catalog.provider.realme

import com.abhinav.otapulse.catalog.provider.DeviceProvider
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.core.model.RegionVariant

class Realme16Pro : DeviceProvider {

    private val baseVariants = listOf(
        Triple("TW", "RMX5120", "RMX5120NV1A"),
        Triple("TR", "RMX5120TR", "RMX5120NV51"),
        Triple("RU", "RMX5120RU", "RMX5120NV37"),
        Triple("MEA", "RMX5120", "RMX5120NVA6"),
        Triple("MY", "RMX5120", "RMX5120NV38"),
        Triple("SA", "RMX5120", "RMX5120NV83"),
        Triple("ID", "RMX5120", "RMX5120NV33"),
        Triple("IN", "RMX5120IN", "RMX5120NV1B"),
        Triple("EU", "RMX5120EEA", "RMX5120NV44"),
        Triple("TH", "RMX5120", "RMX5120NV39"),
        Triple("LATAM", "RMX5120", "RMX5120NV9A"),
        Triple("BR", "RMX5120", "RMX5120NV9E"),
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
                name = "realme 16 Pro",
                ruiVersion = 5,
                imageUrl = "https://placehold.co/100x100/E8EAF6/3949AB?text=16+Pro",
                firmwareGroups = mapOf(
                    "Android 16" to generateRegionalVariants("A")
                )
            )
        )
    }
}
