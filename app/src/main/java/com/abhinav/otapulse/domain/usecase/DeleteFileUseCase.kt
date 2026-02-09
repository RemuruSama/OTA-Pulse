package com.abhinav.otapulse.domain.usecase

import com.abhinav.otapulse.domain.repository.DownloadRepository
import java.io.File
import javax.inject.Inject

class DeleteFileUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository
) {
    operator fun invoke(file: File): Boolean {
        return downloadRepository.deleteFile(file)
    }
}
