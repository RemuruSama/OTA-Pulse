package com.abhinav.otapulse.util.devices

import com.abhinav.otapulse.util.DeviceProvider
import com.abhinav.otapulse.util.PredefinedDevice
import com.abhinav.otapulse.util.RegionVariant

class Realme15x5G : DeviceProvider {

    private val baseVariants = listOf(
        Triple("IN", "RMX5250IN", "RMX5250NV1B"),
    )

    private fun generateRegionalVariants(versionLetter: String): List<RegionVariant> {
        return baseVariants.map { (displayName, productModel, firmwareBase) ->
            val region = when (displayName) {
                "IN" -> "IN"
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
                name = "realme 15x 5G",
                ruiVersion = 5,
                imageUrl = "https://placehold.co/100x100/E1F5FE/0277BD?text=15x+5G",
                firmwareGroups = mapOf(
                    "Android 15" to generateRegionalVariants("A"),
                )
            )
        )
    }
}
