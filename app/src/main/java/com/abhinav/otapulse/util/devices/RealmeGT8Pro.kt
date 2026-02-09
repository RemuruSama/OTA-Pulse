package com.abhinav.otapulse.util.devices

import com.abhinav.otapulse.util.DeviceProvider
import com.abhinav.otapulse.util.PredefinedDevice
import com.abhinav.otapulse.util.RegionVariant

class RealmeGT8Pro : DeviceProvider {

    // Base regional variants
    // Triple -> Display Name, Product Model, Firmware Base
    private val baseVariants = listOf(
        Triple("TW", "RMX5210", "RMX5210NV1A"),
        Triple("TR", "RMX5210TR", "RMX5210NV51"),
        Triple("RU", "RMX5210RU", "RMX5210NV37"),
        Triple("MEA", "RMX5210", "RMX5210NVA6"),
        Triple("MY", "RMX5210", "RMX5210NV38"),
        Triple("SA", "RMX5210", "RMX5210NV83"),
        Triple("ID", "RMX5210", "RMX5210NV33"),
        Triple("IN", "RMX5210IN", "RMX5210NV1B"),
        Triple("EU", "RMX5210EEA", "RMX5210NV44"),
        Triple("TH", "RMX5210", "RMX5210NV39"),
        Triple("LATAM", "RMX5210", "RMX5210NV9A"),
        Triple("BR", "RMX5210", "RMX5210NV9E"),
        Triple("CN", "RMX5200", "RMX5200NV97")
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
                name = "realme GT 8 Pro",
                ruiVersion = 5,
                imageUrl = "https://placehold.co/100x100/FFF3E0/E65100?text=GT8P",
                firmwareGroups = mapOf(
                    "Android 16" to generateRegionalVariants("A")
                )
            )
        )
    }
}
