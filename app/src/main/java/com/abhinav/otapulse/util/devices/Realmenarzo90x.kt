package com.abhinav.otapulse.util.devices

import com.abhinav.otapulse.util.DeviceProvider
import com.abhinav.otapulse.util.PredefinedDevice
import com.abhinav.otapulse.util.RegionVariant

class RealmeNarzo90x : DeviceProvider {

    private val baseVariants = listOf(
        Triple("IN", "RMX5264IN", "RMX5264NV1B"),
        Triple("EU", "RMX5264EEA", "RMX5264NV44"),
        Triple("RU", "RMX5264RU", "RMX5264NV37"),
        Triple("MEA", "RMX5264", "RMX5264NVA6"),
        Triple("TH", "RMX5264", "RMX5264NV39")
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
                name = "realme narzo 90x",
                ruiVersion = 5,
                imageUrl = "https://placehold.co/100x100/F3E5F5/6A1B9A?text=Narzo+90x",
                firmwareGroups = mapOf(
                    "Android 16" to generateRegionalVariants("A")
                )
            )
        )
    }
}
