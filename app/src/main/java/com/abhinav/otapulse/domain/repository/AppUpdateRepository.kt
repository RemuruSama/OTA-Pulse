package com.abhinav.otapulse.domain.repository

import com.abhinav.otapulse.domain.model.AppUpdateInfo

interface AppUpdateRepository {
    suspend fun checkForUpdate(currentVersion: String): Result<AppUpdateInfo?>
}
