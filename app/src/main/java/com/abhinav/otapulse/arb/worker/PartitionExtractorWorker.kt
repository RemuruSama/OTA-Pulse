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
import chromeos_update_engine.UpdateMetadata.InstallOperation
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
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
        const val KEY_PARTITION_NAMES = "partition_names"
        const val KEY_VERSION_NAME = "version_name"
        const val KEY_REGION_NAME = "region_name"
        
        const val PROGRESS_KEY = "progress"
        const val PROGRESS_MAX_KEY = "progress_max"
        const val CURRENT_PARTITION_KEY = "current_partition"

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
        val partitionNames = inputData.getStringArray(KEY_PARTITION_NAMES) ?: return@withContext Result.failure()
        if (partitionNames.isEmpty()) return@withContext Result.failure()

        val versionName = sanitizeFolderSegment(inputData.getString(KEY_VERSION_NAME) ?: "Unknown")
        val regionName = inputData.getString(KEY_REGION_NAME).orEmpty()
        val baseDir = File(Environment.getExternalStorageDirectory(), com.abhinav.otapulse.core.network.Component.OTA_UPDATES_DIR)
        val extractedDir = File(baseDir, "Extracted").also { it.mkdirs() }
        val regionFolderPrefix = sanitizeFolderSegment(regionName).takeIf { regionName.isNotBlank() }.orEmpty()
        val folderName = if (regionFolderPrefix.isBlank()) versionName else "$regionFolderPrefix-$versionName"
        val targetFolder = File(extractedDir, folderName).also { it.mkdirs() }

        val titleName = if (partitionNames.size == 1) partitionNames[0] else "${partitionNames.size} partitions"
        createNotificationChannel()
        setForeground(createForegroundInfo(titleName, 0, 100))

        try {
            Log.d("PartitionExtractor", "Starting extraction for $titleName from $source")
            
            // 1. Open session
            val session = otaExtractor.open(source)

            // 2. Extract with progress
            val totalPartitions = partitionNames.size
            for ((index, partitionName) in partitionNames.withIndex()) {
                val outputFile = File(targetFolder, "$partitionName.img")
                
                // If cancelled before starting the next partition, throw CancellationException
                kotlinx.coroutines.yield()

                // Check if session needs a source file for this partition
                if (session.sourceFile == null && isIncrementalPartition(session, partitionName)) {
                    Log.d("PartitionExtractor", "Partition $partitionName is incremental, searching for source...")
                    val discoveredSource = findSourcePartition(extractedDir, partitionName)
                    if (discoveredSource != null) {
                        Log.i("PartitionExtractor", "Auto-discovered source for $partitionName: ${discoveredSource.absolutePath}")
                        session.sourceFile = discoveredSource
                    }
                }

                try {
                    otaExtractor.extractToFile(session, partitionName, outputFile) { state ->
                        updateProgress(titleName, partitionName, state, index, totalPartitions)
                    }
                } finally {
                    session.sourceFile = null
                }

                if (totalPartitions > 1) {
                    showSinglePartitionSuccessNotification(partitionName, index)
                }
            }

            // Finalize
            setProgress(workDataOf(PROGRESS_KEY to 100, PROGRESS_MAX_KEY to 100))
            showSuccessNotification(titleName, partitionNames, targetFolder.absolutePath)

            Result.success()
        } catch (e: CancellationException) {
            Log.i("PartitionExtractor", "Extraction cancelled for $titleName")
            partitionNames.forEach { otaExtractor.clearExtractionState(it) }
            showCancelledNotification(titleName)
            throw e
        } catch (e: Exception) {
            Log.e("PartitionExtractor", "Extraction failed for $titleName", e)
            partitionNames.forEach { otaExtractor.clearExtractionState(it) }
            showErrorNotification(titleName)
            Result.failure()
        }
    }

    private suspend fun updateProgress(titleName: String, currentPartition: String, state: ExtractionState, currentIndex: Int, totalPartitions: Int) {
        val individualProgress = state.progressPercent
        val overallProgress = ((currentIndex * 100) + individualProgress) / totalPartitions
        
        val currentTime = System.currentTimeMillis()
        
        setProgress(workDataOf(
            PROGRESS_KEY to overallProgress,
            PROGRESS_MAX_KEY to 100,
            CURRENT_PARTITION_KEY to currentPartition
        ))
        
        // Update notification occasionally to avoid spamming SystemUI (2% change AND 1000ms time)
        if (overallProgress % 2 == 0 && (currentTime - lastNotificationTime >= throttleIntervalMs)) {
            lastNotificationTime = currentTime
            val contentText = if (totalPartitions > 1) {
                "$overallProgress% • $currentPartition"
            } else {
                "$overallProgress%"
            }
            setForeground(createForegroundInfo(titleName, overallProgress, 100, contentText))
        }
    }

    private fun createForegroundInfo(
        titleName: String, 
        progress: Int, 
        max: Int, 
        contentText: String? = null
    ): ForegroundInfo {
        val cancelPendingIntent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(id)
        val title = applicationContext.getString(R.string.worker_extracting_title, titleName)
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

    private fun showSuccessNotification(titleName: String, partitionNames: Array<String>, filePath: String) {
        val contentText = "$titleName saved successfully"
        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Extraction Complete")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_download)
            .setAutoCancel(true)

        if (partitionNames.size > 1) {
            val namesStr = partitionNames.joinToString(", ")
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(applicationContext.getString(R.string.worker_extracted_msg, namesStr)))
        }

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID + 1, builder.build())
    }

    private fun showSinglePartitionSuccessNotification(partitionName: String, index: Int) {
        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Partition Extracted")
            .setContentText(applicationContext.getString(R.string.worker_saved_success, partitionName))
            .setSmallIcon(R.drawable.ic_download)
            .setAutoCancel(true)

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID + 10 + index, builder.build())
    }

    private fun showErrorNotification(titleName: String) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Extraction Failed")
            .setContentText(applicationContext.getString(R.string.worker_failed_extract, titleName))
            .setSmallIcon(R.drawable.ic_download)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID + 2, notification)
    }

    private fun showCancelledNotification(titleName: String) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Extraction Cancelled")
            .setContentText(applicationContext.getString(R.string.worker_extraction_cancelled, titleName))
            .setSmallIcon(R.drawable.ic_cancel_circle)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID + 3, notification)
    }

    private fun sanitizeFolderSegment(value: String): String {
        return value
            .trim()
            .replace(Regex("[\\\\/:*?\"<>|]+"), "_")
            .replace(Regex("\\s+"), " ")
            .trim('.')
            .ifBlank { "Unknown" }
    }

    private fun isIncrementalPartition(session: OtaExtractor.Session, partitionName: String): Boolean {
        val partition = session.manifest.partitionsList.find { it.partitionName == partitionName }
            ?: return false
        
        return partition.operationsList.any { op ->
            op.type == InstallOperation.Type.SOURCE_COPY ||
            op.type == InstallOperation.Type.MOVE ||
            op.type == InstallOperation.Type.SOURCE_BSDIFF ||
            op.type == InstallOperation.Type.BROTLI_BSDIFF
        }
    }

    private fun findSourcePartition(extractedDir: File, partitionName: String): File? {
        // Search all subdirectories for {partitionName}.img
        // We prioritize more recent ones if possible, but any match is better than none.
        val candidates = mutableListOf<File>()
        extractedDir.listFiles()?.filter { it.isDirectory }?.forEach { subDir ->
            val file = File(subDir, "$partitionName.img")
            if (file.exists() && file.length() > 0) {
                candidates.add(file)
            }
        }
        
        // Sort by last modified to get the most recent one
        return candidates.maxByOrNull { it.lastModified() }
    }
}
