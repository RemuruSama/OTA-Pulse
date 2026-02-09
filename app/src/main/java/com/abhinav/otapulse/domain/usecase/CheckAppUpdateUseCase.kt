package com.abhinav.otapulse.domain.usecase

import com.abhinav.otapulse.domain.model.AppUpdateInfo
import com.abhinav.otapulse.domain.repository.AppUpdateRepository
import javax.inject.Inject

class CheckAppUpdateUseCase @Inject constructor(
    private val repository: AppUpdateRepository
) {
    suspend operator fun invoke(currentVersion: String): Result<AppUpdateInfo?> {
        return repository.checkForUpdate(currentVersion)
    }
}
