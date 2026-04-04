package com.abhinav.otapulse.feature.downloads.domain

import com.abhinav.otapulse.core.model.Device
import com.abhinav.otapulse.core.model.OtaUpdate
import com.abhinav.otapulse.core.model.RegionVariant
import com.abhinav.otapulse.feature.downloads.domain.DownloadRepository
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
