package com.abhinav.otapulse.feature.downloads.ui

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.abhinav.otapulse.R
import com.abhinav.otapulse.databinding.ListItemDownloadBinding
import com.abhinav.otapulse.core.model.DownloadInfo
import com.abhinav.otapulse.core.common.FormatUtils
import com.abhinav.otapulse.core.common.setHapticClickListener
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
            } else {
                binding.deviceInfoTextView.text = context.getString(R.string.device_region_format, downloadInfo.deviceName, downloadInfo.regionName)
            }

            // Bind OTA Version Name if available
            val otaVersion = downloadInfo.otaUpdate?.versionName
            binding.otaVersionTextView.isVisible = !otaVersion.isNullOrBlank()
            binding.otaVersionTextView.text = otaVersion

            // Always show the actual filename from disk / server URL
            binding.fileNameTextView.text = downloadInfo.fileName.ifBlank {
                try {
                    java.net.URL(downloadInfo.original.url).path
                        .substringAfterLast('/').ifBlank { downloadInfo.original.url.substringAfterLast('/') }
                } catch (_: Exception) {
                    downloadInfo.original.url.substringAfterLast('/').substringBefore('?')
                }
            }

            
            binding.fileSizeTextView.text = context.getString(R.string.file_size_format, FormatUtils.formatSize(downloadInfo.totalBytes))
            updateProgress(downloadInfo)
            bindStatus(downloadInfo)

            binding.actionButton.isEnabled = true
            when (downloadInfo.status) {
                Status.DOWNLOADING -> {
                    binding.actionButton.text = context.getString(R.string.pause)
                    binding.actionButton.setIconResource(R.drawable.ic_pause)
                    binding.actionButton.setHapticClickListener { onPause(downloadInfo) }
                    binding.cancelButton.visibility = View.VISIBLE
                    binding.cancelButton.text = context.getString(android.R.string.cancel)
                    binding.cancelButton.setIconResource(R.drawable.ic_cancel_circle)
                    binding.cancelButton.setHapticClickListener { onCancel(downloadInfo) }
                }
                Status.PAUSED -> {
                    binding.actionButton.text = context.getString(R.string.resume)
                    binding.actionButton.setIconResource(R.drawable.ic_play_arrow)
                    binding.actionButton.setHapticClickListener { onResume(downloadInfo) }
                    binding.cancelButton.visibility = View.VISIBLE
                    binding.cancelButton.text = context.getString(android.R.string.cancel)
                    binding.cancelButton.setIconResource(R.drawable.ic_cancel_circle)
                    binding.cancelButton.setHapticClickListener { onCancel(downloadInfo) }
                }
                Status.COMPLETED -> {
                    binding.actionButton.text = context.getString(R.string.completed)
                    binding.actionButton.setIconResource(R.drawable.ic_check_circle)
                    binding.actionButton.isEnabled = false
                    binding.cancelButton.visibility = View.VISIBLE
                    binding.cancelButton.text = context.getString(R.string.delete)
                    binding.cancelButton.setIconResource(R.drawable.ic_delete)
                    binding.cancelButton.setHapticClickListener { onDelete(downloadInfo) }
                }
                Status.FAILED, Status.CANCELLED -> {
                    binding.actionButton.text = context.getString(R.string.retry)
                    binding.actionButton.setIconResource(R.drawable.ic_retry)
                    binding.actionButton.setHapticClickListener { onRetry(downloadInfo) }
                    binding.cancelButton.visibility = View.VISIBLE
                    binding.cancelButton.text = context.getString(R.string.delete)
                    binding.cancelButton.setIconResource(R.drawable.ic_delete)
                    binding.cancelButton.setHapticClickListener { onDelete(downloadInfo) }
                }
                else -> { // Queued, Added, None
                    binding.actionButton.text = context.getString(R.string.waiting)
                    binding.actionButton.setIconResource(R.drawable.ic_download)
                    binding.actionButton.isEnabled = false
                    binding.cancelButton.visibility = View.VISIBLE
                    binding.cancelButton.text = context.getString(android.R.string.cancel)
                    binding.cancelButton.setIconResource(R.drawable.ic_cancel_circle)
                    binding.cancelButton.setHapticClickListener { onCancel(downloadInfo) }
                }
            }
        }

        private fun updateProgress(downloadInfo: DownloadInfo) {
            binding.downloadProgressBar.progress = downloadInfo.progress
            binding.downloadProgressText.text = context.getString(R.string.download_progress_percent, downloadInfo.progress)
            val downloadedBytes = calculateDownloadedBytes(downloadInfo)
            binding.downloadedAmountTextView.text = context.getString(
                R.string.downloaded_amount_format,
                FormatUtils.formatSize(downloadedBytes),
                FormatUtils.formatSize(downloadInfo.totalBytes)
            )
            val isDownloading = downloadInfo.status == Status.DOWNLOADING
            binding.statsRow.isVisible = isDownloading
            if (isDownloading) {
                binding.downloadSpeedText.text = FormatUtils.formatDownloadSpeed(downloadInfo.speed)
                binding.downloadEtaText.text = formatEta(downloadInfo.eta)
            }
        }

        private fun bindStatus(downloadInfo: DownloadInfo) {
            val (label, bgColorAttr, textColorAttr) = when (downloadInfo.status) {
                Status.DOWNLOADING -> Triple(context.getString(R.string.status_downloading), com.google.android.material.R.attr.colorPrimaryContainer, com.google.android.material.R.attr.colorOnPrimaryContainer)
                Status.PAUSED -> Triple(context.getString(R.string.status_paused), com.google.android.material.R.attr.colorSecondaryContainer, com.google.android.material.R.attr.colorOnSecondaryContainer)
                Status.COMPLETED -> Triple(context.getString(R.string.status_completed), com.google.android.material.R.attr.colorTertiaryContainer, com.google.android.material.R.attr.colorOnTertiaryContainer)
                Status.FAILED -> Triple(context.getString(R.string.status_failed), com.google.android.material.R.attr.colorErrorContainer, com.google.android.material.R.attr.colorOnErrorContainer)
                Status.CANCELLED -> Triple(context.getString(R.string.status_cancelled), com.google.android.material.R.attr.colorErrorContainer, com.google.android.material.R.attr.colorOnErrorContainer)
                Status.QUEUED, Status.ADDED -> Triple(context.getString(R.string.status_queued), com.google.android.material.R.attr.colorSurfaceContainerHighest, com.google.android.material.R.attr.colorOnSurface)
                else -> Triple(context.getString(R.string.status_waiting), com.google.android.material.R.attr.colorSurfaceContainerHighest, com.google.android.material.R.attr.colorOnSurface)
            }

            binding.statusChipTextView.text = label
            binding.statusChipTextView.backgroundTintList = ColorStateList.valueOf(
                com.google.android.material.color.MaterialColors.getColor(binding.root, bgColorAttr)
            )
            binding.statusChipTextView.setTextColor(
                com.google.android.material.color.MaterialColors.getColor(binding.root, textColorAttr)
            )

            val secondaryButtonTint = ColorStateList.valueOf(
                com.google.android.material.color.MaterialColors.getColor(
                    binding.root,
                    if (downloadInfo.status == Status.DOWNLOADING || downloadInfo.status == Status.PAUSED) {
                        com.google.android.material.R.attr.colorPrimaryContainer
                    } else {
                        com.google.android.material.R.attr.colorSurfaceContainerHighest
                    }
                )
            )
            val secondaryButtonText = ColorStateList.valueOf(
                com.google.android.material.color.MaterialColors.getColor(
                    binding.root,
                    if (downloadInfo.status == Status.DOWNLOADING || downloadInfo.status == Status.PAUSED) {
                        com.google.android.material.R.attr.colorOnPrimaryContainer
                    } else {
                        com.google.android.material.R.attr.colorOnSurface
                    }
                )
            )
            binding.actionButton.backgroundTintList = secondaryButtonTint
            binding.actionButton.setTextColor(secondaryButtonText)
            binding.actionButton.iconTint = secondaryButtonText

            val destructiveStatuses = downloadInfo.status == Status.DOWNLOADING ||
                downloadInfo.status == Status.PAUSED ||
                downloadInfo.status == Status.QUEUED ||
                downloadInfo.status == Status.ADDED

            val cancelBgAttr = if (destructiveStatuses) {
                com.google.android.material.R.attr.colorErrorContainer
            } else {
                com.google.android.material.R.attr.colorSurfaceContainerHighest
            }
            val cancelTextAttr = if (destructiveStatuses) {
                com.google.android.material.R.attr.colorOnErrorContainer
            } else {
                com.google.android.material.R.attr.colorOnSurface
            }
            val cancelTint = ColorStateList.valueOf(
                com.google.android.material.color.MaterialColors.getColor(binding.root, cancelBgAttr)
            )
            val cancelTextTint = ColorStateList.valueOf(
                com.google.android.material.color.MaterialColors.getColor(binding.root, cancelTextAttr)
            )
            binding.cancelButton.backgroundTintList = cancelTint
            binding.cancelButton.setTextColor(cancelTextTint)
            binding.cancelButton.iconTint = cancelTextTint

            binding.fileSizeTextView.setTextColor(
                com.google.android.material.color.MaterialColors.getColor(
                    binding.root,
                    com.google.android.material.R.attr.colorOnSurfaceVariant
                )
            )
            binding.fileNameTextView.setTextColor(
                com.google.android.material.color.MaterialColors.getColor(
                    binding.root,
                    com.google.android.material.R.attr.colorOnSurface
                )
            )
        }

        private fun formatEta(etaMillis: Long): String {
            if (etaMillis < 0 || etaMillis == Long.MAX_VALUE) return ""
            val seconds = TimeUnit.MILLISECONDS.toSeconds(etaMillis)
            val hours = TimeUnit.SECONDS.toHours(seconds)
            val minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60
            val remainingSeconds = seconds % 60
            return when {
                hours > 0 -> context.getString(R.string.eta_hours_minutes, hours, minutes)
                minutes > 0 -> context.getString(R.string.eta_minutes_only, if (remainingSeconds >= 30) minutes + 1 else minutes)
                else -> context.getString(R.string.eta_seconds, seconds)
            }
        }

        private fun calculateDownloadedBytes(downloadInfo: DownloadInfo): Long {
            if (downloadInfo.totalBytes <= 0L) return 0L
            return (downloadInfo.totalBytes * downloadInfo.progress / 100L).coerceIn(0L, downloadInfo.totalBytes)
        }
    }

    class DownloadDiffCallback : DiffUtil.ItemCallback<DownloadInfo>() {
        override fun areItemsTheSame(oldItem: DownloadInfo, newItem: DownloadInfo): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: DownloadInfo, newItem: DownloadInfo): Boolean = oldItem == newItem
    }
}
