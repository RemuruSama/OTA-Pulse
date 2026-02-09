package com.abhinav.otapulse.util.devices

import com.abhinav.otapulse.util.DeviceProvider
import com.abhinav.otapulse.util.PredefinedDevice
import com.abhinav.otapulse.util.RegionVariant

class OnePlus10Pro : DeviceProvider {

    override fun getDevices(): List<PredefinedDevice> {
        val baseVariants = listOf(
            Triple("IN", "NE2211", "NE2211NV1B"),
            Triple("GLO", "NE2213", "NE2213NVA7"),
            Triple("EU", "NE2213EEA", "NE2213NV44"),
            Triple("CN", "NE2210", "NE2210NV97")
        )

        fun generateRegionalVariants(versionLetter: String): List<RegionVariant> {
            return baseVariants.map { (displayName, productModel, firmwareBase) ->
                val region = when (displayName) {
                    "IN" -> "IN"
                    "CN" -> "CN"
                    "EU" -> "EU"
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

        return listOf(
            PredefinedDevice(
                name = "OnePlus 10 Pro",
                ruiVersion = 5, // Use the latest ruiVersion
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=OP10P",
                firmwareGroups = mapOf(
                    "Android 12" to generateRegionalVariants("A"),
                    "Android 13" to generateRegionalVariants("C"),
                    "Android 14" to generateRegionalVariants("F"),
                    "Android 15" to generateRegionalVariants("H")
                )
            )
        )
    }
}
