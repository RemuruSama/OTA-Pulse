package com.abhinav.otapulse.core.download

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.abhinav.otapulse.core.model.OtaUpdate
import com.abhinav.otapulse.core.network.OtaResolver
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp-based download engine.
 *
 * Replaces the Fetch2 (`com.tonyodev.fetch2`) library. Supports:
 *  - Parallel downloads (max 3) via Semaphore
 *  - Pause / resume via HTTP Range headers (byte offset persisted)
 *  - Auto-retry on network errors and expired signed URLs (403/410/416)
 *  - Transparent URL refresh via [OtaResolver]
 *  - Persistent queue backed by SharedPreferences (survives process death)
 *  - Listener callbacks dispatched on the main thread
 */
@Singleton
class OkHttpDownloadEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Guards concurrent access to [records]. */
    private val lock = Any()

    /** In-memory store; source-of-truth is always synced to prefs. */
    private val records = ConcurrentHashMap<Int, DownloadRecord>()

    /** Active download jobs; keyed by download ID. */
    private val activeJobs = ConcurrentHashMap<Int, kotlinx.coroutines.Job>()

    /** Whether a given download has been explicitly pause-requested. */
    private val pauseRequested = ConcurrentHashMap<Int, Boolean>()

    /** Limits simultaneous downloads to 3. */
    private val semaphore = Semaphore(3)

    private val listeners = mutableListOf<DownloadListener>()

    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val nextId = AtomicInteger(0)

    init {
        loadPersistedRecords()
        // Seed nextId above the highest persisted ID so we never collide.
        val maxId = records.keys.maxOrNull() ?: 0
        nextId.set(maxId + 1)
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun addListener(listener: DownloadListener) {
        synchronized(lock) { listeners.add(listener) }
    }

    fun removeListener(listener: DownloadListener) {
        synchronized(lock) { listeners.remove(listener) }
    }

    /**
     * Enqueue a new download. Returns the assigned download ID.
     *
     * @param url         Resolved CDN URL.
     * @param filePath    Absolute path of the target file on disk.
     * @param extras      Arbitrary metadata (deviceName, otaUpdate JSON, …)
     */
    fun enqueue(
        url: String,
        filePath: String,
        extras: Map<String, String> = emptyMap()
    ): Int {
        val id = nextId.getAndIncrement()
        val record = DownloadRecord(
            id = id,
            url = url,
            file = filePath,
            status = DownloadStatus.QUEUED,
            error = DownloadError.NONE,
            downloaded = 0L,
            total = -1L,
            downloadedBytesPerSecond = 0L,
            etaInMilliSeconds = -1L,
            progress = 0,
            created = System.currentTimeMillis(),
            extras = extras
        )
        updateRecord(record)
        dispatch { it.onAdded(record) }
        dispatch { it.onQueued(record, waitingOnNetwork = false) }
        startWorker(id)
        return id
    }

    fun pause(id: Int) {
        pauseRequested[id] = true
        activeJobs[id]?.cancel()
    }

    fun resume(id: Int) {
        pauseRequested.remove(id)
        val rec = getRecord(id) ?: return
        if (rec.status != DownloadStatus.PAUSED && rec.status != DownloadStatus.FAILED) return
        updateRecord(rec.copy(status = DownloadStatus.QUEUED, error = DownloadError.NONE))
        startWorker(id)
    }

    fun cancel(id: Int) {
        pauseRequested.remove(id)
        activeJobs[id]?.cancel()
        val rec = getRecord(id) ?: return
        updateRecord(rec.copy(status = DownloadStatus.CANCELLED))
        dispatch { it.onCancelled(rec.copy(status = DownloadStatus.CANCELLED)) }
    }

    fun retry(id: Int) {
        val rec = getRecord(id) ?: return
        // Do not reset downloaded bytes. Allow the engine to resume from existing file.
        // It will automatically handle 416 Range Not Satisfiable if the file is stale.
        updateRecord(
            rec.copy(
                status = DownloadStatus.QUEUED,
                error = DownloadError.NONE,
                downloadedBytesPerSecond = 0L,
                etaInMilliSeconds = -1L
            )
        )
        startWorker(id)
    }

    fun delete(id: Int) {
        activeJobs[id]?.cancel()
        pauseRequested.remove(id)
        val rec = getRecord(id) ?: return
        synchronized(lock) { records.remove(id) }
        persistRecords()
        dispatch { it.onDeleted(rec) }
    }

    /** Returns a snapshot list of all known [DownloadRecord]s, newest first. */
    fun getDownloads(): List<DownloadRecord> =
        synchronized(lock) { records.values.toList() }
            .sortedByDescending { it.created }

    /** Returns the [DownloadRecord] for [id], or null if not found. */
    fun getDownload(id: Int): DownloadRecord? = getRecord(id)

    // ── Worker ────────────────────────────────────────────────────────────────

    private fun startWorker(id: Int) {
        val job = ioScope.launch {
            semaphore.withPermit {
                runDownload(id)
            }
        }
        activeJobs[id] = job
        job.invokeOnCompletion { activeJobs.remove(id) }
    }

    private suspend fun runDownload(id: Int) {
        val rec = getRecord(id) ?: return

        // ── Transition to DOWNLOADING ────────────────────────────────────────
        val startedRec = rec.copy(status = DownloadStatus.DOWNLOADING)
        updateRecord(startedRec)
        dispatch { it.onStarted(startedRec) }

        var currentUrl = rec.url
        var retryCount = 0

        try {
            while (retryCount <= MAX_RETRIES) {
                // Check for pause/cancel between retries.
                if (pauseRequested[id] == true || !isActive(id)) {
                    handlePause(id)
                    return
                }

                val result = attemptDownload(id, currentUrl)

                when (result) {
                    is DownloadResult.Success -> {
                        val finalRec = (getRecord(id) ?: return).copy(
                            status = DownloadStatus.COMPLETED,
                            progress = 100,
                            error = DownloadError.NONE
                        )
                        updateRecord(finalRec)
                        dispatch { it.onCompleted(finalRec) }
                        return
                    }
                    is DownloadResult.Paused -> {
                        handlePause(id)
                        return
                    }
                    is DownloadResult.ExpiredUrl -> {
                        Log.w(TAG, "[$id] URL expired (${result.httpCode}), refreshing... attempt ${retryCount + 1}")
                        val refreshed = tryRefreshUrl(id, rec.extras)
                        if (refreshed != null && refreshed != currentUrl) {
                            currentUrl = refreshed
                            // Update URL in record so resume also uses it.
                            updateRecord((getRecord(id) ?: return).copy(url = currentUrl))
                            retryCount++
                            continue
                        } else {
                            // Can't refresh → treat as a real error.
                            reportError(id, DownloadError.REQUEST_NOT_SUCCESSFUL, null)
                            return
                        }
                    }
                    is DownloadResult.NetworkError -> {
                        retryCount++
                        if (retryCount > MAX_RETRIES) {
                            reportError(id, result.error, result.throwable)
                            return
                        }
                        Log.w(TAG, "[$id] Network error: ${result.throwable?.message}, retry $retryCount/$MAX_RETRIES")
                        kotlinx.coroutines.delay(RETRY_DELAY_MS * retryCount)
                    }
                    is DownloadResult.FatalError -> {
                        reportError(id, result.error, result.throwable)
                        return
                    }
                }
            }

            reportError(id, DownloadError.UNKNOWN, null)
        } catch (e: kotlinx.coroutines.CancellationException) {
            if (pauseRequested[id] == true) {
                handlePause(id)
            }
            throw e
        } catch (e: Exception) {
            reportError(id, DownloadError.UNKNOWN_IO_ERROR, e)
        }
    }

    private suspend fun attemptDownload(id: Int, url: String): DownloadResult {
        val rec = getRecord(id) ?: return DownloadResult.FatalError(DownloadError.UNKNOWN, null)
        val targetFile = File(rec.file)
        targetFile.parentFile?.mkdirs()

        // Resume from existing bytes if file is partially written.
        val resumeOffset = if (targetFile.exists()) targetFile.length() else 0L

        val conn: HttpURLConnection
        try {
            conn = openConnection(url)
            if (resumeOffset > 0L) {
                conn.setRequestProperty("Range", "bytes=$resumeOffset-")
            }
        } catch (e: Exception) {
            return DownloadResult.NetworkError(DownloadError.UNKNOWN_IO_ERROR, e)
        }

        return withContext(Dispatchers.IO) {
            try {
                conn.connect()
                val code = conn.responseCode

                Log.d(TAG, "[$id] HTTP $code for ${url.take(80)}…")

                // 416 = Range Not Satisfiable → offset stale, delete partial file and restart.
                if (code == 416) {
                    Log.w(TAG, "[$id] 416 Range Not Satisfiable — deleting partial file and restarting")
                    conn.disconnect()
                    targetFile.takeIf { it.exists() }?.delete()
                    return@withContext DownloadResult.NetworkError(DownloadError.REQUEST_NOT_SUCCESSFUL, null)
                }

                // 403/410 → expired signed URL → caller will attempt URL refresh.
                if (code == 403 || code == 410) {
                    Log.w(TAG, "[$id] HTTP $code — will attempt URL refresh")
                    conn.disconnect()
                    return@withContext DownloadResult.ExpiredUrl(code)
                }

                if (code != 200 && code != 206) {
                    Log.e(TAG, "[$id] Non-retryable HTTP $code")
                    conn.disconnect()
                    return@withContext DownloadResult.FatalError(DownloadError.fromHttpCode(code), null)
                }

                val contentLength = conn.contentLengthLong
                val totalBytes = when {
                    code == 206 && resumeOffset > 0 -> resumeOffset + contentLength
                    contentLength > 0 -> contentLength
                    else -> -1L
                }

                if (totalBytes > 0) {
                    updateRecord(
                        (getRecord(id) ?: run {
                            conn.disconnect()
                            return@withContext DownloadResult.FatalError(DownloadError.UNKNOWN, null)
                        }).copy(total = totalBytes)
                    )
                }

                val seekOffset = if (code == 206 && resumeOffset > 0) resumeOffset else 0L
                val raf = try {
                    RandomAccessFile(targetFile, "rw").also { it.seek(seekOffset) }
                } catch (e: Exception) {
                    conn.disconnect()
                    return@withContext DownloadResult.FatalError(DownloadError.UNKNOWN_IO_ERROR, e)
                }

                try {
                    streamToFile(id, raf, conn.inputStream, seekOffset, totalBytes)
                } finally {
                    raf.close()
                    conn.disconnect()
                }

            } catch (e: UnknownHostException) {
                conn.disconnect()
                DownloadResult.NetworkError(DownloadError.NO_NETWORK_CONNECTION, e)
            } catch (e: SocketTimeoutException) {
                conn.disconnect()
                DownloadResult.NetworkError(DownloadError.CONNECTION_TIMED_OUT, e)
            } catch (e: Exception) {
                conn.disconnect()
                if (pauseRequested[id] == true) DownloadResult.Paused
                else DownloadResult.NetworkError(DownloadError.UNKNOWN_IO_ERROR, e)
            }
        }
    }

    private fun openConnection(url: String): HttpURLConnection {
        return (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 30_000
            readTimeout = 60_000           // 60-second read timeout to prevent holding threads/semaphores forever
            requestMethod = "GET"

            if (com.abhinav.otapulse.feature.downloads.data.DownloadManager.isDownloadCheckUrl(url)) {
                setRequestProperty("Accept-Encoding", "identity")
                setRequestProperty("User-Agent", "okhttp/3.12.12")
                setRequestProperty("userId", "oplus-ota|16002018")
                setRequestProperty("Accept", "*/*")
                setRequestProperty("Connection", "Keep-Alive")
                setRequestProperty("Cache-Control", "no-cache")
            }
        }
    }

    private suspend fun streamToFile(
        id: Int,
        raf: RandomAccessFile,
        input: InputStream,
        initialOffset: Long,
        totalBytes: Long
    ): DownloadResult {
        val buffer = ByteArray(BUFFER_SIZE)
        var written = initialOffset
        var lastProgressReport = System.currentTimeMillis()
        var lastWritten = written
        var speedBytesPerSec = 0L

        return try {
            val loopResult = withContext(Dispatchers.IO) {
                while (isActive) {
                    if (pauseRequested[id] == true) return@withContext DownloadResult.Paused
                    val read = input.read(buffer)
                    if (read == -1) break
                    raf.write(buffer, 0, read)
                    written += read

                    val now = System.currentTimeMillis()
                    val elapsed = now - lastProgressReport
                    if (elapsed >= PROGRESS_INTERVAL_MS) {
                        val deltaBytes = written - lastWritten
                        speedBytesPerSec = if (elapsed > 0) deltaBytes * 1000 / elapsed else 0L
                        lastWritten = written
                        lastProgressReport = now

                        val progress = if (totalBytes > 0)
                            ((written.toDouble() / totalBytes) * 100).toInt().coerceIn(0, 100)
                        else 0
                        val eta = if (speedBytesPerSec > 0 && totalBytes > 0)
                            ((totalBytes - written) * 1000L / speedBytesPerSec)
                        else -1L

                        val updatedRec = (getRecord(id) ?: return@withContext DownloadResult.FatalError(DownloadError.UNKNOWN, null)).copy(
                            downloaded = written,
                            total = if (totalBytes > 0) totalBytes else -1L,
                            downloadedBytesPerSecond = speedBytesPerSec,
                            etaInMilliSeconds = eta,
                            progress = progress
                        )
                        updateRecord(updatedRec)
                        dispatch { it.onProgress(updatedRec, eta, speedBytesPerSec) }
                    }
                }
                null
            }

            if (loopResult is DownloadResult.Paused) return loopResult
            if (loopResult is DownloadResult.FatalError) return loopResult

            val finalProgress = if (totalBytes > 0)
                ((written.toDouble() / totalBytes) * 100).toInt().coerceIn(0, 100)
            else 100
            val completedRec = (getRecord(id) ?: return DownloadResult.FatalError(DownloadError.UNKNOWN, null)).copy(
                downloaded = written,
                progress = finalProgress
            )
            updateRecord(completedRec)
            DownloadResult.Success
        } catch (e: Exception) {
            if (pauseRequested[id] == true) DownloadResult.Paused
            else DownloadResult.NetworkError(DownloadError.UNKNOWN_IO_ERROR, e)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun isActive(id: Int): Boolean {
        val rec = getRecord(id) ?: return false
        return rec.status != DownloadStatus.CANCELLED && rec.status != DownloadStatus.COMPLETED
    }

    private fun handlePause(id: Int) {
        val rec = getRecord(id) ?: return
        pauseRequested.remove(id)
        val pausedRec = rec.copy(status = DownloadStatus.PAUSED)
        updateRecord(pausedRec)
        dispatch { it.onPaused(pausedRec) }
    }

    private fun reportError(id: Int, error: DownloadError, t: Throwable?) {
        val rec = getRecord(id) ?: return
        val errorRec = rec.copy(status = DownloadStatus.FAILED, error = error)
        updateRecord(errorRec)
        dispatch { it.onError(errorRec, error, t) }
        Log.e(TAG, "[$id] Download failed: $error - ${t?.message}")
    }

    private suspend fun tryRefreshUrl(id: Int, extras: Map<String, String>): String? {
        val otaUpdateStr = extras["otaUpdate"] ?: return null
        val otaUpdate = OtaUpdate.fromString(otaUpdateStr) ?: return null
        val downloadUrl = otaUpdate.downloadUrl

        // Only `downloadCheck` API URLs can be refreshed via OtaResolver.
        // Direct CDN URLs (signed Gauss/Aliyun allawnfs.com URLs) are time-limited and
        // cannot be re-signed here — the user must re-query the OTA server to get a new one.
        if (!com.abhinav.otapulse.feature.downloads.data.DownloadManager.isDownloadCheckUrl(downloadUrl)) {
            Log.w(TAG, "[$id] Cannot refresh direct CDN URL — user must re-query the OTA server")
            return null
        }

        return try {
            withContext(Dispatchers.IO) {
                OtaResolver.resolveUrl(downloadUrl).url
            }
        } catch (e: Exception) {
            Log.e(TAG, "[$id] URL refresh failed: ${e.message}")
            null
        }
    }

    private fun getRecord(id: Int): DownloadRecord? = records[id]

    private fun updateRecord(record: DownloadRecord) {
        records[record.id] = record
        persistRecords()
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private fun persistRecords() {
        ioScope.launch {
            try {
                val snapshot = synchronized(lock) { records.values.toList() }
                // Only persist terminal or paused states to survive process death.
                val toSave = snapshot.filter {
                    it.status in setOf(
                        DownloadStatus.PAUSED,
                        DownloadStatus.COMPLETED,
                        DownloadStatus.FAILED,
                        DownloadStatus.CANCELLED
                    )
                }.map { record ->
                    // Store current byte offset so resume works correctly.
                    val diskBytes = File(record.file).takeIf { it.exists() }?.length() ?: record.downloaded
                    record.copy(downloaded = diskBytes)
                }
                prefs.edit().putString(KEY_RECORDS, gson.toJson(toSave)).apply()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist download records", e)
            }
        }
    }

    private fun loadPersistedRecords() {
        try {
            val json = prefs.getString(KEY_RECORDS, null) ?: return
            val type = object : TypeToken<List<DownloadRecord>>() {}.type
            val loaded: List<DownloadRecord> = gson.fromJson(json, type) ?: return
            loaded.forEach { rec ->
                // Rehydrate in-progress → PAUSED so the user can resume.
                val status = when (rec.status) {
                    DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED, DownloadStatus.ADDED ->
                        DownloadStatus.PAUSED
                    else -> rec.status
                }
                records[rec.id] = rec.copy(status = status)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load persisted download records", e)
        }
    }

    // ── Dispatcher ────────────────────────────────────────────────────────────

    private fun dispatch(block: (DownloadListener) -> Unit) {
        val snapshot = synchronized(lock) { listeners.toList() }
        mainScope.launch { snapshot.forEach { block(it) } }
    }

    // ── Result types ──────────────────────────────────────────────────────────

    private sealed interface DownloadResult {
        object Success : DownloadResult
        object Paused : DownloadResult
        data class ExpiredUrl(val httpCode: Int) : DownloadResult
        data class NetworkError(val error: DownloadError, val throwable: Throwable?) : DownloadResult
        data class FatalError(val error: DownloadError, val throwable: Throwable?) : DownloadResult
    }

    // ── Constants ─────────────────────────────────────────────────────────────

    companion object {
        private const val TAG = "OkHttpDownloadEngine"
        private const val PREFS_NAME = "okhttp_download_engine"
        private const val KEY_RECORDS = "records"
        private const val BUFFER_SIZE = 8 * 1024      // 8 KB
        private const val PROGRESS_INTERVAL_MS = 500L  // Emit progress every 500 ms
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1_500L
    }
}
