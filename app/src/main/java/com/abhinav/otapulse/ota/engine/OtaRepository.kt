package com.abhinav.otapulse.ota.engine

import com.abhinav.otapulse.core.model.OtaRequest
import com.abhinav.otapulse.core.model.OtaUpdate

/**
 * Repository for OTA update fetching only.
 * Device catalog and favorites management live in [DeviceRepository].
 */
interface OtaRepository {
    suspend fun fetchOtaUpdate(request: OtaRequest): Result<List<OtaUpdate>>
}
