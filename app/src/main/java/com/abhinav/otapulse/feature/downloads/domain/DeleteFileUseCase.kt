package com.abhinav.otapulse.feature.downloads.domain

import com.abhinav.otapulse.feature.downloads.domain.DownloadRepository
import java.io.File
import javax.inject.Inject

class DeleteFileUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository
) {
    operator fun invoke(file: File): Boolean {
        return downloadRepository.deleteFile(file)
    }
}
