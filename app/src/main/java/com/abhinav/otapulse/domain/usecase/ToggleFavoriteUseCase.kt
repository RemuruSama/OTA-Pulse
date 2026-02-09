package com.abhinav.otapulse.domain.usecase

import com.abhinav.otapulse.domain.repository.OtaRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(private val otaRepository: OtaRepository) {
    suspend operator fun invoke(deviceName: String) {
        val favorites = otaRepository.getFavorites()
        val currentStatus = favorites[deviceName] ?: false
        otaRepository.toggleFavoriteStatus(deviceName, !currentStatus)
    }
}