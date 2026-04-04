package com.abhinav.otapulse.feature.settings

import com.abhinav.otapulse.core.model.AppUpdateInfo
import com.abhinav.otapulse.feature.settings.AppUpdateRepository
import javax.inject.Inject

class CheckAppUpdateUseCase @Inject constructor(
    private val repository: AppUpdateRepository
) {
    suspend operator fun invoke(currentVersion: String): Result<AppUpdateInfo?> {
        return repository.checkForUpdate(currentVersion)
    }
}
