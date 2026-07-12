package com.abhinav.otapulse.feature.otatools.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.abhinav.otapulse.arb.worker.PartitionExtractorWorker
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhinav.otapulse.R
import com.abhinav.otapulse.core.common.performHapticFeedback
import com.abhinav.otapulse.core.ui.components.OtaCard
import com.abhinav.otapulse.core.ui.components.OtaOutlinedButton
import com.abhinav.otapulse.core.ui.components.OtaPrimaryButton
import com.abhinav.otapulse.core.ui.components.OtaTextField
import com.abhinav.otapulse.core.ui.components.OtaTopAppBar
import com.abhinav.otapulse.feature.devicecatalog.ui.PartitionSelectDialog
import com.abhinav.otapulse.feature.devicecatalog.ui.PartitionSelectDialogData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartitionExtractionScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OtaToolsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val context = LocalContext.current

    androidx.compose.runtime.LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearUserMessage()
        }
    }

    val activeWorkId = uiState.activeExtractionWorkId
    val workInfo by produceState<WorkInfo?>(initialValue = null, activeWorkId) {
        val id = activeWorkId
        if (id != null) {
            WorkManager.getInstance(context)
                .getWorkInfoByIdFlow(id)
                .collect { value = it }
        } else {
            value = null
        }
    }

    val isExtracting = (uiState.isStartingExtraction && activeWorkId == null) ||
            (workInfo != null && workInfo?.state != WorkInfo.State.CANCELLED && workInfo?.state != WorkInfo.State.FAILED && workInfo?.state != WorkInfo.State.SUCCEEDED)

    val extractionProgress: Int? = remember(isExtracting, workInfo) {
        when {
            workInfo?.state == WorkInfo.State.SUCCEEDED -> 100
            workInfo != null && !workInfo?.state!!.isFinished -> {
                val p = workInfo?.progress?.getInt(PartitionExtractorWorker.PROGRESS_KEY, -1) ?: -1
                if (p != -1) p else null
            }
            else -> null
        }
    }

    var inputUrl by remember { mutableStateOf("") }
    var selectedLocalUri by remember { mutableStateOf<Uri?>(null) }
    var selectedLocalName by remember { mutableStateOf("") }

    val pickLocalZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }
            selectedLocalUri = uri
            selectedLocalName = resolveDisplayName(context, uri)
        }
    }

    val showPartitionDialogData = uiState.showPartitionSelectDialog?.let {
        PartitionSelectDialogData(
            url = it.source,
            versionName = it.versionName,
            partitions = it.partitions
        )
    }

    if (showPartitionDialogData != null) {
        PartitionSelectDialog(
            data = showPartitionDialogData,
            onDismiss = { viewModel.clearPartitionSelectDialog() },
            onExtractPartitions = { url, versionName, partitionNames ->
                val id = viewModel.extractPartitions(url, versionName, partitionNames)
                viewModel.clearPartitionSelectDialog()
                Toast.makeText(context, context.getString(R.string.toast_partition_extraction_started), Toast.LENGTH_SHORT).show()
                id
            },
            isStartingExtraction = uiState.isStartingExtraction,
            onClearStartingExtraction = { viewModel.clearStartingExtraction() }
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            OtaTopAppBar(
                title = stringResource(R.string.tools_partition_extraction_title),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = {
                        context.performHapticFeedback()
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            item {
                OtaCard(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_extract_stroke),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondary,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "PAYLOAD_DUMPER.sh",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.tools_partition_extraction_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (activeWorkId != null || uiState.isStartingExtraction) {
                item {
                    OtaCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Memory,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Extraction Status",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = when (workInfo?.state) {
                                        WorkInfo.State.SUCCEEDED -> MaterialTheme.colorScheme.primaryContainer
                                        WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> MaterialTheme.colorScheme.errorContainer
                                        else -> MaterialTheme.colorScheme.secondaryContainer
                                    }
                                ) {
                                    Text(
                                        text = when (workInfo?.state) {
                                            WorkInfo.State.SUCCEEDED -> "Completed"
                                            WorkInfo.State.FAILED -> "Failed"
                                            WorkInfo.State.CANCELLED -> "Cancelled"
                                            else -> "In Progress..."
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = when (workInfo?.state) {
                                            WorkInfo.State.SUCCEEDED -> MaterialTheme.colorScheme.onPrimaryContainer
                                            WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> MaterialTheme.colorScheme.onErrorContainer
                                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                                        },
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            if (uiState.activeExtractionNames.isNotEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Target Partitions (${uiState.activeExtractionNames.size})",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = if (isExtracting) "Writing to Storage" else "Status: Done",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Text(
                                            text = uiState.activeExtractionNames.joinToString(", "),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            if (isExtracting) {
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
                                            text = if (extractionProgress != null) "Extracting payload streams..." else "Initializing extraction engine...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (extractionProgress != null) {
                                            Text(
                                                text = "$extractionProgress%",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    if (extractionProgress != null && extractionProgress >= 0) {
                                        LinearProgressIndicator(
                                            progress = { extractionProgress / 100f },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                        )
                                    } else {
                                        LinearProgressIndicator(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                        )
                                    }
                                }
                            } else if (workInfo?.state == WorkInfo.State.SUCCEEDED) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .padding(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Successfully extracted and verified partition images to storage.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                if (isExtracting && activeWorkId != null) {
                                    OtaOutlinedButton(
                                        text = stringResource(R.string.cancel),
                                        icon = Icons.Rounded.Close,
                                        onClick = {
                                            context.performHapticFeedback()
                                            viewModel.cancelPartitionExtraction(activeWorkId, uiState.activeExtractionNames.firstOrNull() ?: "")
                                        },
                                        compact = true
                                    )
                                } else {
                                    OtaPrimaryButton(
                                        text = "Dismiss & Clear",
                                        icon = Icons.Rounded.CheckCircle,
                                        onClick = {
                                            context.performHapticFeedback()
                                            viewModel.clearActiveExtraction()
                                        },
                                        compact = true
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 1: URL Input
            item {
                OtaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Link,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.partition_extraction_url_label),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        OtaTextField(
                            value = inputUrl,
                            onValueChange = { inputUrl = it },
                            label = { Text(stringResource(R.string.partition_extraction_url_placeholder)) },
                            placeholder = { Text("https://gaota-gl.realme.com/...") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            trailingIcon = {
                                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                                    Box(modifier = Modifier.padding(end = 12.dp)) {
                                        if (inputUrl.isNotEmpty()) {
                                            IconButton(
                                                onClick = {
                                                    context.performHapticFeedback()
                                                    inputUrl = ""
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Close,
                                                    contentDescription = stringResource(R.string.clear_url),
                                                    modifier = Modifier.size(20.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        } else {
                                            IconButton(
                                                onClick = {
                                                    context.performHapticFeedback()
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    val clipData = clipboard.primaryClip
                                                    if (clipData != null && clipData.itemCount > 0) {
                                                        inputUrl = clipData.getItemAt(0).text.toString()
                                                        Toast.makeText(context, context.getString(R.string.toast_link_pasted), Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, context.getString(R.string.toast_clipboard_empty), Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_paste_stroke),
                                                    contentDescription = stringResource(R.string.paste_url),
                                                    modifier = Modifier.size(20.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        )

                        val isUrlFetching = uiState.isFetchingPartitions && uiState.fetchingSource == inputUrl.trim()
                        OtaPrimaryButton(
                            text = if (isUrlFetching) "Fetching Partitions..." else stringResource(R.string.partition_extraction_load_url_btn),
                            icon = if (isUrlFetching) null else Icons.Rounded.Search,
                            isLoading = isUrlFetching,
                            onClick = {
                                context.performHapticFeedback()
                                val trimmed = inputUrl.trim()
                                if (trimmed.isBlank()) {
                                    Toast.makeText(context, context.getString(R.string.toast_paste_ota_url_first), Toast.LENGTH_SHORT).show()
                                    return@OtaPrimaryButton
                                }
                                val guessName = guessVersionNameFromUrl(trimmed)
                                viewModel.fetchExtractablePartitions(
                                    source = trimmed,
                                    versionName = guessName,
                                    sourceLabel = "Full OTA URL"
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Section 2: Local ZIP File
            item {
                OtaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FolderZip,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.partition_extraction_local_zip_title),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        if (selectedLocalUri == null) {
                            // Sleek Interactive Dropzone Box when no file is chosen
                            Surface(
                                onClick = {
                                    context.performHapticFeedback()
                                    pickLocalZipLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(26.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.UploadFile,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Text(
                                        text = "Tap to pick local OTA .zip file",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Supports payload.bin inside full or incremental recovery ZIPs",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            // Sleek Selected File Preview Card (clickable to change or clear via X)
                            Surface(
                                onClick = {
                                    context.performHapticFeedback()
                                    pickLocalZipLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                                },
                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = selectedLocalName.ifBlank { "Selected ZIP Package" },
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Tap to pick a different file",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            context.performHapticFeedback()
                                            selectedLocalUri = null
                                            selectedLocalName = ""
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Remove file",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            val isLocalFetching = uiState.isFetchingPartitions && uiState.fetchingSource == selectedLocalUri?.toString()
                            OtaPrimaryButton(
                                text = if (isLocalFetching) "Scanning Partitions..." else stringResource(R.string.partition_extraction_load_local_btn),
                                icon = if (isLocalFetching) null else Icons.Rounded.Search,
                                isLoading = isLocalFetching,
                                onClick = {
                                    context.performHapticFeedback()
                                    val uri = selectedLocalUri
                                    if (uri == null) {
                                        Toast.makeText(context, context.getString(R.string.toast_choose_local_zip_first), Toast.LENGTH_SHORT).show()
                                        return@OtaPrimaryButton
                                    }
                                    viewModel.fetchExtractablePartitions(
                                        source = uri.toString(),
                                        versionName = selectedLocalName.ifBlank { "Local OTA" },
                                        sourceLabel = selectedLocalName.ifBlank { "Local OTA ZIP" }
                                    )
                                },
                                enabled = selectedLocalUri != null,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun guessVersionNameFromUrl(url: String): String {
    val cleanUrl = url.substringBefore("?")
    val fileName = cleanUrl.substringAfterLast("/")
    val withoutExt = if (fileName.endsWith(".zip", ignoreCase = true)) {
        fileName.dropLast(4)
    } else {
        fileName
    }
    return withoutExt.ifBlank { "Full OTA" }
}

private fun resolveDisplayName(context: android.content.Context, uri: Uri): String {
    var name = ""
    if (uri.scheme == "content") {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        name = cursor.getString(index)
                    }
                }
            }
        } catch (_: Exception) {
        }
    }
    if (name.isBlank()) {
        name = uri.path?.substringAfterLast("/") ?: "OTA.zip"
    }
    return name
}
