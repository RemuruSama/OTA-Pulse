package com.abhinav.otapulse.feature.devices.domain

import com.abhinav.otapulse.core.model.OtaRequest
import com.abhinav.otapulse.core.model.OtaUpdate
import com.abhinav.otapulse.ota.engine.OtaRepository
import javax.inject.Inject

class FetchOtaUpdateUseCase @Inject constructor(
    private val repository: OtaRepository
) {
    suspend operator fun invoke(request: OtaRequest): Result<List<OtaUpdate>> {
        return repository.fetchOtaUpdate(request)
    }
}
