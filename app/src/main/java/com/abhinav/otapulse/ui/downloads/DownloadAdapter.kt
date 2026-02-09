package com.abhinav.otapulse.ui.downloads

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.abhinav.otapulse.R
import com.abhinav.otapulse.databinding.ListItemDownloadBinding
import com.abhinav.otapulse.domain.model.DownloadInfo
import com.abhinav.otapulse.util.FormatUtils
import com.abhinav.otapulse.util.setHapticClickListener
import com.tonyodev.fetch2.Status
import java.util.concurrent.TimeUnit

class DownloadAdapter(
    private val onPause: (DownloadInfo) -> Unit,
    private val onResume: (DownloadInfo) -> Unit,
    private val onCancel: (DownloadInfo) -> Unit,
    private val onRetry: (DownloadInfo) -> Unit,
    private val onDelete: (DownloadInfo) -> Unit
) : ListAdapter<DownloadInfo, DownloadAdapter.DownloadViewHolder>(DownloadDiffCallback()) {

    companion object {
        private const val UNKNOWN_DEVICE = "Unknown Device"
        private const val UNKNOWN_REGION = "Unknown Region"
        private const val CUSTOM_DEVICE_TEXT = "Custom Device"
        private const val DIRECT_DOWNLOAD = "Direct Download"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadViewHolder {
        val binding = ListItemDownloadBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DownloadViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DownloadViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DownloadViewHolder(private val binding: ListItemDownloadBinding) : RecyclerView.ViewHolder(binding.root) {
        private val context: Context = binding.root.context

        fun bind(downloadInfo: DownloadInfo) {
            val isManualDownload = downloadInfo.deviceName == DIRECT_DOWNLOAD || downloadInfo.deviceName == UNKNOWN_DEVICE

            if (isManualDownload) {
                val infoText = if (downloadInfo.regionName.isNotBlank() && downloadInfo.regionName != UNKNOWN_REGION) {
                    "${downloadInfo.deviceName} (${downloadInfo.regionName})"
                } else {
                    downloadInfo.deviceName
                }
                binding.deviceInfoTextView.text = if (downloadInfo.deviceName == UNKNOWN_DEVICE) CUSTOM_DEVICE_TEXT else infoText
                binding.fileNameTextView.text = downloadInfo.fileName
            } else {
                binding.fileNameTextView.text = "${downloadInfo.regionName}-${downloadInfo.otaUpdate?.versionName ?: "unknown_version"}.zip"
                binding.deviceInfoTextView.text = context.getString(R.string.device_region_format, downloadInfo.deviceName, downloadInfo.regionName)
            }
            
            binding.fileSizeTextView.text = context.getString(R.string.file_size_format, FormatUtils.formatSize(downloadInfo.totalBytes))
            updateProgress(downloadInfo)

            binding.actionButton.isEnabled = true
            when (downloadInfo.status) {
                Status.DOWNLOADING -> {
                    binding.actionButton.text = context.getString(R.string.pause)
                    binding.actionButton.setHapticClickListener { onPause(downloadInfo) }
                    binding.cancelButton.visibility = View.VISIBLE
                    binding.cancelButton.text = context.getString(android.R.string.cancel)
                    binding.cancelButton.setHapticClickListener { onCancel(downloadInfo) }
                }
                Status.PAUSED -> {
                    binding.actionButton.text = context.getString(R.string.resume)
                    binding.actionButton.setHapticClickListener { onResume(downloadInfo) }
                    binding.cancelButton.visibility = View.VISIBLE
                    binding.cancelButton.text = context.getString(android.R.string.cancel)
                    binding.cancelButton.setHapticClickListener { onCancel(downloadInfo) }
                }
                Status.COMPLETED -> {
                    binding.actionButton.text = context.getString(R.string.completed)
                    binding.actionButton.isEnabled = false
                    binding.cancelButton.visibility = View.VISIBLE
                    binding.cancelButton.text = context.getString(R.string.delete)
                    binding.cancelButton.setHapticClickListener { onDelete(downloadInfo) }
                }
                Status.FAILED, Status.CANCELLED -> {
                    binding.actionButton.text = context.getString(R.string.retry)
                    binding.actionButton.setHapticClickListener { onRetry(downloadInfo) }
                    binding.cancelButton.visibility = View.VISIBLE
                    binding.cancelButton.text = context.getString(R.string.delete)
                    binding.cancelButton.setHapticClickListener { onDelete(downloadInfo) }
                }
                else -> { // Queued, Added, None
                    binding.actionButton.text = context.getString(R.string.waiting)
                    binding.actionButton.isEnabled = false
                    binding.cancelButton.visibility = View.VISIBLE
                    binding.cancelButton.text = context.getString(android.R.string.cancel)
                    binding.cancelButton.setHapticClickListener { onCancel(downloadInfo) }
                }
            }
        }

        private fun updateProgress(downloadInfo: DownloadInfo) {
            binding.downloadProgressBar.progress = downloadInfo.progress
            binding.downloadProgressText.text = context.getString(R.string.download_progress_percent, downloadInfo.progress)
            val isDownloading = downloadInfo.status == Status.DOWNLOADING
            binding.downloadSpeedText.isVisible = isDownloading
            binding.downloadEtaText.isVisible = isDownloading
            if (isDownloading) {
                binding.downloadSpeedText.text = FormatUtils.formatDownloadSpeed(downloadInfo.speed)
                binding.downloadEtaText.text = formatEta(downloadInfo.eta)
            }
        }

        private fun formatEta(etaMillis: Long): String {
            if (etaMillis < 0 || etaMillis == Long.MAX_VALUE) return ""
            val seconds = TimeUnit.MILLISECONDS.toSeconds(etaMillis)
            val hours = TimeUnit.SECONDS.toHours(seconds)
            val minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60
            return when {
                hours > 0 -> context.getString(R.string.eta_hours_minutes, hours, minutes)
                minutes > 0 -> context.getString(R.string.eta_minutes_seconds, minutes, seconds % 60)
                else -> context.getString(R.string.eta_seconds, seconds)
            }
        }
    }

    class DownloadDiffCallback : DiffUtil.ItemCallback<DownloadInfo>() {
        override fun areItemsTheSame(oldItem: DownloadInfo, newItem: DownloadInfo): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: DownloadInfo, newItem: DownloadInfo): Boolean = oldItem == newItem
    }
}
