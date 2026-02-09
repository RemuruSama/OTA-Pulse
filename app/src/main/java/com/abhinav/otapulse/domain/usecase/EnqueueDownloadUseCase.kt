package com.abhinav.otapulse.domain.usecase

import com.abhinav.otapulse.domain.model.Device
import com.abhinav.otapulse.domain.model.OtaUpdate
import com.abhinav.otapulse.domain.model.RegionVariant
import com.abhinav.otapulse.domain.repository.DownloadRepository
import javax.inject.Inject

class EnqueueDownloadUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository
) {
    operator fun invoke(otaUpdate: OtaUpdate, device: Device, variant: RegionVariant) {
        downloadRepository.enqueueDownload(
            otaUpdate = otaUpdate,
            deviceName = device.name,
            regionName = variant.displayName
        )
    }
}
