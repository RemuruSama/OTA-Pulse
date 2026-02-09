package com.abhinav.otapulse.domain.usecase

import com.abhinav.otapulse.domain.model.OtaRequest
import com.abhinav.otapulse.domain.repository.OtaRepository
import com.abhinav.otapulse.util.NetworkComponent
import javax.inject.Inject

class FetchOtaUpdateUseCase @Inject constructor(
    private val repository: OtaRepository
) {
    suspend operator fun invoke(request: OtaRequest): Result<List<NetworkComponent>> {
        return repository.fetchOtaUpdate(request)
    }
}
