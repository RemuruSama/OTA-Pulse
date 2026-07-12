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

package com.abhinav.otapulse.feature.devicecatalog.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.abhinav.otapulse.arb.worker.PartitionExtractorWorker
import java.util.UUID
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhinav.otapulse.R
import android.widget.Toast
import com.abhinav.otapulse.core.common.performHapticFeedback
import com.abhinav.otapulse.core.common.FormatUtils
import com.abhinav.otapulse.core.model.OtaUpdate
import com.abhinav.otapulse.core.ui.components.OtaCard
import com.abhinav.otapulse.core.ui.components.OtaOutlinedButton
import com.abhinav.otapulse.core.ui.components.OtaPrimaryButton
import com.abhinav.otapulse.core.ui.components.OtaTextField
import com.abhinav.otapulse.core.ui.components.OtaTonalButton
import com.abhinav.otapulse.core.ui.components.ProgressSheet
import com.abhinav.otapulse.core.ui.components.WavyProgressIndicator
import com.abhinav.otapulse.core.ui.theme.OtaPulseTheme
import com.abhinav.otapulse.ota.payload.PartitionInfo

data class PartitionSelectDialogData(
    val url: String,
    val versionName: String,
    val partitions: List<PartitionInfo>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtaDetailsSheet(
    ota: OtaUpdate,
    onDismiss: () -> Unit,
    onDownload: (OtaUpdate) -> Unit,
    onCopyLink: (String) -> Unit,
    onViewChangelog: (String?) -> Unit,
    onShare: (OtaUpdate) -> Unit,
    onViewJson: (OtaUpdate) -> Unit,
    isFetchingPartitions: Boolean = false,
    onFetchPartitions: (OtaUpdate) -> Unit = {},
    partitionDialogData: PartitionSelectDialogData? = null,
    onDismissPartitionDialog: () -> Unit = {},
    onExtractPartitions: (url: String, versionName: String, partitionNames: List<String>) -> UUID? = { _, _, _ -> null },
    isStartingExtraction: Boolean = false,
    onClearStartingExtraction: () -> Unit = {}
) {
    var selectedPartitions by remember { mutableStateOf(setOf<PartitionInfo>()) }
    var cachedPartitionDialogData by remember { mutableStateOf<PartitionSelectDialogData?>(null) }
    var showPartitionDialog by remember { mutableStateOf(false) }
    var activeWorkId by remember { mutableStateOf<UUID?>(null) }

    LaunchedEffect(partitionDialogData) {
        if (partitionDialogData != null) {
            cachedPartitionDialogData = partitionDialogData
            showPartitionDialog = true
        }
    }

    val context = LocalContext.current
    val workInfoFromId by produceState<WorkInfo?>(initialValue = null, activeWorkId) {
        val id = activeWorkId
        if (id != null) {
            WorkManager.getInstance(context)
                .getWorkInfoByIdFlow(id)
                .collect { value = it }
        } else {
            value = null
        }
    }

    val tag = remember(selectedPartitions) {
        if (selectedPartitions.isNotEmpty()) "extraction_${selectedPartitions.map { it.name }.joinToString("_")}" else ""
    }
    val workInfoFromTag by produceState<WorkInfo?>(initialValue = null, tag) {
        if (tag.isNotEmpty()) {
            WorkManager.getInstance(context)
                .getWorkInfosByTagFlow(tag)
                .collect { list ->
                    value = list.firstOrNull { !it.state.isFinished }
                }
        } else {
            value = null
        }
    }

    val workInfo = workInfoFromId ?: workInfoFromTag

    LaunchedEffect(workInfo?.state, activeWorkId) {
        if (activeWorkId != null || workInfo != null) {
            if (isStartingExtraction) {
                onClearStartingExtraction()
            }
        }
        if (workInfo?.state?.isFinished == true) {
            onClearStartingExtraction()
            kotlinx.coroutines.delay(1500)
            if (activeWorkId == workInfo?.id) {
                activeWorkId = null
            }
        }
    }

    val isExtracting = (isStartingExtraction && activeWorkId == null && workInfo == null) ||
            (activeWorkId != null && workInfo?.state != WorkInfo.State.CANCELLED && workInfo?.state != WorkInfo.State.FAILED) ||
            (workInfoFromTag != null && !workInfoFromTag!!.state.isFinished)

    val extractionProgress: Int? = remember(isExtracting, activeWorkId, workInfo) {
        when {
            !isExtracting -> null
            workInfo?.state == WorkInfo.State.SUCCEEDED -> 100
            workInfo != null && !workInfo?.state!!.isFinished -> {
                val p = workInfo?.progress?.getInt(PartitionExtractorWorker.PROGRESS_KEY, -1) ?: -1
                if (p != -1) p else null
            }
            else -> null
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isHolo = OtaPulseTheme.holographicConfig.isEnabled
    val containerColor = if (isHolo) {
        OtaPulseTheme.extendedColors.glassPanel
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = containerColor,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Title & View JSON Top Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = ota.componentName ?: stringResource(R.string.ota_details_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = ota.versionName ?: stringResource(R.string.unknown_version),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    onClick = { onViewJson(ota) },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Code,
                            contentDescription = stringResource(R.string.button_view_json),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 12.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Details Grid Card
            OtaCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Row 1: Android Version | Security Patch
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = stringResource(R.string.label_android),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = ota.realAndroidVersion?.removePrefix("Android ")?.trim() ?: stringResource(R.string.unknown),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.label_security_patch),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = ota.securityPatch ?: stringResource(R.string.unknown),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Row 2: ARB Status | Update Size
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = stringResource(R.string.arb_status_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val arbStatusText = ota.arbStatus ?: "N/A"
                            val arbColor = when {
                                arbStatusText.equals("Safe", ignoreCase = true) -> OtaPulseTheme.extendedColors.arbSafe
                                arbStatusText.contains("Protected", ignoreCase = true) -> OtaPulseTheme.extendedColors.arbWarning
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                            Text(
                                text = arbStatusText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = arbColor
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.label_size),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = ota.size,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Row 3: NV Identifier | Project ID
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = stringResource(R.string.label_nv_id),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = ota.nvId16?.ifBlank { "N/A" } ?: "N/A",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.label_separate_soft),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = ota.oplusSeparateSoft?.ifBlank { "N/A" } ?: "N/A",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Row 4: Build Date
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.label_published_time),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = FormatUtils.formatBuildDate(ota),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Row 5: MD5 Checksum
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.label_md5),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = ota.md5.ifBlank { "N/A" },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Row 6: Target Version (Bottom single line)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.label_target_version),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = ota.otaTargetVersion?.ifBlank { null } ?: ota.realOtaVersion?.ifBlank { null } ?: "N/A",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Compact Actions Row: Changelog | Copy | Share
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OtaTonalButton(
                    text = stringResource(R.string.btn_changelog),
                    onClick = { onViewChangelog(ota.panelUrl) },
                    icon = Icons.Rounded.OpenInNew,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                ActionIconButton(
                    icon = Icons.Rounded.ContentCopy,
                    contentDescription = "Copy Link",
                    onClick = { onCopyLink(ota.url) }
                )
                ActionIconButton(
                    icon = Icons.Rounded.Share,
                    contentDescription = "Share",
                    onClick = { onShare(ota) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Single-line Expandable Partition Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = {
                        if (!isFetchingPartitions && !isExtracting) {
                            if (cachedPartitionDialogData != null) {
                                showPartitionDialog = true
                            } else {
                                onFetchPartitions(ota)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val pillText = when {
                                isFetchingPartitions -> stringResource(R.string.partition_loading_msg)
                                isExtracting -> {
                                    if (extractionProgress != null && extractionProgress in 0..100) {
                                        if (selectedPartitions.size == 1) {
                                            "Extracting ${selectedPartitions.first().name} ($extractionProgress%)..."
                                        } else {
                                            "Extracting ${selectedPartitions.size} partitions ($extractionProgress%)..."
                                        }
                                    } else {
                                        if (selectedPartitions.size == 1) {
                                            "Extracting ${selectedPartitions.first().name}..."
                                        } else {
                                            "Extracting ${selectedPartitions.size} partitions..."
                                        }
                                    }
                                }
                                selectedPartitions.isNotEmpty() -> {
                                    if (selectedPartitions.size == 1) {
                                        val p = selectedPartitions.first()
                                        "${p.name} (${p.formattedSize})"
                                    } else {
                                        val totalBytes = selectedPartitions.sumOf { it.sizeBytes }
                                        "${selectedPartitions.size} partitions (${FormatUtils.formatSize(totalBytes)})"
                                    }
                                }
                                else -> stringResource(R.string.partition_extraction_select_partition)
                            }
                            Text(
                                text = pillText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedPartitions.isNotEmpty() || isExtracting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (isFetchingPartitions) {
                                WavyProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    size = 20.dp
                                )
                            } else if (!isExtracting) {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = "Select Partition",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (isExtracting) {
                            if (extractionProgress != null && extractionProgress >= 0) {
                                LinearProgressIndicator(
                                    progress = { extractionProgress / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .align(Alignment.BottomCenter),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = Color.Transparent
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .align(Alignment.BottomCenter),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = Color.Transparent
                                )
                            }
                        }
                    }
                }

                androidx.compose.material3.Button(
                    onClick = {
                        if (isExtracting && extractionProgress != 100) {
                            context.performHapticFeedback()
                            val idToCancel = activeWorkId ?: workInfo?.id
                            if (idToCancel != null) {
                                WorkManager.getInstance(context).cancelWorkById(idToCancel)
                            }
                            if (tag.isNotEmpty()) {
                                WorkManager.getInstance(context).cancelAllWorkByTag(tag)
                            }
                            activeWorkId = null
                            onClearStartingExtraction()
                            Toast.makeText(context, context.getString(R.string.cancel) + " - Extraction Stopped", Toast.LENGTH_SHORT).show()
                        } else {
                            val dialogData = cachedPartitionDialogData ?: partitionDialogData
                            if (dialogData != null && selectedPartitions.isNotEmpty() && !isExtracting) {
                                val workId = onExtractPartitions(
                                    dialogData.url,
                                    dialogData.versionName,
                                    selectedPartitions.map { it.name }
                                )
                                if (workId != null) {
                                    activeWorkId = workId
                                }
                            }
                        }
                    },
                    enabled = (isExtracting && extractionProgress != 100) || ((cachedPartitionDialogData != null || partitionDialogData != null) && selectedPartitions.isNotEmpty() && !isExtracting && !isFetchingPartitions),
                    shape = RoundedCornerShape(8.dp),
                    colors = if (isExtracting && extractionProgress != 100) {
                        androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    } else {
                        androidx.compose.material3.ButtonDefaults.buttonColors()
                    },
                    modifier = Modifier
                        .width(88.dp)
                        .fillMaxHeight(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                ) {
                    Text(
                        text = if (isExtracting) {
                            if (extractionProgress == 100) "100%" else stringResource(R.string.cancel)
                        } else {
                            stringResource(R.string.extract)
                        },
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Download Update Button
            OtaPrimaryButton(
                text = stringResource(R.string.button_download),
                onClick = { onDownload(ota) },
                icon = Icons.Rounded.Download,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )
        }
    }

    val activeDialogData = cachedPartitionDialogData ?: partitionDialogData
    if (showPartitionDialog && activeDialogData != null) {
        PartitionSelectDialog(
            data = activeDialogData,
            onDismiss = {
                showPartitionDialog = false
                onDismissPartitionDialog()
            },
            onExtractPartitions = onExtractPartitions,
            isStartingExtraction = isStartingExtraction,
            onClearStartingExtraction = onClearStartingExtraction,
            initialSelectedPartitions = selectedPartitions,
            onConfirmSelection = { chosen ->
                selectedPartitions = chosen
                showPartitionDialog = false
                onDismissPartitionDialog()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartitionSelectDialog(
    data: PartitionSelectDialogData,
    onDismiss: () -> Unit,
    onExtractPartitions: (url: String, versionName: String, partitionNames: List<String>) -> UUID? = { _, _, _ -> null },
    isStartingExtraction: Boolean = false,
    onClearStartingExtraction: () -> Unit = {},
    initialSelectedPartitions: Set<PartitionInfo> = emptySet(),
    onConfirmSelection: ((Set<PartitionInfo>) -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedPartitions by remember(data, initialSelectedPartitions) {
        mutableStateOf(initialSelectedPartitions)
    }
    var searchQuery by remember { mutableStateOf("") }

    val filteredPartitions = remember(data.partitions, searchQuery) {
        if (searchQuery.isBlank()) {
            data.partitions.sortedBy { it.name }
        } else {
            data.partitions.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }.sortedBy { it.name }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isHolo = OtaPulseTheme.holographicConfig.isEnabled
    val containerColor = if (isHolo) {
        OtaPulseTheme.extendedColors.glassPanel
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = containerColor,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            // Header matching old design
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.partition_extraction_select_partition),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${selectedPartitions.size}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OtaTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.hint_search_partitions),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
            }

            // Scrollable partition list matching old item_partition_popup layout
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredPartitions, key = { it.name }) { partition ->
                    val isSelected = selectedPartitions.contains(partition)
                    val sizeColor = Color(FormatUtils.getSizeColor(partition.sizeBytes))
                    val sizeCategory = FormatUtils.getSizeCategory(partition.sizeBytes)

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerLow,
                        onClick = {
                            selectedPartitions = if (isSelected) {
                                selectedPartitions - partition
                            } else {
                                selectedPartitions + partition
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 64.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left vertical accent bar
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .heightIn(min = 64.dp)
                                    .fillMaxHeight()
                                    .background(sizeColor)
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 14.dp, top = 14.dp, bottom = 14.dp)
                            ) {
                                Text(
                                    text = partition.name,
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.02.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = sizeColor
                                    ) {
                                        Text(
                                            text = sizeCategory,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                                letterSpacing = 0.05.sp
                                            ),
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Text(
                                        text = "${partition.formattedSize} • ${partition.opCount} ops",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        }
                    }
                }
            }

            // Footer matching old layoutFooter
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val allDisplayedSelected = filteredPartitions.isNotEmpty() && filteredPartitions.all { selectedPartitions.contains(it) }

                    OtaTonalButton(
                        text = if (allDisplayedSelected) stringResource(R.string.partition_extraction_deselect_all) else stringResource(R.string.partition_extraction_select_all),
                        onClick = {
                            if (allDisplayedSelected) {
                                selectedPartitions = selectedPartitions - filteredPartitions.toSet()
                            } else {
                                selectedPartitions = selectedPartitions + filteredPartitions.toSet()
                            }
                        },
                        modifier = Modifier.height(56.dp)
                    )

                    androidx.compose.material3.Button(
                        onClick = {
                            if (isStartingExtraction) {
                                val tag = "extraction_${selectedPartitions.map { it.name }.joinToString("_")}"
                                WorkManager.getInstance(context).cancelAllWorkByTag(tag)
                                onClearStartingExtraction()
                                onDismiss()
                            } else if (selectedPartitions.isNotEmpty()) {
                                if (onConfirmSelection != null) {
                                    onConfirmSelection(selectedPartitions)
                                } else {
                                    onExtractPartitions(
                                        data.url,
                                        data.versionName,
                                        selectedPartitions.map { it.name }
                                    )
                                }
                            }
                        },
                        enabled = isStartingExtraction || selectedPartitions.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp),
                        colors = if (isStartingExtraction) {
                            androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        } else {
                            androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Text(
                            text = if (isStartingExtraction) stringResource(R.string.cancel) else stringResource(R.string.partition_extraction_confirm_selection, selectedPartitions.size),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.6f)
        )
    }
}

@Composable
private fun ActionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = com.abhinav.otapulse.core.ui.theme.OtaPulseMotion.SpringStiff,
        label = "action_icon_scale"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier
            .size(52.dp)
            .scale(scale)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
