package com.abhinav.otapulse.catalog.provider.oppo

import com.abhinav.otapulse.catalog.provider.DeviceProvider
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.core.model.RegionVariant

class OPPOFindN3 : DeviceProvider {

    private val baseVariants = listOf(
        Triple("ID", "CPH2499", "CPH2499NV33"),
        Triple("SG", "CPH2499", "CPH2499NV2C"),
        Triple("TW", "CPH2499", "CPH2499NV1A"),
        Triple("MY", "CPH2499", "CPH2499NV38"),
        Triple("TH", "CPH2499", "CPH2499NV39"),
        Triple("VN", "CPH2499", "CPH2499NV3C"),
        Triple("OCA", "CPH2499", "CPH2499NVA5"),
        Triple("CN", "PHN110", "PHN110NV97")
    )

    private fun generateRegionalVariants(versionLetter: String): List<RegionVariant> {
        return baseVariants.map { (displayName, productModel, firmwareBase) ->
            val region = when (displayName) {
                "CN" -> "CN"
                "ID", "SG", "TW", "MY", "TH", "VN", "OCA" -> "EU"
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
                name = "OPPO Find N3",
                ruiVersion = 5,
                imageUrl = "https://placehold.co/100x100/E0F2F1/00796B?text=OPFN3",
                firmwareGroups = mapOf(
                    "Android 13" to generateRegionalVariants("A"),
                    "Android 14" to generateRegionalVariants("C"),
                    "Android 15" to generateRegionalVariants("F"),
                    "Android 16" to generateRegionalVariants("H")
                )
            )
        )
    }
}
