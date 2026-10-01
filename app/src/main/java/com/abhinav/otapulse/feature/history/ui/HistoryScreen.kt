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

package com.abhinav.otapulse.feature.history.ui

import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import com.abhinav.otapulse.core.ui.ApplyDialogBlurEffect
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.launch
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhinav.otapulse.core.common.FormatUtils
import com.abhinav.otapulse.core.common.HapticType
import com.abhinav.otapulse.core.common.haptic
import com.abhinav.otapulse.core.model.OtaHistoryEntry
import com.abhinav.otapulse.core.ui.components.EmptyState
import com.abhinav.otapulse.core.ui.components.FloatingSearchBar
import com.abhinav.otapulse.core.ui.components.OtaCard
import com.abhinav.otapulse.core.ui.components.OtaPrimaryButton
import com.abhinav.otapulse.core.ui.components.stackItemAppearance
import com.abhinav.otapulse.core.ui.theme.OtaPulseMotion
import com.abhinav.otapulse.core.ui.theme.OtaPulseTheme
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.abhinav.otapulse.R
import com.abhinav.otapulse.core.common.OtaCardData
import com.abhinav.otapulse.core.common.OtaShareHelper
import com.abhinav.otapulse.core.ui.components.OtaTopAppBar
import com.abhinav.otapulse.feature.browser.InAppBrowserActivity
import com.abhinav.otapulse.feature.devicecatalog.ui.OtaDetailsSheet
import com.abhinav.otapulse.feature.devices.ui.DevicesViewModel
import com.abhinav.otapulse.core.common.OtaJsonOutputHelper
import com.abhinav.otapulse.feature.otatools.ui.JsonOutputActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    historyViewModel: OtaHistoryViewModel = hiltViewModel(),
    devicesViewModel: DevicesViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current
    val focusManager = LocalFocusManager.current
    val historyList by historyViewModel.historyFlow.collectAsState()
    val checkingArbIds by historyViewModel.checkingArbIds.collectAsState()
    val userMessage by historyViewModel.userMessage.collectAsState()
    val devicesUiState by devicesViewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    var searchQuery by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(userMessage) {
        userMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            historyViewModel.clearUserMessage()
        }
    }

    LaunchedEffect(searchQuery) {
        if (scrollBehavior.state.heightOffset < 0f) {
            scrollBehavior.state.heightOffset = 0f
            scrollBehavior.state.contentOffset = 0f
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val json = stream.bufferedReader().use { reader -> reader.readText() }
                    val type = object : TypeToken<List<OtaHistoryEntry>>() {}.type
                    val list: List<OtaHistoryEntry> = Gson().fromJson(json, type) ?: emptyList()
                    if (list.isNotEmpty()) {
                        historyViewModel.importHistory(list)
                        Toast.makeText(context, context.getString(R.string.history_imported_count, list.size), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, context.getString(R.string.history_no_records_in_file), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, context.getString(R.string.history_import_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        uri?.let {
            try {
                val json = Gson().toJson(historyList)
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(json.toByteArray())
                }
                Toast.makeText(context, context.getString(R.string.history_exported_success), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, context.getString(R.string.history_export_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.history_clear_all), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.history_empty_msg)) },
            confirmButton = {
                ApplyDialogBlurEffect()
                OtaPrimaryButton(
                    text = stringResource(R.string.history_clear_all),
                    onClick = {
                        historyViewModel.clearHistory(null)
                        showClearDialog = false
                        Toast.makeText(context, context.getString(R.string.history_cleared_toast), Toast.LENGTH_SHORT).show()
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    val filteredList = remember(historyList, searchQuery) {
        if (searchQuery.isBlank()) {
            historyList
        } else {
            val q = searchQuery.trim().lowercase()
            historyList.filter {
                it.deviceName.lowercase().contains(q) ||
                it.region.lowercase().contains(q) ||
                it.otaUpdate.versionName?.lowercase()?.contains(q) == true ||
                it.otaUpdate.realOsVersion?.lowercase()?.contains(q) == true
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        },
        topBar = {
            OtaTopAppBar(
                title = stringResource(R.string.history_screen_title),
                scrollBehavior = scrollBehavior,
                actions = {
                    if (historyList.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = stringResource(R.string.history_records_count, historyList.size),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Box {
                        IconButton(onClick = {
                            view.haptic(HapticType.TICK)
                            showMenu = true
                        }) {
                            Icon(imageVector = Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.history_more_options_cd))
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.history_import_json)) },
                                leadingIcon = { Icon(Icons.Rounded.FileUpload, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    importLauncher.launch(arrayOf("application/json"))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.history_export_json)) },
                                leadingIcon = { Icon(Icons.Rounded.FileDownload, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    if (historyList.isEmpty()) {
                                        Toast.makeText(context, context.getString(R.string.history_no_records_export), Toast.LENGTH_SHORT).show()
                                    } else {
                                        exportLauncher.launch("ota_pulse_history.json")
                                    }
                                }
                            )
                            if (historyList.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Check Missing ARB") },
                                    leadingIcon = { Icon(Icons.Rounded.Security, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        historyViewModel.checkAllMissingArb(historyList)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.history_clear_all), color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        showClearDialog = true
                                    }
                                )
                            }
                        }
                    }
                },
                bottomContent = {
                    if (historyList.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 8.dp)
                        ) {
                            FloatingSearchBar(
                                query = searchQuery,
                                onQueryChange = { searchQuery = it },
                                placeholder = stringResource(R.string.history_search_placeholder),
                                onClear = {
                                    view.haptic(HapticType.TICK)
                                    searchQuery = ""
                                    focusManager.clearFocus()
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (historyList.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.History,
                    title = stringResource(R.string.history_empty_title),
                    message = stringResource(R.string.history_empty_msg),
                    actionLabel = stringResource(R.string.history_import_json),
                    onAction = {
                        view.haptic(HapticType.CLICK)
                        importLauncher.launch(arrayOf("application/json"))
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (filteredList.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.Search,
                    title = stringResource(R.string.history_no_matches_title),
                    message = stringResource(R.string.history_no_matches_msg, searchQuery),
                    actionLabel = stringResource(R.string.history_reset_search),
                    onAction = { searchQuery = "" },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(
                        items = filteredList,
                        key = { _, entry -> "${entry.id}_${entry.timestamp}_${entry.deviceName}_${entry.otaUpdate.versionName}" }
                    ) { index, entry ->
                        val currentEntry by rememberUpdatedState(entry)
                        val density = LocalDensity.current
                        val dismissState = remember(entry.id, entry.timestamp) {
                            SwipeToDismissBoxState(
                                initialValue = SwipeToDismissBoxValue.Settled,
                                density = density,
                                confirmValueChange = { dismissValue ->
                                    if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                        view.haptic(HapticType.HEAVY_CLICK)
                                        historyViewModel.deleteHistoryEntry(currentEntry)
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = context.getString(R.string.history_entry_deleted),
                                                actionLabel = context.getString(R.string.action_undo),
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                historyViewModel.restoreHistoryEntry(currentEntry)
                                            }
                                        }
                                        true
                                    } else {
                                        false
                                    }
                                },
                                positionalThreshold = { it * 0.4f }
                            )
                        }

                        LaunchedEffect(entry.id, entry.timestamp) {
                            if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                            }
                        }

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = true,
                            backgroundContent = {
                                val isDismissing = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
                                val backgroundColor by animateColorAsState(
                                    targetValue = if (isDismissing) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                    label = "swipe_delete_bg"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(backgroundColor)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.action_delete),
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Icon(
                                            imageVector = Icons.Rounded.Delete,
                                            contentDescription = stringResource(R.string.action_delete),
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .stackItemAppearance(index, searchQuery)
                                .animateItem(placementSpec = OtaPulseMotion.StackReorderSpec)
                        ) {
                            HistoryEntryCard(
                                entry = entry,
                                isCheckingArb = checkingArbIds.contains(entry.id),
                                onCheckArb = {
                                    view.haptic(HapticType.CLICK)
                                    historyViewModel.checkAndSaveArbStatus(entry)
                                },
                                onClick = {
                                    view.haptic(HapticType.CLICK)
                                    devicesViewModel.showOtaDetailsFromHistory(entry)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // OTA Details Bottom Sheet from History
    devicesUiState.showOtaDetailsDialog?.let { dialogData ->
        val currentOta = historyList.find {
            it.otaUpdate.url == dialogData.otaUpdate.url || it.otaUpdate.downloadUrl == dialogData.otaUpdate.downloadUrl
        }?.otaUpdate ?: dialogData.otaUpdate

        OtaDetailsSheet(
            ota = currentOta,
            onDismiss = { devicesViewModel.clearOtaDetailsDialog() },
            onCheckArb = { ota ->
                historyList.find { it.otaUpdate.url == ota.url || it.otaUpdate.downloadUrl == ota.downloadUrl }?.let {
                    historyViewModel.checkAndSaveArbStatus(it)
                }
            },
            onDownload = { selected ->
                devicesViewModel.startDownload(selected, dialogData.deviceName, dialogData.regionName)
                Toast.makeText(context, context.getString(R.string.history_download_started), Toast.LENGTH_SHORT).show()
                devicesViewModel.clearOtaDetailsDialog()
            },
            onCopyLink = { url ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.history_ota_url_label), url))
                Toast.makeText(context, context.getString(R.string.toast_link_copied), Toast.LENGTH_SHORT).show()
            },
            onViewChangelog = { url ->
                if (url.isNullOrBlank()) {
                    Toast.makeText(context, context.getString(R.string.history_changelog_unavail), Toast.LENGTH_SHORT).show()
                } else {
                    context.startActivity(InAppBrowserActivity.createIntent(context, url, "Changelog"))
                }
            },
            onShare = { selected ->
                OtaShareHelper.shareOtaCard(
                    context,
                    OtaCardData(
                        deviceName = dialogData.deviceName,
                        versionName = selected.versionName,
                        regionName = dialogData.regionName,
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
                context.startActivity(JsonOutputActivity.createIntent(context, otaWithJson, dialogData.regionName))
            },
            isFetchingPartitions = devicesUiState.isFetchingPartitions,
            onFetchPartitions = { selected -> devicesViewModel.fetchExtractablePartitions(selected) },
            partitionDialogData = devicesUiState.showPartitionSelectDialog,
            onDismissPartitionDialog = { devicesViewModel.clearPartitionSelectDialog() },
            onExtractPartitions = { url, ver, parts ->
                devicesViewModel.extractPartitions(url, ver, parts, dialogData.regionName)
            },
            isStartingExtraction = devicesUiState.isStartingExtraction,
            onClearStartingExtraction = { devicesViewModel.clearStartingExtraction() }
        )
    }
}

@Composable
private fun HistoryEntryCard(
    entry: OtaHistoryEntry,
    isCheckingArb: Boolean,
    onCheckArb: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = androidx.compose.ui.platform.LocalView.current

    OtaCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val isHomeUpdateRecord = entry.deviceName == "Custom Device" || entry.deviceName.startsWith("Custom|") || entry.deviceName.equals("This Device", ignoreCase = true)
            val resolvedDeviceName = if (entry.deviceName.startsWith("Custom|")) {
                entry.deviceName.removePrefix("Custom|").ifBlank {
                    (entry.otaUpdate.versionName ?: entry.otaUpdate.componentVersion).substringBefore("_")
                }
            } else if (entry.deviceName == "Custom Device") {
                (entry.otaUpdate.versionName ?: entry.otaUpdate.componentVersion).substringBefore("_").ifBlank { "Custom Device" }
            } else {
                entry.deviceName.ifBlank { "Unknown Device" }
            }

            // Top Row: Device Name & Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = resolvedDeviceName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = FormatUtils.formatTimestamp(entry.timestamp),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Middle Row: Version Name (Prominent & Multi-line capable without squishing)
            Text(
                text = entry.otaUpdate.versionName ?: entry.otaUpdate.realOsVersion ?: "Update available",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            // Bottom Row: Metadata Badges (Region, Android Version/Security Patch, ARB Status, Size)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (entry.region.isNotBlank() && !isHomeUpdateRecord) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                        ) {
                            Text(
                                text = stringResource(R.string.history_region_label, entry.region),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    val androidVer = entry.otaUpdate.realAndroidVersion?.removePrefix("Android ")?.trim()
                    if (!androidVer.isNullOrBlank() && androidVer != "null") {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Text(
                                text = stringResource(R.string.history_android_ver_label, androidVer),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    } else if (!entry.otaUpdate.securityPatch.isNullOrBlank() && entry.otaUpdate.securityPatch != "null") {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Text(
                                text = stringResource(R.string.history_patch_label, entry.otaUpdate.securityPatch),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // ARB Status Badge / Interactive Check Action
                    val arbStatus = entry.otaUpdate.arbStatus
                    if (isCheckingArb) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(11.dp),
                                    strokeWidth = 1.5.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Checking...",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else if (!arbStatus.isNullOrBlank() && arbStatus != "N/A") {
                        val isSafe = arbStatus.equals("Safe", ignoreCase = true) || arbStatus.contains("Safe", ignoreCase = true)
                        val isProtected = arbStatus.contains("Protected", ignoreCase = true)
                        val badgeColor = when {
                            isSafe -> OtaPulseTheme.extendedColors.arbSafe
                            isProtected -> OtaPulseTheme.extendedColors.arbWarning
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val badgeIcon = when {
                            isSafe -> Icons.Rounded.VerifiedUser
                            isProtected -> Icons.Rounded.Warning
                            else -> Icons.Rounded.Security
                        }
                        val displayText = when {
                            isSafe -> "ARB 0 (Safe)"
                            isProtected -> arbStatus
                            else -> "ARB: $arbStatus"
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = badgeColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f)),
                            modifier = Modifier.clickable {
                                view.haptic(HapticType.CLICK)
                                onCheckArb()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = badgeIcon,
                                    contentDescription = null,
                                    tint = badgeColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = displayText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = badgeColor
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                            modifier = Modifier.clickable {
                                view.haptic(HapticType.CLICK)
                                onCheckArb()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Check ARB",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                val size = entry.otaUpdate.size
                if (!size.isNullOrBlank() && size != "0") {
                    Text(
                        text = size,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}


