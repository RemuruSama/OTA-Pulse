package com.abhinav.otapulse.domain.usecase

import com.abhinav.otapulse.domain.model.DownloadInfo
import com.abhinav.otapulse.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class GetDownloadsUseCase @Inject constructor(private val downloadRepository: DownloadRepository) {
    operator fun invoke(): StateFlow<List<DownloadInfo>> {
        return downloadRepository.allDownloads
    }
}