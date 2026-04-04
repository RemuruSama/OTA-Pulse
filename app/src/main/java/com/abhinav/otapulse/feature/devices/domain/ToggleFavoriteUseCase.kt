package com.abhinav.otapulse.feature.devices.domain

import com.abhinav.otapulse.catalog.repository.DeviceRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(private val deviceRepository: DeviceRepository) {
    suspend operator fun invoke(deviceName: String) {
        deviceRepository.toggleFavoriteStatus(deviceName)
    }
}
