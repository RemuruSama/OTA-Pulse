package com.abhinav.otapulse.catalog.provider.oneplus

import com.abhinav.otapulse.catalog.provider.DeviceProvider
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.core.model.RegionVariant

class OnePlus13 : DeviceProvider {

    // Base list of regional variants to avoid duplication.
    // The triple contains: Display Name, Product Model, and the base part of the firmware string.
    private val baseVariants = listOf(
        Triple("IN", "CPH2649IN", "CPH2649NV1B"),
        Triple("GLO", "CPH2653", "CPH2653NVA7"),
        Triple("EU", "CPH2653EEA", "CPH2653NV44"),
        Triple("CN", "PJZ110", "PJZ110NV97")
    )

    // Helper function to generate the full list of variants for a specific version letter (e.g., "A").
    private fun generateRegionalVariants(defaultLetter: String, cnLetter: String = defaultLetter): List<RegionVariant> {
        return baseVariants.map { (displayName, productModel, firmwareBase) ->
            val region = when (displayName) {
                "IN" -> "IN"
                "CN" -> "CN"
                "EU" -> "EU"
                else -> "GL"
            }
            val letter = if (displayName == "CN") cnLetter else defaultLetter
            RegionVariant(
                displayName = displayName,
                productModel = productModel,
                firmwareVersion = "${firmwareBase}_11.${letter}.01_0001_100001010000",
                region = region
            )
        }
    }

    override fun getDevices(): List<PredefinedDevice> {
        return listOf(
            PredefinedDevice(
                name = "OnePlus 13",
                ruiVersion = 5, // Use the latest ruiVersion
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=OP13",
                firmwareGroups = mapOf(
                    "Android 15" to generateRegionalVariants("A"),
                    "Android 16" to generateRegionalVariants("F", cnLetter = "C")
                )
            )
        )
    }
}
