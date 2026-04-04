package com.abhinav.otapulse.arb.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.abhinav.otapulse.R
import com.abhinav.otapulse.ota.engine.OtaExtractor
import com.abhinav.otapulse.ota.resume.ExtractionState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import java.io.File

@HiltWorker
class PartitionExtractorWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_SOURCE = "source"
        const val KEY_URL = "url"
        const val KEY_PARTITION_NAME = "partition_name"
        const val KEY_VERSION_NAME = "version_name"
        
        const val PROGRESS_KEY = "progress"
        const val PROGRESS_MAX_KEY = "progress_max"

        private const val NOTIFICATION_ID = 4050
        private const val CHANNEL_ID = "partition_extraction_channel"
    }

    private val otaExtractor = OtaExtractor(applicationContext)
    private var lastNotificationTime = 0L
    private val throttleIntervalMs = 1000L

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val source = inputData.getString(KEY_SOURCE)
            ?: inputData.getString(KEY_URL)
            ?: return@withContext Result.failure()
        val partitionName = inputData.getString(KEY_PARTITION_NAME) ?: return@withContext Result.failure()
        val versionName = inputData.getString(KEY_VERSION_NAME) ?: "Unknown"
        val baseDir = File(Environment.getExternalStorageDirectory(), com.abhinav.otapulse.core.network.Component.OTA_UPDATES_DIR)
        val extractedDir = File(baseDir, "Extracted").also { it.mkdirs() }
        val targetFolder = File(extractedDir, versionName).also { it.mkdirs() }
        val outputFile = File(targetFolder, "$partitionName.img")

        createNotificationChannel()
        setForeground(createForegroundInfo(partitionName, 0, 100))

        try {
            Log.d("PartitionExtractor", "Starting extraction for $partitionName from $source")
            
            // 1. Open session
            val session = otaExtractor.open(source)

            // 2. Extract with progress
            otaExtractor.extractToFile(session, partitionName, outputFile) { state ->
                updateProgress(partitionName, state)
            }

            // Finalize
            setProgress(workDataOf(PROGRESS_KEY to 100, PROGRESS_MAX_KEY to 100))
            showSuccessNotification(partitionName, outputFile.absolutePath)

            Result.success()
        } catch (e: CancellationException) {
            Log.i("PartitionExtractor", "Extraction cancelled for $partitionName")
            otaExtractor.clearExtractionState(partitionName)
            if (outputFile.exists()) {
                outputFile.delete()
            }
            showCancelledNotification(partitionName)
            throw e
        } catch (e: Exception) {
            Log.e("PartitionExtractor", "Extraction failed for $partitionName", e)
            otaExtractor.clearExtractionState(partitionName)
            showErrorNotification(partitionName)
            Result.failure()
        }
    }

    private suspend fun updateProgress(partitionName: String, state: ExtractionState) {
        val progress = state.progressPercent
        val currentTime = System.currentTimeMillis()
        
        setProgress(workDataOf(
            PROGRESS_KEY to progress,
            PROGRESS_MAX_KEY to 100
        ))
        
        // Update notification occasionally to avoid spamming SystemUI (2% change AND 1000ms time)
        if (progress % 2 == 0 && (currentTime - lastNotificationTime >= throttleIntervalMs)) {
            lastNotificationTime = currentTime
            setForeground(createForegroundInfo(partitionName, progress, 100, state.formattedProgress))
        }
    }

    private fun createForegroundInfo(
        partitionName: String, 
        progress: Int, 
        max: Int, 
        contentText: String? = null
    ): ForegroundInfo {
        val cancelPendingIntent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(id)
        val title = "Extracting $partitionName.img"
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText ?: "$progress%")
            .setSmallIcon(R.drawable.ic_download)
            .setProgress(max, progress, false)
            .addAction(R.drawable.ic_cancel_circle, "Cancel", cancelPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Partition Extraction",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress for OTA partition extraction"
            }
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun showSuccessNotification(partitionName: String, filePath: String) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Extraction Complete")
            .setContentText("$partitionName.img saved successfully")
            .setSmallIcon(R.drawable.ic_download)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun showErrorNotification(partitionName: String) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Extraction Failed")
            .setContentText("Failed to extract $partitionName.img")
            .setSmallIcon(R.drawable.ic_download)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID + 2, notification)
    }

    private fun showCancelledNotification(partitionName: String) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Extraction Cancelled")
            .setContentText("$partitionName.img extraction was cancelled")
            .setSmallIcon(R.drawable.ic_cancel_circle)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID + 3, notification)
    }
}
