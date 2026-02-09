package com.abhinav.otapulse.util.devices

import com.abhinav.otapulse.util.DeviceProvider
import com.abhinav.otapulse.util.PredefinedDevice
import com.abhinav.otapulse.util.RegionVariant

class RealmeGT7 : DeviceProvider {

    private val baseVariants = listOf(
        Triple("TW", "RMX5061", "RMX5061NV1A"),
        Triple("TR", "RMX5061TR", "RMX5061NV51"),
        Triple("RU", "RMX5061RU", "RMX5061NV37"),
        Triple("MEA", "RMX5061", "RMX5061NVA6"),
        Triple("MY", "RMX5061", "RMX5061NV38"),
        Triple("SA", "RMX5061", "RMX5061NV83"),
        Triple("ID", "RMX5061", "RMX5061NV33"),
        Triple("IN", "RMX5061IN", "RMX5061NV1B"),
        Triple("EU", "RMX5061EEA", "RMX5061NV44"),
        Triple("TH", "RMX5061", "RMX5061NV39"),
        Triple("LATAM", "RMX5061", "RMX5061NV9A"),
        Triple("BR", "RMX5061", "RMX5061NV9E"),
        Triple("CN", "RMX6688", "RMX6688NV97")
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
                name = "realme GT 7",
                ruiVersion = 5,
                imageUrl = "https://placehold.co/100x100/E3F2FD/1565C0?text=GT7",
                firmwareGroups = mapOf(
                    "Android 15" to generateRegionalVariants("A"),
                    "Android 16" to generateRegionalVariants("F")
                )
            )
        )
    }
}
