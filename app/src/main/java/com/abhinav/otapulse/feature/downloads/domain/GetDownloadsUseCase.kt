package com.abhinav.otapulse.feature.downloads.domain

import com.abhinav.otapulse.core.model.DownloadInfo
import com.abhinav.otapulse.feature.downloads.domain.DownloadRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class GetDownloadsUseCase @Inject constructor(private val downloadRepository: DownloadRepository) {
    operator fun invoke(): StateFlow<List<DownloadInfo>> {
        return downloadRepository.allDownloads
    }
}
