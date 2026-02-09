package com.abhinav.otapulse.util.devices

import com.abhinav.otapulse.util.DeviceProvider
import com.abhinav.otapulse.util.PredefinedDevice
import com.abhinav.otapulse.util.RegionVariant

class Realme16ProPlus : DeviceProvider {

    private val baseVariants = listOf(
        Triple("TW", "RMX5131", "RMX5131NV1A"),
        Triple("TR", "RMX5131TR", "RMX5131NV51"),
        Triple("RU", "RMX5131RU", "RMX5131NV37"),
        Triple("MEA", "RMX5131", "RMX5131NVA6"),
        Triple("MY", "RMX5131", "RMX5131NV38"),
        Triple("SA", "RMX5131", "RMX5131NV83"),
        Triple("ID", "RMX5131", "RMX5131NV33"),
        Triple("IN", "RMX5131IN", "RMX5131NV1B"),
        Triple("EU", "RMX5131EEA", "RMX5131NV44"),
        Triple("TH", "RMX5131", "RMX5131NV39"),
        Triple("LATAM", "RMX5131", "RMX5131NV9A"),
        Triple("BR", "RMX5131", "RMX5131NV9E"),
    )

    private fun generateRegionalVariants(versionLetter: String): List<RegionVariant> {
        return baseVariants.map { (displayName, productModel, firmwareBase) ->
            val region = when (displayName) {
                "IN" -> "IN"
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
                name = "realme 16 Pro+",
                ruiVersion = 5,
                imageUrl = "https://placehold.co/100x100/FCE4EC/C2185B?text=16+Pro+",
                firmwareGroups = mapOf(
                    "Android 16" to generateRegionalVariants("A"),
                )
            )
        )
    }
}
