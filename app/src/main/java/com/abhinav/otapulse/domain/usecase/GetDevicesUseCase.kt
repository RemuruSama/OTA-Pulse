package com.abhinav.otapulse.domain.usecase

import com.abhinav.otapulse.domain.model.Device
import com.abhinav.otapulse.domain.repository.OtaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetDevicesUseCase @Inject constructor(private val otaRepository: OtaRepository) {
    operator fun invoke(): Flow<List<Device>> {
        return otaRepository.getDevices().map { devices ->
            val favorites = otaRepository.getFavorites()
            devices.forEach { device ->
                device.isFavorite = favorites.containsKey(device.name)
            }
            devices.sortedByDescending { it.isFavorite }
        }
    }
}