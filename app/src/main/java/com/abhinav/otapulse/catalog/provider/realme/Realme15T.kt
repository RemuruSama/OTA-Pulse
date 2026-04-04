package com.abhinav.otapulse.catalog.provider.realme

import com.abhinav.otapulse.catalog.provider.DeviceProvider
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.core.model.RegionVariant
import kotlin.Triple

class Realme15T : DeviceProvider {

    private val baseVariants = listOf(
        Triple("IN", "RMX5111IN", "RMX5111NV1B"),
        Triple("CN", "RMX5112", "RMX5112NV97")
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
                name = "realme 15T",
                ruiVersion = 5,
                imageUrl = "https://placehold.co/100x100/F1F8E9/558B2F?text=15T",
                firmwareGroups = mapOf(
                    "Android 15" to generateRegionalVariants("C"),
                    "Android 16" to generateRegionalVariants("F")
                )
            )
        )
    }
}
