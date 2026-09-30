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

package com.abhinav.otapulse.feature.updates.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhinav.otapulse.R
import com.abhinav.otapulse.core.common.FormatUtils
import com.abhinav.otapulse.core.common.HapticType
import com.abhinav.otapulse.core.common.OtaCardData
import com.abhinav.otapulse.core.common.OtaShareHelper
import com.abhinav.otapulse.core.common.rememberHaptic
import com.abhinav.otapulse.core.common.toFullRegionName
import com.abhinav.otapulse.core.model.OtaUpdate
import com.abhinav.otapulse.core.ui.components.ErrorState
import com.abhinav.otapulse.core.ui.components.LoadingState
import com.abhinav.otapulse.core.ui.components.OtaCard
import com.abhinav.otapulse.core.ui.components.OtaPrimaryButton
import com.abhinav.otapulse.core.ui.components.OtaTextField
import com.abhinav.otapulse.core.ui.components.OtaTonalButton
import com.abhinav.otapulse.core.ui.components.StaggeredItem
import com.abhinav.otapulse.core.ui.theme.OtaPulseTheme
import com.abhinav.otapulse.core.ui.theme.ThemeMode
import com.abhinav.otapulse.feature.browser.InAppBrowserActivity
import com.abhinav.otapulse.core.common.OtaJsonOutputHelper
import com.abhinav.otapulse.feature.devicecatalog.ui.OtaDetailsSheet
import com.abhinav.otapulse.feature.otatools.ui.JsonOutputActivity
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeUpdateScreen(
    modifier: Modifier = Modifier,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToDeviceCatalog: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: HomeUpdateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    HomeUpdateContent(
        uiState = uiState,
        modifier = modifier,
        onNavigateToHistory = onNavigateToHistory,
        onNavigateToDownloads = onNavigateToDownloads,
        onNavigateToDeviceCatalog = onNavigateToDeviceCatalog,
        onNavigateToAbout = onNavigateToAbout,
        onNavigateToSettings = onNavigateToSettings,
        onUpdateModel = viewModel::updateModel,
        onUpdateName = viewModel::updateName,
        onUpdateNvId = viewModel::updateNvId,
        onUpdateDeviceRegion = viewModel::updateDeviceRegion,
        onUpdateVersionLetter = viewModel::updateVersionLetter,
        onUpdateReqMode = viewModel::updateReqMode,
        onResetDefaults = viewModel::resetToSystemDefaults,
        onCheckForUpdate = viewModel::checkForUpdate,
        onSelectOta = viewModel::selectOta,
        onClearUserMessage = viewModel::clearUserMessage,
        onStartDownload = viewModel::startDownload,
        onFetchPartitions = viewModel::fetchExtractablePartitions,
        onClearPartitionDialog = viewModel::clearPartitionSelectDialog,
        onExtractPartitions = viewModel::extractPartitions,
        onClearStartingExtraction = viewModel::clearStartingExtraction
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeUpdateContent(
    uiState: HomeUpdateUiState,
    modifier: Modifier = Modifier,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToDeviceCatalog: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onUpdateModel: (String) -> Unit = {},
    onUpdateName: (String) -> Unit = {},
    onUpdateNvId: (String) -> Unit = {},
    onUpdateDeviceRegion: (String) -> Unit = {},
    onUpdateVersionLetter: (String) -> Unit = {},
    onUpdateReqMode: (String) -> Unit = {},
    onResetDefaults: () -> Unit = {},
    onCheckForUpdate: () -> Unit = {},
    onSelectOta: (OtaUpdate?) -> Unit = {},
    onClearUserMessage: () -> Unit = {},
    onStartDownload: (OtaUpdate) -> Unit = {},
    onFetchPartitions: (OtaUpdate) -> Unit = {},
    onClearPartitionDialog: () -> Unit = {},
    onExtractPartitions: (String, String, List<String>) -> UUID = { _, _, _ -> UUID.randomUUID() },
    onClearStartingExtraction: () -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val haptic = rememberHaptic()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var showSections by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showSections = true }

    if (showSections) {
        LaunchedEffect(uiState.userMessage) {
            uiState.userMessage?.let { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                onClearUserMessage()
            }
        }
    }

    val copyToClipboard = { label: String, text: String ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        haptic(HapticType.CLICK)
        Toast.makeText(context, R.string.toast_link_copied, Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 28.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.app_tagline),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            haptic(HapticType.CLICK)
                            onNavigateToHistory()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = stringResource(R.string.home_update_history_cd),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // HERO DEVICE CARD
                StaggeredItem(visible = showSections, index = 0) {
                    OtaCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        0.0f to MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                        0.5f to MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                        1.0f to MaterialTheme.colorScheme.surfaceContainerLow
                                    )
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Top Badges Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                        border = BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        ),
                                        shape = CircleShape
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Settings,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = stringResource(R.string.software_update_panel_chip),
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    Surface(
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                        border = BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        ),
                                        shape = CircleShape,
                                        modifier = Modifier.clickable {
                                            haptic(HapticType.CLICK)
                                            onNavigateToDeviceCatalog()
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .background(
                                                        Color(0xFF4CAF50),
                                                        CircleShape
                                                    )
                                            )
                                            Text(
                                                text = "Live profile",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Icon(
                                                imageVector = Icons.Rounded.ChevronRight,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                // Device Info Row
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = uiState.marketName.ifBlank { "OnePlus 12" },
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Text(
                                            text = uiState.deviceName.ifBlank { uiState.deviceModel },
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Official device profile loaded from live data",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }

                                // CURRENT VERSION INNER CARD
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Download,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = stringResource(R.string.software_update_panel_version_label),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            if (uiState.nvId.isNotBlank()) {
                                                Text(
                                                    text = "NV ID: ${uiState.nvId}",
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                                )
                                            }
                                        }

                                        val currentVer = uiState.osVersion.ifBlank {
                                            uiState.displayOtaVersion.ifBlank {
                                                uiState.fallbackOtaVersion.ifBlank { stringResource(R.string.unknown_version) }
                                            }
                                        }

                                        // Large Version Box
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { copyToClipboard("Version", currentVer) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = currentVer,
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Black
                                                    ),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Icon(
                                                    imageVector = Icons.Rounded.ContentCopy,
                                                    contentDescription = "Copy Version",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        // Side-by-Side Spec Cards
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            // Build Type
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    Text(
                                                        text = "Build Type",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = uiState.buildType.ifBlank { "Official Stable" },
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }

                                            // Region
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    Text(
                                                        text = "Region",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = "${uiState.deviceRegion} (${uiState.deviceRegion.toFullRegionName()})",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = "Built from the live device profile and ready for a one-tap check.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // PRIMARY FULL-WIDTH CTA BUTTON
                StaggeredItem(visible = showSections, index = 1) {
                    OtaPrimaryButton(
                        text = if (uiState.isLoading) "Checking across update servers..." else stringResource(R.string.btn_check_for_update),
                        onClick = {
                            focusManager.clearFocus()
                            haptic(HapticType.CLICK)
                            onCheckForUpdate()
                        },
                        enabled = !uiState.isLoading,
                        isLoading = false,
                        icon = Icons.Rounded.Refresh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(CircleShape),
                        compact = false
                    )
                }

                // LOADING & ERROR STATES
                if (uiState.isLoading) {
                    LoadingState(message = stringResource(R.string.home_searching_msg))
                }

                uiState.error?.let { err ->
                    ErrorState(
                        message = err,
                        onRetry = {
                            haptic(HapticType.CLICK)
                            onCheckForUpdate()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // RESULTS SECTION
                val results = uiState.multiResults
                if (!results.isNullOrEmpty()) {
                    var showResults by remember(results) { mutableStateOf(false) }
                    LaunchedEffect(results) { showResults = true }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SystemUpdateAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Updates Found",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "${results.size} Available",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    results.forEachIndexed { index, ota ->
                        StaggeredItem(visible = showResults, index = index) {
                            OtaCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                onClick = {
                                    haptic(HapticType.CLICK)
                                    onSelectOta(ota)
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Brush.verticalGradient(
                                                0.0f to MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                                                1.0f to MaterialTheme.colorScheme.surfaceContainerLow
                                            )
                                        )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        // Top Badges Bar
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                                shape = CircleShape
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.SystemUpdateAlt,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(
                                                        text = "OFFICIAL UPDATE",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Black,
                                                            letterSpacing = 0.5.sp
                                                        ),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }

                                            if (!ota.securityPatch.isNullOrBlank()) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                                    shape = CircleShape
                                                ) {
                                                    Text(
                                                        text = "Patch: ${ota.securityPatch}",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Version Name Display Box
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = "VERSION PACKAGE",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 0.5.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            val verName = ota.versionName ?: stringResource(R.string.unknown_version)
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { copyToClipboard("Version Package", verName) }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = verName,
                                                        style = MaterialTheme.typography.titleSmall.copy(
                                                            fontFamily = FontFamily.Monospace,
                                                            fontWeight = FontWeight.Black
                                                        ),
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Icon(
                                                        imageVector = Icons.Rounded.ContentCopy,
                                                        contentDescription = "Copy Version Package",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Metadata Spec Badges Row
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {

                                            // Android Version
                                            val androidVer = ota.realAndroidVersion?.removePrefix("Android ")?.trim() ?: "Android"
                                            Surface(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "Android $androidVer",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }

                                            // API Level (e.g. API 37)
                                            if (!ota.androidApiLevel.isNullOrBlank()) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        text = "API ${ota.androidApiLevel}",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }

                                            // Size Badge
                                            Surface(
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = ota.size,
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }

                                            // ARB Status Badge
                                            if (!ota.arbStatus.isNullOrBlank() && ota.arbStatus != "N/A") {
                                                val isProtected = ota.arbStatus.contains("Protected", ignoreCase = true) ||
                                                        ota.arbStatus.contains("Warning", ignoreCase = true) ||
                                                        ota.arbStatus.contains("Risk", ignoreCase = true) ||
                                                        (!ota.arbStatus.contains("Safe", ignoreCase = true) && !ota.arbStatus.endsWith("0"))

                                                val arbBgColor = if (isProtected) {
                                                    MaterialTheme.colorScheme.errorContainer
                                                } else {
                                                    MaterialTheme.colorScheme.tertiaryContainer
                                                }

                                                val arbTextColor = if (isProtected) {
                                                    MaterialTheme.colorScheme.onErrorContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onTertiaryContainer
                                                }

                                                Surface(
                                                    color = arbBgColor,
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        text = "ARB: ${ota.arbStatus}",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = arbTextColor,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Upgrade Tips / Summary Banner
                                        if (!ota.upgradeTips.isNullOrBlank() || !ota.firstTitle.isNullOrBlank()) {
                                            val tipText = ota.upgradeTips?.replace("%s", "")?.trim()?.takeIf { it.isNotEmpty() }
                                                ?: ota.firstTitle?.lines()?.firstOrNull()?.trim() ?: ""
                                            if (tipText.isNotBlank()) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = tipText,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                    )
                                                }
                                            }
                                        }

                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                            thickness = 1.dp
                                        )

                                        // Inline Quick Actions
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OtaTonalButton(
                                                text = "View Details",
                                                onClick = {
                                                    haptic(HapticType.CLICK)
                                                    onSelectOta(ota)
                                                },
                                                icon = Icons.Rounded.Info,
                                                compact = true,
                                                modifier = Modifier.weight(1f)
                                            )

                                            OtaPrimaryButton(
                                                text = "Download",
                                                onClick = {
                                                    haptic(HapticType.CLICK)
                                                    onStartDownload(ota)
                                                    Toast.makeText(context, R.string.home_download_started, Toast.LENGTH_SHORT).show()
                                                },
                                                icon = Icons.Rounded.Download,
                                                compact = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(160.dp))
            }
        }
    }

    uiState.selectedOta?.let { ota ->
        OtaDetailsSheet(
            ota = ota,
            onDismiss = { onSelectOta(null) },
            onDownload = { selected ->
                onStartDownload(selected)
                Toast.makeText(context, R.string.home_download_started, Toast.LENGTH_SHORT).show()
                onSelectOta(null)
            },
            onCopyLink = { url ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("OTA Link", url))
                Toast.makeText(context, R.string.toast_link_copied, Toast.LENGTH_SHORT).show()
            },
            onViewChangelog = { url ->
                if (url.isNullOrBlank()) {
                    Toast.makeText(context, R.string.home_changelog_unavail, Toast.LENGTH_SHORT).show()
                } else {
                    context.startActivity(InAppBrowserActivity.createIntent(context, url, "Changelog"))
                }
            },
            onShare = { selected ->
                val deviceLabel = uiState.deviceName.ifBlank { uiState.deviceModel }.ifBlank { "Unknown" }
                OtaShareHelper.shareOtaCard(
                    context,
                    OtaCardData(
                        deviceName = deviceLabel,
                        versionName = selected.versionName,
                        regionName = null,
                        androidVersion = selected.realAndroidVersion?.removePrefix("Android ")?.trim(),
                        securityPatch = selected.securityPatch,
                        size = selected.size,
                        arbStatus = selected.arbStatus,
                        md5 = selected.md5,
                        downloadUrl = selected.url,
                        changelogUrl = selected.panelUrl,
                        nvId = selected.nvId16,
                        projectId = selected.oplusSeparateSoft,
                        buildDate = FormatUtils.formatBuildDate(selected),
                        targetVersion = selected.otaTargetVersion?.ifBlank { null } ?: selected.realOtaVersion?.ifBlank { null }
                    )
                )
            },
            onViewJson = { selected ->
                val otaWithJson = if (selected.rawJson.isNullOrBlank()) {
                    selected.copy(rawJson = OtaJsonOutputHelper.getJsonOutput(selected))
                } else {
                    selected
                }
                context.startActivity(JsonOutputActivity.createIntent(context, otaWithJson, "GLO"))
            },
            isFetchingPartitions = uiState.isFetchingPartitions,
            onFetchPartitions = { selected -> onFetchPartitions(selected) },
            partitionDialogData = uiState.partitionSelectDialog,
            onDismissPartitionDialog = { onClearPartitionDialog() },
            onExtractPartitions = { url, versionName, partitionNames ->
                val id = onExtractPartitions(url, versionName, partitionNames)
                onClearPartitionDialog()
                id
            },
            isStartingExtraction = uiState.isStartingExtraction,
            onClearStartingExtraction = { onClearStartingExtraction() }
        )

        Spacer(modifier = Modifier.height(84.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun HomeUpdateScreenPreview() {
    OtaPulseTheme(themeMode = ThemeMode.MATERIAL_YOU) {
        HomeUpdateContent(uiState = HomeUpdateUiState())
    }
}

