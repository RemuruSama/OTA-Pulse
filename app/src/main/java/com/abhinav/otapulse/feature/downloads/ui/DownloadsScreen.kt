/*
 * Copyright (C) 2026 OTA Pulse
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.abhinav.otapulse.feature.downloads.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import com.abhinav.otapulse.core.ui.ApplyDialogBlurEffect
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhinav.otapulse.R
import com.abhinav.otapulse.core.common.FormatUtils
import com.abhinav.otapulse.core.common.performHapticFeedback
import com.abhinav.otapulse.core.download.DownloadStatus
import com.abhinav.otapulse.core.model.DownloadInfo
import com.abhinav.otapulse.core.model.Md5Status
import com.abhinav.otapulse.core.ui.components.EmptyState
import com.abhinav.otapulse.core.ui.components.OtaCard
import com.abhinav.otapulse.core.ui.components.OtaOutlinedButton
import com.abhinav.otapulse.core.ui.components.OtaPrimaryButton
import com.abhinav.otapulse.core.ui.components.OtaTextField
import com.abhinav.otapulse.core.ui.components.OtaTonalButton
import com.abhinav.otapulse.core.ui.components.OtaTopAppBar
import androidx.compose.ui.tooling.preview.Preview
import com.abhinav.otapulse.core.ui.theme.OtaPulseTheme
import com.abhinav.otapulse.core.ui.theme.ThemeMode
import java.io.File
import kotlin.math.roundToInt

private enum class DragValue { Settled, Swiped }

@Composable
fun DownloadsScreen(
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val downloads by viewModel.allDownloads.collectAsState()
    DownloadsContent(
        downloads = downloads,
        modifier = modifier,
        onStartDownloadWithUrl = viewModel::startDownloadWithUrl,
        onDeleteDownload = viewModel::deleteDownload,
        onPauseDownload = viewModel::pauseDownload,
        onResumeDownload = viewModel::resumeDownload,
        onCancelDownload = viewModel::cancelDownload,
        onRetryDownload = viewModel::retryDownload
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsContent(
    downloads: List<DownloadInfo>,
    modifier: Modifier = Modifier,
    onStartDownloadWithUrl: (String) -> Unit = {},
    onDeleteDownload: (DownloadInfo) -> Unit = {},
    onPauseDownload: (DownloadInfo) -> Unit = {},
    onResumeDownload: (DownloadInfo) -> Unit = {},
    onCancelDownload: (DownloadInfo) -> Unit = {},
    onRetryDownload: (DownloadInfo) -> Unit = {}
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddDownloadDialog(
            onDismiss = { showAddDialog = false },
            onStartDownload = { url ->
                onStartDownloadWithUrl(url)
                showAddDialog = false
                Toast.makeText(context, context.getString(R.string.toast_download_started), Toast.LENGTH_SHORT).show()
            }
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            OtaTopAppBar(
                title = "Downloads",
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            val navBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            FloatingActionButton(
                onClick = {
                    context.performHapticFeedback()
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = navBarsBottom + 88.dp)
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = "Add Download")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (downloads.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.Download,
                    title = "No Downloads Active",
                    message = "Queue OTA update downloads or paste a direct payload link to monitor speed and verify MD5 integrity.",
                    actionLabel = "Add Direct Link",
                    onAction = {
                        context.performHapticFeedback()
                        showAddDialog = true
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = downloads,
                        key = { it.id }
                    ) { download ->
                        val density = LocalDensity.current
                        val swipeWidth = with(density) { 80.dp.toPx() }

                        val decaySpec = remember { exponentialDecay<Float>() }
                        val state = remember {
                            AnchoredDraggableState(
                                initialValue = DragValue.Settled,
                                anchors = DraggableAnchors {
                                    DragValue.Settled at 0f
                                    DragValue.Swiped at -swipeWidth
                                },
                                positionalThreshold = { distance -> distance * 0.3f },
                                velocityThreshold = { with(density) { 125.dp.toPx() } },
                                snapAnimationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                decayAnimationSpec = decaySpec
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max)
                                .animateItem()
                                .clip(RoundedCornerShape(16.dp))
                        ) {
                            // Delete Button (revealed behind)
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .align(Alignment.CenterEnd)
                                    .width(80.dp)
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .clickable {
                                        context.performHapticFeedback()
                                        onDeleteDownload(download)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }

                            // Foreground Content
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset { IntOffset(state.requireOffset().roundToInt(), 0) }
                                    .anchoredDraggable(state, Orientation.Horizontal)
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                DownloadItemCard(
                                    download = download,
                                    onPause = { onPauseDownload(download) },
                                    onResume = { onResumeDownload(download) },
                                    onCancel = { onCancelDownload(download) },
                                    onRetry = { onRetryDownload(download) },
                                    onOpen = { openDownloadedFile(context, download) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadItemCard(
    download: DownloadInfo,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val statusIcon = when (download.status) {
        DownloadStatus.DOWNLOADING -> Icons.Rounded.Download
        DownloadStatus.PAUSED -> Icons.Rounded.Pause
        DownloadStatus.COMPLETED -> Icons.Rounded.FolderZip
        DownloadStatus.FAILED -> Icons.Rounded.ErrorOutline
        DownloadStatus.CANCELLED -> Icons.Rounded.Close
        else -> Icons.Rounded.Schedule
    }

    val iconTint = when (download.status) {
        DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primary
        DownloadStatus.PAUSED -> MaterialTheme.colorScheme.secondary
        DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primary
        DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
        DownloadStatus.CANCELLED -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.tertiary
    }

    val isDownloading = download.status == DownloadStatus.DOWNLOADING
    val infiniteTransition = rememberInfiniteTransition(label = "download_animation")
    val arrowOffsetY by infiniteTransition.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrow_offset"
    )
    val iconAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_alpha"
    )

    OtaCard(
        modifier = modifier.fillMaxWidth(),
        onClick = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Bar: Animated Status Icon + Device/Region info + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = iconTint.copy(alpha = 0.12f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier
                                .size(26.dp)
                                .offset(y = if (isDownloading) arrowOffsetY.dp else 0.dp)
                                .alpha(if (isDownloading) iconAlpha else 1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = if (download.deviceName.isNotBlank()) "${download.deviceName} • ${download.regionName}" else "OTA Package",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    DownloadStatusBadge(status = download.status, md5Status = download.md5Status)
                }
            }

            // Middle Row: Filename visible in a single line with ellipsis
            Text(
                text = download.fileName.ifBlank { "OTA Update Package" },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            // Progress Bar & Timing/Stats block
            if (download.status in listOf(DownloadStatus.DOWNLOADING, DownloadStatus.PAUSED, DownloadStatus.QUEUED, DownloadStatus.ADDED)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${FormatUtils.formatSize(download.downloadedBytes)} / ${FormatUtils.formatSize(download.totalBytes)}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${download.progress}%",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (download.status == DownloadStatus.PAUSED) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                        )
                    }

                    LinearProgressIndicator(
                        progress = { if (download.progress > 0) download.progress / 100f else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (download.status == DownloadStatus.PAUSED) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (download.status == DownloadStatus.DOWNLOADING) {
                            Text(
                                text = if (download.speed > 0) FormatUtils.formatDownloadSpeed(download.speed) else "Downloading...",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            val etaStr = FormatUtils.formatEta(download.eta)
                            if (etaStr != "--") {
                                Text(
                                    text = etaStr,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else if (download.status == DownloadStatus.PAUSED) {
                            Text(
                                text = "Paused",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Tap Resume to continue",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Queued for download...",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (download.status == DownloadStatus.FAILED) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Error: ${download.error.name}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Footer actions row with clear labeled buttons and support for retry, cancel, delete, pause/resume
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (download.status) {
                    DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED, DownloadStatus.ADDED -> {
                        OtaOutlinedButton(
                            text = "Pause",
                            icon = Icons.Rounded.Pause,
                            compact = true,
                            onClick = {
                                context.performHapticFeedback()
                                onPause()
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OtaTonalButton(
                            text = "Cancel",
                            icon = Icons.Rounded.Close,
                            compact = true,
                            onClick = {
                                context.performHapticFeedback()
                                onCancel()
                            }
                        )
                    }
                    DownloadStatus.PAUSED -> {
                        OtaPrimaryButton(
                            text = "Resume",
                            icon = Icons.Rounded.PlayArrow,
                            compact = true,
                            onClick = {
                                context.performHapticFeedback()
                                onResume()
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OtaOutlinedButton(
                            text = "Cancel",
                            icon = Icons.Rounded.Close,
                            compact = true,
                            onClick = {
                                context.performHapticFeedback()
                                onCancel()
                            }
                        )
                    }
                    DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                        OtaPrimaryButton(
                            text = "Retry",
                            icon = Icons.Rounded.Refresh,
                            compact = true,
                            onClick = {
                                context.performHapticFeedback()
                                onRetry()
                            }
                        )
                    }
                    DownloadStatus.COMPLETED -> {
                        OtaPrimaryButton(
                            text = "Open ZIP",
                            icon = Icons.Rounded.FolderOpen,
                            compact = true,
                            onClick = {
                                context.performHapticFeedback()
                                onOpen()
                            }
                        )
                    }
                    else -> {
                        OtaOutlinedButton(
                            text = "Retry",
                            icon = Icons.Rounded.Refresh,
                            compact = true,
                            onClick = {
                                context.performHapticFeedback()
                                onRetry()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadStatusBadge(
    status: DownloadStatus,
    md5Status: Md5Status
) {
    val (label, color) = when (status) {
        DownloadStatus.DOWNLOADING -> "Downloading" to MaterialTheme.colorScheme.primary
        DownloadStatus.PAUSED -> "Paused" to MaterialTheme.colorScheme.secondary
        DownloadStatus.QUEUED -> "Queued" to MaterialTheme.colorScheme.tertiary
        DownloadStatus.COMPLETED -> {
            when (md5Status) {
                Md5Status.VERIFIED -> "Verified MD5" to MaterialTheme.colorScheme.primary
                Md5Status.FAILED -> "MD5 Failed" to MaterialTheme.colorScheme.error
                Md5Status.VERIFYING -> "Verifying..." to MaterialTheme.colorScheme.tertiary
                else -> "Completed" to MaterialTheme.colorScheme.primary
            }
        }
        DownloadStatus.FAILED -> "Failed" to MaterialTheme.colorScheme.error
        DownloadStatus.CANCELLED -> "Cancelled" to MaterialTheme.colorScheme.outline
        else -> status.name to MaterialTheme.colorScheme.outline
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun AddDownloadDialog(
    onDismiss: () -> Unit,
    onStartDownload: (String) -> Unit
) {
    var urlInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Add Direct OTA Download", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Paste a direct download link (e.g. from OPPO / OnePlus / Realme server or payload link) to enqueue.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OtaTextField(
                    value = urlInput,
                    onValueChange = {
                        urlInput = it
                        errorMessage = null
                    },
                    label = { Text("OTA URL") },
                    placeholder = { Text("https://...") },
                    isError = errorMessage != null,
                    showPaste = true,
                    showClear = true
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            ApplyDialogBlurEffect()
            OtaPrimaryButton(
                text = "Download",
                onClick = {
                    val trimmed = urlInput.trim()
                    if (trimmed.isEmpty() || !trimmed.startsWith("http")) {
                        errorMessage = "Please enter a valid HTTP/HTTPS URL"
                    } else {
                        onStartDownload(trimmed)
                    }
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun openDownloadedFile(context: Context, downloadInfo: DownloadInfo) {
    val file = File(downloadInfo.file)
    if (!file.exists()) {
        Toast.makeText(context, "File not found on storage", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/zip")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.could_not_open_link), Toast.LENGTH_SHORT).show()
    }
}

@Preview(showBackground = true)
@Composable
fun DownloadsScreenPreview() {
    OtaPulseTheme(themeMode = ThemeMode.MATERIAL_YOU) {
        DownloadsContent(downloads = emptyList())
    }
}
