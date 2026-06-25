package com.abhinav.otapulse.feature.downloads.domain

import com.abhinav.otapulse.core.model.Device
import com.abhinav.otapulse.core.model.OtaUpdate
import com.abhinav.otapulse.core.model.RegionVariant
import com.abhinav.otapulse.feature.downloads.domain.DownloadRepository
import javax.inject.Inject

class EnqueueDownloadUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository
) {
    operator fun invoke(otaUpdate: OtaUpdate, deviceName: String, regionName: String, isFromHomeUpdate: Boolean = false) {
        downloadRepository.enqueueDownload(
            otaUpdate = otaUpdate,
            deviceName = deviceName,
            regionName = regionName,
            isFromHomeUpdate = isFromHomeUpdate
        )
    }
}
