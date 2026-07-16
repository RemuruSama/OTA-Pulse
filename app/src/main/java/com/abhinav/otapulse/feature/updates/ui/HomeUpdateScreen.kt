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

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhinav.otapulse.R
import com.abhinav.otapulse.core.common.FormatUtils
import com.abhinav.otapulse.core.common.OtaCardData
import com.abhinav.otapulse.core.common.OtaShareHelper
import com.abhinav.otapulse.core.model.OtaUpdate
import com.abhinav.otapulse.core.ui.components.ErrorState
import com.abhinav.otapulse.core.ui.components.LoadingState
import com.abhinav.otapulse.core.ui.components.OtaCard
import com.abhinav.otapulse.core.ui.components.OtaTextField
import com.abhinav.otapulse.core.ui.components.StaggeredItem
import com.abhinav.otapulse.core.ui.components.OtaPrimaryButton
import com.abhinav.otapulse.core.ui.components.OtaTopAppBar
import com.abhinav.otapulse.feature.browser.InAppBrowserActivity
import com.abhinav.otapulse.feature.devicecatalog.ui.OtaDetailsSheet
import com.abhinav.otapulse.feature.otatools.ui.JsonOutputActivity
import androidx.compose.ui.tooling.preview.Preview
import com.abhinav.otapulse.core.ui.theme.OtaPulseTheme
import com.abhinav.otapulse.core.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeUpdateScreen(
    modifier: Modifier = Modifier,
    onNavigateToHistory: () -> Unit = {},
    viewModel: HomeUpdateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    HomeUpdateContent(
        uiState = uiState,
        modifier = modifier,
        onNavigateToHistory = onNavigateToHistory,
        onUpdateModel = viewModel::updateModel,
        onUpdateName = viewModel::updateName,
        onUpdateNvId = viewModel::updateNvId,
        onUpdateVersionLetter = viewModel::updateVersionLetter,
        onUpdateReqMode = viewModel::updateReqMode,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeUpdateContent(
    uiState: HomeUpdateUiState,
    modifier: Modifier = Modifier,
    onNavigateToHistory: () -> Unit = {},
    onUpdateModel: (String) -> Unit = {},
    onUpdateName: (String) -> Unit = {},
    onUpdateNvId: (String) -> Unit = {},
    onUpdateVersionLetter: (String) -> Unit = {},
    onUpdateReqMode: (String) -> Unit = {},
    onCheckForUpdate: () -> Unit = {},
    onSelectOta: (OtaUpdate?) -> Unit = {},
    onClearUserMessage: () -> Unit = {},
    onStartDownload: (OtaUpdate) -> Unit = {},
    onFetchPartitions: (OtaUpdate) -> Unit = {},
    onClearPartitionDialog: () -> Unit = {},
    onExtractPartitions: (String, String, List<String>) -> java.util.UUID = { _, _, _ -> java.util.UUID.randomUUID() },
    onClearStartingExtraction: () -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var showManualFields by remember { mutableStateOf(false) }

    var showSections by remember { mutableStateOf(true) }

    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    if (showSections) {
        LaunchedEffect(uiState.userMessage) {
            uiState.userMessage?.let { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                onClearUserMessage()
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            OtaTopAppBar(
                title = stringResource(R.string.app_name),
                subtitle = stringResource(R.string.app_tagline),
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(
                        onClick = {
                            onNavigateToHistory()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = stringResource(R.string.home_update_history_cd),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StaggeredItem(visible = showSections, index = 0) {
                OtaCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                    0.5f to MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                    1.0f to MaterialTheme.colorScheme.surface,
                                )
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Top Row: Badges
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    ),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = stringResource(R.string.software_update_panel_chip).uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(
                                            horizontal = 10.dp,
                                            vertical = 4.dp
                                        )
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                Color(0xFF4CAF50).copy(alpha = dotAlpha),
                                                CircleShape
                                            ) // Pulsating green dot
                                    )
                                    Text(
                                        text = stringResource(R.string.software_update_panel_version_badge),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Device Info
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = uiState.marketName.ifBlank { stringResource(R.string.unknown) },
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = uiState.deviceName.ifBlank { stringResource(R.string.unknown) },
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.7f
                                        )
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(
                                        alpha = 0.5f
                                    ),
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_device),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                thickness = 1.dp
                            )

                            // Current Version section
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_ota_version),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.software_update_panel_version_label),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                val currentVer = uiState.osVersion.ifBlank {
                                    uiState.displayOtaVersion.ifBlank {
                                        uiState.fallbackOtaVersion.ifBlank { stringResource(R.string.unknown_version) }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    )
                                ) {
                                    Text(
                                        text = currentVer,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 8.dp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Text(
                                text = stringResource(R.string.software_update_panel_meta_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showManualFields) {
                OtaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.software_update_panel_summary),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        OtaTextField(
                        value = uiState.deviceModel,
                        onValueChange = { onUpdateModel(it) },
                        label = { Text(stringResource(R.string.home_model_required_label)) }
                    )

                    OtaTextField(
                        value = uiState.deviceName,
                        onValueChange = { onUpdateName(it) },
                        label = { Text(stringResource(R.string.home_name_label)) }
                    )

                    OtaTextField(
                        value = uiState.nvId,
                        onValueChange = { onUpdateNvId(it) },
                        label = { Text(stringResource(R.string.home_nv_id_label)) }
                    )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DropdownField(
                                label = stringResource(R.string.hint_ver_letter),
                                selectedOption = uiState.versionLetter,
                                options = listOf("A", "C", "F", "H", "J"),
                                onOptionSelected = { onUpdateVersionLetter(it) },
                                modifier = Modifier.weight(1f)
                            )
                            DropdownField(
                                label = "Req Mode",
                                selectedOption = uiState.reqMode,
                                options = listOf("manual", "server_auto", "client_auto", "taste"),
                                onOptionSelected = { onUpdateReqMode(it) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text(
                            text = stringResource(R.string.software_update_panel_footer),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            StaggeredItem(visible = showSections, index = 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showManualFields = !showManualFields }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showManualFields) "Hide Manual Identity Overrides" else "Show Manual Identity Overrides",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = if (showManualFields) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp).size(18.dp)
                    )
                }
            }

            StaggeredItem(visible = showSections, index = 2) {
                OtaPrimaryButton(
                    text = if (uiState.isLoading) "Checking across servers..." else stringResource(R.string.btn_check_for_update),
                    onClick = {
                        focusManager.clearFocus()
                        onCheckForUpdate()
                    },
                    enabled = !uiState.isLoading,
                    icon = if (uiState.isLoading) null else Icons.Rounded.Refresh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    compact = true
                )
            }

            if (uiState.isLoading) {
                LoadingState(message = stringResource(R.string.home_searching_msg))
            }

            uiState.error?.let { err ->
                ErrorState(
                    message = err,
                    onRetry = { onCheckForUpdate() },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val results = uiState.multiResults
            if (!results.isNullOrEmpty()) {
                Text(
                    text = "${stringResource(R.string.update_available_label)} (${results.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                results.forEachIndexed { index, ota ->
                    StaggeredItem(visible = true, index = index) {
                        OtaCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onSelectOta(ota) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SystemUpdateAlt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ota.versionName ?: stringResource(R.string.unknown_version),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${ota.realAndroidVersion?.removePrefix("Android ")?.trim() ?: "Android"} • ${ota.size}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(88.dp))
        }
    }

    uiState.selectedOta?.let { ota ->
        OtaDetailsSheet(
            ota = ota,
            onDismiss = { onSelectOta(null) },
            onDownload = { selected ->
                onStartDownload(selected)
                Toast.makeText(context, context.getString(R.string.home_download_started), Toast.LENGTH_SHORT).show()
                onSelectOta(null)
            },
            onCopyLink = { url ->
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText(context.getString(R.string.home_ota_url_label), url))
                Toast.makeText(context, R.string.toast_link_copied, Toast.LENGTH_SHORT).show()
            },
            onViewChangelog = { url ->
                if (url.isNullOrBlank()) {
                    Toast.makeText(context, context.getString(R.string.home_changelog_unavail), Toast.LENGTH_SHORT).show()
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
                if (selected.rawJson.isNullOrBlank()) {
                    Toast.makeText(context, context.getString(R.string.json_output_unavailable), Toast.LENGTH_SHORT).show()
                } else {
                    context.startActivity(JsonOutputActivity.createIntent(context, selected, "GLO"))
                }
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

@Composable
private fun DropdownField(
    label: String,
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OtaTextField(
            value = selectedOption,
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null
                )
            }
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeUpdateScreenPreview() {
    OtaPulseTheme(themeMode = ThemeMode.MATERIAL_YOU) {
        HomeUpdateContent(uiState = HomeUpdateUiState())
    }
}
