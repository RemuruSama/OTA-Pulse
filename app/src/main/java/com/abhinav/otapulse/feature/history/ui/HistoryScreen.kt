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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import com.abhinav.otapulse.core.ui.ApplyDialogBlurEffect
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhinav.otapulse.core.common.FormatUtils
import com.abhinav.otapulse.core.common.performHapticFeedback
import com.abhinav.otapulse.core.model.OtaHistoryEntry
import com.abhinav.otapulse.core.ui.components.EmptyState
import com.abhinav.otapulse.core.ui.components.FloatingSearchBar
import com.abhinav.otapulse.core.ui.components.OtaCard
import com.abhinav.otapulse.core.ui.components.OtaPrimaryButton
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
    val historyList by historyViewModel.historyFlow.collectAsState()
    val devicesUiState by devicesViewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    var searchQuery by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val json = stream.bufferedReader().use { reader -> reader.readText() }
                    val type = object : TypeToken<List<OtaHistoryEntry>>() {}.type
                    val list: List<OtaHistoryEntry> = Gson().fromJson(json, type) ?: emptyList()
                    if (list.isNotEmpty()) {
                        historyViewModel.importHistory(list)
                        Toast.makeText(context, "Imported ${list.size} history records!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "No records found in file.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to import history JSON.", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, "History exported successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to export history.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear OTA History", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to clear all logged OTA update queries and records? This action cannot be undone.") },
            confirmButton = {
                ApplyDialogBlurEffect()
                OtaPrimaryButton(
                    text = "Clear All",
                    onClick = {
                        historyViewModel.clearHistory(null)
                        showClearDialog = false
                        Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
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
        topBar = {
            OtaTopAppBar(
                title = "Update History",
                scrollBehavior = scrollBehavior,
                actions = {
                    if (historyList.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "${historyList.size} records",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Box {
                        IconButton(onClick = {
                            context.performHapticFeedback()
                            showMenu = true
                        }) {
                            Icon(imageVector = Icons.Rounded.MoreVert, contentDescription = "More Options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Import JSON") },
                                leadingIcon = { Icon(Icons.Rounded.FileUpload, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    importLauncher.launch(arrayOf("application/json"))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export JSON") },
                                leadingIcon = { Icon(Icons.Rounded.FileDownload, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    if (historyList.isEmpty()) {
                                        Toast.makeText(context, "No records to export", Toast.LENGTH_SHORT).show()
                                    } else {
                                        exportLauncher.launch("ota_pulse_history.json")
                                    }
                                }
                            )
                            if (historyList.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Clear All", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        showClearDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (historyList.isNotEmpty()) {
                FloatingSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search by device, region or build version...",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (historyList.isEmpty()) {
                    EmptyState(
                        icon = Icons.Rounded.History,
                        title = "No History Logged",
                        message = "Whenever you query for OTA updates across devices, your results and lookup timestamps will appear here.",
                        actionLabel = "Import JSON History",
                        onAction = {
                            context.performHapticFeedback()
                            importLauncher.launch(arrayOf("application/json"))
                        },
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (filteredList.isEmpty()) {
                    EmptyState(
                        icon = Icons.Rounded.Search,
                        title = "No Matches Found",
                        message = "No history records match '$searchQuery'. Try checking your spelling or search terms.",
                        actionLabel = "Reset Search",
                        onAction = { searchQuery = "" },
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 84.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(
                            items = filteredList,
                            key = { index, entry -> "${entry.timestamp}_${entry.deviceName}_${entry.otaUpdate.versionName}_$index" }
                        ) { _, entry ->
                            HistoryEntryCard(
                                entry = entry,
                                onClick = {
                                    context.performHapticFeedback()
                                    devicesViewModel.showOtaDetailsFromHistory(entry)
                                },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }

    // OTA Details Bottom Sheet from History
    devicesUiState.showOtaDetailsDialog?.let { dialogData ->
        OtaDetailsSheet(
            ota = dialogData.otaUpdate,
            onDismiss = { devicesViewModel.clearOtaDetailsDialog() },
            onDownload = { selected ->
                devicesViewModel.startDownload(selected, dialogData.deviceName, dialogData.regionName)
                Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
                devicesViewModel.clearOtaDetailsDialog()
            },
            onCopyLink = { url ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("OTA URL", url))
                Toast.makeText(context, context.getString(R.string.toast_link_copied), Toast.LENGTH_SHORT).show()
            },
            onViewChangelog = { url ->
                if (url.isNullOrBlank()) {
                    Toast.makeText(context, "Changelog unavailable", Toast.LENGTH_SHORT).show()
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
                if (selected.rawJson.isNullOrBlank()) {
                    Toast.makeText(context, context.getString(R.string.json_output_unavailable), Toast.LENGTH_SHORT).show()
                } else {
                    context.startActivity(JsonOutputActivity.createIntent(context, selected, "GLO"))
                }
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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

            // Bottom Row: Metadata Badges (Region, Android Version/Security Patch, Size)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (entry.region.isNotBlank() && !isHomeUpdateRecord) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                        ) {
                            Text(
                                text = "Region: ${entry.region}",
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
                                text = "Android $androidVer",
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
                                text = "Patch: ${entry.otaUpdate.securityPatch}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                if (!entry.otaUpdate.size.isNullOrBlank() && entry.otaUpdate.size != "0") {
                    Text(
                        text = entry.otaUpdate.size!!,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
