package com.abhinav.otapulse.catalog.provider.realme

import com.abhinav.otapulse.catalog.provider.DeviceProvider
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.core.model.RegionVariant

class RealmeP4Power_5G : DeviceProvider {

    // Base list of regional variants to avoid duplication.
    // Triple: Display Name, Product Model, Firmware Base
    private val baseVariants = listOf(
        Triple("IN", "RMX5107IN", "RMX5107NV1B"),
    )

    // Generate regional variants dynamically based on version letter
    private fun generateRegionalVariants(versionLetter: String): List<RegionVariant> {
        return baseVariants.map { (displayName, productModel, firmwareBase) ->
            val region = when (displayName) {
                "IN" -> "EU"
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
                name = "realme P4 Power 5G",
                ruiVersion = 6,
                imageUrl = "https://placehold.co/100x100/E8F5E9/2E7D32?text=P4_5G",
                firmwareGroups = mapOf(
                    "Android 16" to generateRegionalVariants("A")
                )
            )
        )
    }
}
