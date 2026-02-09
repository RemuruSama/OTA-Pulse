package com.abhinav.otapulse.util.devices

import com.abhinav.otapulse.util.DeviceProvider
import com.abhinav.otapulse.util.PredefinedDevice
import com.abhinav.otapulse.util.RegionVariant

class RealmeP4_5G : DeviceProvider {

    // Base list of regional variants to avoid duplication.
    // Triple: Display Name, Product Model, Firmware Base
    private val baseVariants = listOf(
        Triple("IN", "RMX5110IN", "RMX5110NV1B"),
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
                name = "realme P4 5G",
                ruiVersion = 6,
                imageUrl = "https://placehold.co/100x100/E8F5E9/2E7D32?text=P4_5G",
                firmwareGroups = mapOf(
                    "Android 15" to generateRegionalVariants("A"),
                    "Android 16" to generateRegionalVariants("C")
                )
            )
        )
    }
}
