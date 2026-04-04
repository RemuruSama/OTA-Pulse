package com.abhinav.otapulse.core.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.abhinav.otapulse.core.model.OtaUpdate
import com.abhinav.otapulse.feature.downloads.domain.DownloadRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val downloadRepository: DownloadRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val otaUpdateJson = inputData.getString(KEY_OTA_UPDATE)
        val deviceName = inputData.getString(KEY_DEVICE_NAME)
        val regionName = inputData.getString(KEY_REGION_NAME)

        if (otaUpdateJson.isNullOrEmpty() || deviceName.isNullOrEmpty() || regionName.isNullOrEmpty()) {
            Log.e("DownloadWorker", "Invalid input data")
            return Result.failure()
        }

        val otaUpdate = OtaUpdate.fromString(otaUpdateJson)
        if (otaUpdate == null) {
            Log.e("DownloadWorker", "Failed to parse OtaUpdate from JSON")
            return Result.failure()
        }

        return try {
            downloadRepository.enqueueDownload(otaUpdate, deviceName, regionName)
            Result.success()
        } catch (e: Exception) {
            Log.e("DownloadWorker", "Failed to enqueue download via repository", e)
            Result.failure()
        }
    }

    companion object {
        const val KEY_OTA_UPDATE = "key_ota_update"
        const val KEY_DEVICE_NAME = "key_device_name"
        const val KEY_REGION_NAME = "key_region_name"
    }
}
