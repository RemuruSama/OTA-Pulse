package com.abhinav.otapulse.domain.usecase

import com.abhinav.otapulse.domain.model.Device
import com.abhinav.otapulse.domain.model.OtaUpdate
import com.abhinav.otapulse.domain.model.RegionVariant
import com.abhinav.otapulse.domain.model.OtaRequest
import com.abhinav.otapulse.domain.repository.OtaRepository
import com.abhinav.otapulse.util.Data
import com.abhinav.otapulse.util.RegionData
import com.abhinav.otapulse.util.toDomain
import javax.inject.Inject

/**
 * Unified use case to fetch OTA details for both manual queries and predefined devices.
 */
class FetchOtaDetailsUseCase @Inject constructor(private val otaRepository: OtaRepository) {
    suspend operator fun invoke(device: Device, variant: RegionVariant): Result<OtaUpdate> {
        // 1. Resolve Region Info / Server ID
        // Try mapping the region string directly (for manual "EU", "IN", etc.)
        var regionId = Data.getServerId(variant.region)
        
        // If it's a display name (e.g. "Vietnam"), find its corresponding server code
        val regionInfo = RegionData.regions.find {
            it.displayName.equals(variant.region, ignoreCase = true)
        }
        
        if (regionInfo != null && regionId == 0 && variant.region != "GL") {
            // Re-resolve if it was a display name
            regionId = Data.getServerId(regionInfo.serverCode)
        }

        // 2. Resolve NV Identifier
        val nvIdentifier = variant.nvId ?: regionInfo?.nvid ?: "0"

        // 3. Construct Request
        val otaRequest = OtaRequest(
            version = if (device.ruiVersion == 1) 1 else 2,
            model = variant.productModel,
            firmwareVersion = variant.firmwareVersion,
            region = regionId,
            ruiVersion = device.ruiVersion,
            imei0 = device.imei,
            beta = device.beta,
            nvIdentifier = nvIdentifier,
            language = variant.language
        )

        // 4. Fetch and Map
        return otaRepository.fetchOtaUpdate(otaRequest).mapCatching { components ->
            if (components.isNotEmpty()) {
                components.first().toDomain()
            } else {
                throw Exception("Server returned empty component list.")
            }
        }
    }
}