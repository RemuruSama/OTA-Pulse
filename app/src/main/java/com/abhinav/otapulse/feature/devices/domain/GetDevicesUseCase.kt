package com.abhinav.otapulse.feature.devices.domain

import com.abhinav.otapulse.core.model.Device
import com.abhinav.otapulse.catalog.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDevicesUseCase @Inject constructor(private val deviceRepository: DeviceRepository) {
    operator fun invoke(): Flow<List<Device>> = deviceRepository.getDevices()
}
