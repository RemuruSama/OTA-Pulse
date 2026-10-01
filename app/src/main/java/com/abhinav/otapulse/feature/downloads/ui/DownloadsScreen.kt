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

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import com.abhinav.otapulse.core.ui.theme.OtaPulseMotion
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
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
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.abhinav.otapulse.core.download.DownloadError
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhinav.otapulse.R
import com.abhinav.otapulse.core.common.FormatUtils
import com.abhinav.otapulse.core.common.HapticType
import com.abhinav.otapulse.core.common.haptic
import com.abhinav.otapulse.core.download.DownloadStatus
import com.abhinav.otapulse.core.model.DownloadInfo
import com.abhinav.otapulse.core.model.Md5Status
import com.abhinav.otapulse.core.ui.components.EmptyState
import com.abhinav.otapulse.core.ui.components.OtaCard
import com.abhinav.otapulse.core.ui.components.OtaOutlinedButton
import com.abhinav.otapulse.core.ui.components.stackItemAppearance
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
    val view = androidx.compose.ui.platform.LocalView.current
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
                title = stringResource(R.string.downloads_screen_title),
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            val navBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            FloatingActionButton(
                onClick = {
                    view.haptic(HapticType.CLICK)
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = navBarsBottom + 88.dp)
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = stringResource(R.string.downloads_add_cd))
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
                    title = stringResource(R.string.downloads_empty_title),
                    message = stringResource(R.string.downloads_empty_msg),
                    actionLabel = stringResource(R.string.downloads_add_direct_link),
                    onAction = {
                        view.haptic(HapticType.CLICK)
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
                    itemsIndexed(
                        items = downloads,
                        key = { _, it -> it.id }
                    ) { index, download ->
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
                                snapAnimationSpec = OtaPulseMotion.StackSwipeSpring,
                                decayAnimationSpec = decaySpec
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max)
                                .stackItemAppearance(index)
                                .animateItem(placementSpec = OtaPulseMotion.StackReorderSpec)
                                .clip(RoundedCornerShape(20.dp))
                        ) {
                            // Delete Button (revealed behind)
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .align(Alignment.CenterEnd)
                                    .width(88.dp)
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .clickable {
                                        view.haptic(HapticType.HEAVY_CLICK)
                                        onDeleteDownload(download)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = stringResource(R.string.action_delete),
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.action_delete),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            // Foreground Content
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset { IntOffset(state.requireOffset().roundToInt(), 0) }
                                    .anchoredDraggable(state, Orientation.Horizontal)
                                    .clip(RoundedCornerShape(20.dp))
                            ) {
                                DownloadItemCard(
                                    download = download,
                                    onPause = { onPauseDownload(download) },
                                    onResume = { onResumeDownload(download) },
                                    onCancel = { onCancelDownload(download) },
                                    onRetry = { onRetryDownload(download) },
                                    onOpen = { openDownloadedFile(context, download) },
                                    onShare = { shareDownloadedFile(context, download) }
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
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current

    val (statusColor, containerTint) = when (download.status) {
        DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        DownloadStatus.PAUSED -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
        DownloadStatus.COMPLETED -> OtaPulseTheme.extendedColors.arbSafe to OtaPulseTheme.extendedColors.arbSafe.copy(alpha = 0.07f)
        DownloadStatus.FAILED -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.error.copy(alpha = 0.07f)
        DownloadStatus.CANCELLED -> MaterialTheme.colorScheme.outline to MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f)
    }

    val statusIcon = when (download.status) {
        DownloadStatus.DOWNLOADING -> Icons.Rounded.Download
        DownloadStatus.PAUSED -> Icons.Rounded.Pause
        DownloadStatus.COMPLETED -> Icons.Rounded.CheckCircle
        DownloadStatus.FAILED -> Icons.Rounded.ErrorOutline
        DownloadStatus.CANCELLED -> Icons.Rounded.Close
        else -> Icons.Rounded.Schedule
    }

    val isDownloading = download.status == DownloadStatus.DOWNLOADING
    val infiniteTransition = rememberInfiniteTransition(label = "download_animation")
    val arrowOffsetY by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrow_offset"
    )
    val iconAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_alpha"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = (download.progress / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "download_progress"
    )

    OtaCard(
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth(),
        onClick = if (download.status == DownloadStatus.COMPLETED) {
            { onOpen() }
        } else null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0.0f to containerTint,
                        0.35f to Color.Transparent
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row: Lead Icon + Device/Target Info + Status Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Lead Squircle Icon
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = statusColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.25f)),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier
                                    .size(24.dp)
                                    .offset(y = if (isDownloading) arrowOffsetY.dp else 0.dp)
                                    .alpha(if (isDownloading) iconAlpha else 1f)
                            )
                        }
                    }

                    // Device / Target info
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (download.deviceName.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Rounded.Smartphone,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = statusColor
                            )
                            Text(
                                text = download.deviceName,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = statusColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = statusColor
                            )
                            Text(
                                text = "OTA Package",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = statusColor
                            )
                        }
                    }

                    // Status Badge
                    DownloadStatusBadge(
                        status = download.status,
                        md5Status = download.md5Status
                    )
                }

                // Full Package Name (untruncated)
                val packageName = when {
                    download.fileName.isNotBlank() && !download.fileName.startsWith("ota_", ignoreCase = true) -> download.fileName
                    !download.otaUpdate?.fileName.isNullOrBlank() -> download.otaUpdate.fileName
                    !download.otaUpdate?.versionName.isNullOrBlank() -> download.otaUpdate.versionName
                    download.fileName.isNotBlank() -> download.fileName
                    else -> "OTA Update Package"
                }
                Text(
                    text = packageName,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.1).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )

                // Middle Section: Status-specific layout
                when (download.status) {
                    DownloadStatus.DOWNLOADING, DownloadStatus.PAUSED, DownloadStatus.QUEUED, DownloadStatus.ADDED -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Telemetry Sizes & Percentage Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = FormatUtils.formatSize(download.downloadedBytes),
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "/",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = FormatUtils.formatSize(download.totalBytes),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = statusColor.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = "${download.progress}%",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = statusColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Dynamic Gradient Progress Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = animatedProgress.coerceAtLeast(0.01f))
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = if (download.status == DownloadStatus.PAUSED) {
                                                    listOf(
                                                        MaterialTheme.colorScheme.secondary,
                                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                                                    )
                                                } else {
                                                    listOf(
                                                        MaterialTheme.colorScheme.primary,
                                                        MaterialTheme.colorScheme.tertiary
                                                    )
                                                }
                                            )
                                        )
                                )
                            }

                            // Speed & Time Remaining Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (download.status == DownloadStatus.DOWNLOADING) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Speed,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (download.speed > 0) FormatUtils.formatDownloadSpeed(download.speed) else "Calculating...",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    val etaStr = FormatUtils.formatEta(download.eta)
                                    if (etaStr != "--") {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Timer,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "$etaStr left",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else if (download.status == DownloadStatus.PAUSED) {
                                    Text(
                                        text = "Download paused",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = "Tap Resume to continue",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Text(
                                        text = "Queued in download manager",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    DownloadStatus.COMPLETED -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = OtaPulseTheme.extendedColors.arbSafe.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, OtaPulseTheme.extendedColors.arbSafe.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FolderZip,
                                    contentDescription = null,
                                    tint = OtaPulseTheme.extendedColors.arbSafe,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Package Size: ${FormatUtils.formatSize(download.totalBytes.takeIf { it > 0 } ?: download.downloadedBytes)}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (download.md5Status == Md5Status.VERIFIED) "MD5 Checksum Verified • Ready to install" else "Saved to Downloads/OTA Pulse",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    DownloadStatus.FAILED -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Download Failed",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = formatErrorMessage(download.error),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }

                    else -> Unit
                }

                // Actions Footer: Share utility on Left (if completed), Primary Actions on Right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (download.status == DownloadStatus.COMPLETED) Arrangement.SpaceBetween else Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (download.status == DownloadStatus.COMPLETED) {
                        IconButton(
                            onClick = {
                                view.haptic(HapticType.CLICK)
                                onShare()
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "Share File",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Right Contextual Action Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (download.status) {
                            DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED, DownloadStatus.ADDED -> {
                                OtaTonalButton(
                                    text = "Pause",
                                    icon = Icons.Rounded.Pause,
                                    compact = true,
                                    onClick = {
                                        view.haptic(HapticType.CLICK)
                                        onPause()
                                    }
                                )
                                OtaOutlinedButton(
                                    text = "Cancel",
                                    icon = Icons.Rounded.Close,
                                    compact = true,
                                    onClick = {
                                        view.haptic(HapticType.CLICK)
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
                                        view.haptic(HapticType.CLICK)
                                        onResume()
                                    }
                                )
                                OtaOutlinedButton(
                                    text = "Cancel",
                                    icon = Icons.Rounded.Close,
                                    compact = true,
                                    onClick = {
                                        view.haptic(HapticType.CLICK)
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
                                        view.haptic(HapticType.CLICK)
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
                                        view.haptic(HapticType.CLICK)
                                        onOpen()
                                    }
                                )
                            }
                            else -> {
                                OtaPrimaryButton(
                                    text = "Retry",
                                    icon = Icons.Rounded.Refresh,
                                    compact = true,
                                    onClick = {
                                        view.haptic(HapticType.CLICK)
                                        onRetry()
                                    }
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
private fun DownloadStatusBadge(
    status: DownloadStatus,
    md5Status: Md5Status
) {
    val (label, icon, color) = when (status) {
        DownloadStatus.DOWNLOADING -> Triple("Downloading", Icons.Rounded.Download, MaterialTheme.colorScheme.primary)
        DownloadStatus.PAUSED -> Triple("Paused", Icons.Rounded.Pause, MaterialTheme.colorScheme.secondary)
        DownloadStatus.QUEUED -> Triple("Queued", Icons.Rounded.Schedule, MaterialTheme.colorScheme.tertiary)
        DownloadStatus.COMPLETED -> {
            when (md5Status) {
                Md5Status.VERIFIED -> Triple("Verified", Icons.Rounded.CheckCircle, OtaPulseTheme.extendedColors.arbSafe)
                Md5Status.FAILED -> Triple("MD5 Error", Icons.Rounded.ErrorOutline, MaterialTheme.colorScheme.error)
                Md5Status.VERIFYING -> Triple("Verifying", Icons.Rounded.Schedule, MaterialTheme.colorScheme.tertiary)
                else -> Triple("Completed", Icons.Rounded.CheckCircle, OtaPulseTheme.extendedColors.arbSafe)
            }
        }
        DownloadStatus.FAILED -> Triple("Failed", Icons.Rounded.ErrorOutline, MaterialTheme.colorScheme.error)
        DownloadStatus.CANCELLED -> Triple("Cancelled", Icons.Rounded.Close, MaterialTheme.colorScheme.outline)
        else -> Triple(status.name, null, MaterialTheme.colorScheme.outline)
    }

    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = color
            )
        }
    }
}

private fun formatErrorMessage(error: DownloadError): String = when (error) {
    DownloadError.NO_NETWORK_CONNECTION -> "No internet connection detected"
    DownloadError.CONNECTION_TIMED_OUT -> "Server connection timed out"
    DownloadError.HTTP_NOT_FOUND -> "OTA package not found on server (404)"
    DownloadError.REQUEST_NOT_SUCCESSFUL -> "Server request failed"
    DownloadError.INSUFFICIENT_STORAGE -> "Insufficient device storage space"
    DownloadError.UNKNOWN_IO_ERROR -> "Storage I/O write error"
    DownloadError.UNKNOWN -> "Unexpected download failure"
    DownloadError.NONE -> "Unknown error"
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
            Text(text = stringResource(R.string.downloads_add_direct_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
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
                    label = { Text(stringResource(R.string.downloads_ota_url_label)) },
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
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

private fun openDownloadedFile(context: Context, downloadInfo: DownloadInfo) {
    val file = File(downloadInfo.file)
    if (!file.exists()) {
        Toast.makeText(context, context.getString(R.string.downloads_file_not_found), Toast.LENGTH_SHORT).show()
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

private fun shareDownloadedFile(context: Context, downloadInfo: DownloadInfo) {
    val file = File(downloadInfo.file)
    if (!file.exists()) {
        Toast.makeText(context, context.getString(R.string.downloads_file_not_found), Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share OTA Package"))
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

