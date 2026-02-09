package com.abhinav.otapulse.util.devices

import com.abhinav.otapulse.util.DeviceProvider
import com.abhinav.otapulse.util.PredefinedDevice
import com.abhinav.otapulse.util.RegionVariant

class OnePlus13s : DeviceProvider {

    // Base list of regional variants to avoid duplication.
    // Triple contains: Display Name, Product Model, Firmware Base
    private val baseVariants = listOf(
        Triple("IN", "CPH2723IN", "CPH2723NV1B"),
        Triple("GLO", "CPH2653", "CPH2653NVA7"),
        Triple("EU", "CPH2653EEA", "CPH2653NV44")
    )

    // Helper function to generate variants using a single version letter
    private fun generateRegionalVariants(versionLetter: String): List<RegionVariant> {
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

    // Android 16: GLO & EU use "F", others use "C"
    private fun generateAndroid16Variants(): List<RegionVariant> {
        return baseVariants.map { (displayName, productModel, firmwareBase) ->
            val region = when (displayName) {
                "IN" -> "IN"
                "CN" -> "CN"
                "EU" -> "EU"
                else -> "GL"
            }

            val versionLetter = when (displayName) {
                "GLO", "EU" -> "F"
                else -> "C"
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
                name = "OnePlus 13s",
                ruiVersion = 5,
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=OP13",
                firmwareGroups = mapOf(
                    "Android 15" to generateRegionalVariants("A"),
                    "Android 16" to generateAndroid16Variants()
                )
            )
        )
    }
}