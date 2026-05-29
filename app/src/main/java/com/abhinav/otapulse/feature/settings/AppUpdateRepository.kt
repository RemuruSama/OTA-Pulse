package com.abhinav.otapulse.feature.settings

import com.abhinav.otapulse.core.model.AppUpdateInfo

interface AppUpdateRepository {
    suspend fun checkForUpdate(currentVersion: String): Result<AppUpdateInfo?>
    suspend fun fetchChangelog(versionTag: String): Result<String?>
}
