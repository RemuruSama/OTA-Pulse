package com.abhinav.otapulse.feature.otatools.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import com.abhinav.otapulse.core.ui.ApplyDialogBlurEffect
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhinav.otapulse.R
import com.abhinav.otapulse.catalog.model.RegionData
import com.abhinav.otapulse.core.common.DeviceUtils
import com.abhinav.otapulse.core.common.FormatUtils
import com.abhinav.otapulse.core.common.OtaShareHelper
import com.abhinav.otapulse.core.common.performHapticFeedback
import com.abhinav.otapulse.core.ui.components.EmptyState
import com.abhinav.otapulse.core.ui.components.ErrorState
import com.abhinav.otapulse.core.ui.components.LoadingState
import com.abhinav.otapulse.core.ui.components.OtaCard
import com.abhinav.otapulse.core.ui.components.OtaOutlinedButton
import com.abhinav.otapulse.core.ui.components.OtaPrimaryButton
import com.abhinav.otapulse.core.ui.components.OtaSwitch
import com.abhinav.otapulse.core.ui.components.OtaTextField
import com.abhinav.otapulse.core.ui.components.OtaTonalButton
import com.abhinav.otapulse.core.ui.components.OtaTopAppBar
import com.abhinav.otapulse.core.ui.theme.OtaPulseTheme
import com.abhinav.otapulse.feature.browser.InAppBrowserActivity
import com.abhinav.otapulse.feature.devicecatalog.ui.OtaDetailsSheet
import com.abhinav.otapulse.feature.devicecatalog.ui.PartitionSelectDialog
import com.abhinav.otapulse.feature.devicecatalog.ui.PartitionSelectDialogData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualQueryScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OtaToolsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val context = LocalContext.current

    // Form inputs
    var productModel by remember { mutableStateOf("RMX3840") }
    var productName by remember { mutableStateOf("") }
    var ruiVersion by remember { mutableStateOf("4") }
    var region by remember { mutableStateOf("GLO") }
    var versionLetter by remember { mutableStateOf("A") }
    var server by remember { mutableStateOf("GL") }

    // Advanced inputs
    var showAdvanced by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var imei by remember { mutableStateOf("0") }
    var beta by remember { mutableStateOf(false) }
    var nvId by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("en-EN") }
    var reqMode by remember { mutableStateOf("manual") }
    var gray by remember { mutableStateOf("0") }

    val ruiOptions = listOf("2", "3", "4", "5", "6", "7")
    val regionOptions = remember { RegionData.regions.map { it.displayName } }
    val letterOptions = listOf("A", "C", "F", "H", "J")
    val serverOptions = listOf("GL", "CN", "IN", "EU")
    val languageOptions = listOf("en-EN", "zh-CN", "ru-RU", "hi-IN", "es-ES")
    val reqModeOptions = listOf("manual", "server_auto", "client_auto", "taste")
    val grayOptions = listOf("0", "1")

    // Dialog & extraction handling
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

    uiState.showOtaDetailsDialog?.let { ota ->
        OtaDetailsSheet(
            ota = ota,
            onDismiss = { viewModel.clearOtaDetailsDialog() },
            onDownload = { selected ->
                viewModel.startDownload(selected, productModel, region)
                viewModel.clearOtaDetailsDialog()
                Toast.makeText(context, context.getString(R.string.toast_download_started), Toast.LENGTH_SHORT).show()
            },
            onCopyLink = { url ->
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText(context.getString(R.string.manual_ota_url_label), url))
                Toast.makeText(context, context.getString(R.string.toast_url_copied), Toast.LENGTH_SHORT).show()
            },
            onViewChangelog = { url ->
                if (url.isNullOrBlank()) {
                    Toast.makeText(context, context.getString(R.string.manual_changelog_unavail), Toast.LENGTH_SHORT).show()
                } else {
                    context.startActivity(InAppBrowserActivity.createIntent(context, url, "Changelog"))
                }
            },
            onShare = { update ->
                val deviceLabel = productName.ifBlank { productModel }
                OtaShareHelper.shareOtaCard(
                    context,
                    com.abhinav.otapulse.core.common.OtaCardData(
                        deviceName = deviceLabel,
                        versionName = update.versionName,
                        regionName = region,
                        androidVersion = update.realAndroidVersion?.removePrefix("Android ")?.trim(),
                        securityPatch = update.securityPatch,
                        size = update.size,
                        arbStatus = update.arbStatus,
                        md5 = update.md5,
                        downloadUrl = update.url,
                        changelogUrl = update.panelUrl,
                        nvId = update.nvId16,
                        projectId = update.oplusSeparateSoft,
                        buildDate = FormatUtils.formatBuildDate(update),
                        targetVersion = update.otaTargetVersion?.ifBlank { null } ?: update.realOtaVersion?.ifBlank { null }
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
            onFetchPartitions = { update -> viewModel.fetchExtractablePartitions(update) },
            partitionDialogData = showPartitionDialogData,
            onDismissPartitionDialog = { viewModel.clearPartitionSelectDialog() },
            onExtractPartitions = { url, versionName, partitionNames ->
                val id = viewModel.extractPartitions(url, versionName, partitionNames)
                viewModel.clearPartitionSelectDialog()
                id
            },
            isStartingExtraction = uiState.isStartingExtraction,
            onClearStartingExtraction = { viewModel.clearStartingExtraction() }
        )
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            confirmButton = {
                ApplyDialogBlurEffect()
                TextButton(onClick = { showInfoDialog = false }) {
                    Text(stringResource(R.string.manual_got_it_btn), style = MaterialTheme.typography.labelLarge)
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Manual Query Guide & Details",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text(
                            text = "Query Action Buttons",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        )
                    }
                    item {
                        InfoDetailRow(
                            title = stringResource(R.string.manual_autofill_title),
                            desc = "Automatically detects and populates your device's Product Model, NV Identifier, and active OTA branch instantly."
                        )
                    }
                    item {
                        InfoDetailRow(
                            title = stringResource(R.string.manual_query_now_title),
                            desc = "Performs a single targeted update check using your exact entered parameters on the selected regional server."
                        )
                    }
                    item {
                        InfoDetailRow(
                            title = stringResource(R.string.manual_multiserver_title),
                            desc = "Concurrently searches all 4 global server endpoints (Global, China, India, and Europe) to detect active updates across regions."
                        )
                    }
                    item {
                        InfoDetailRow(
                            title = stringResource(R.string.manual_fullscan_title),
                            desc = "Exhaustively sweeps across all 5 branch prefix letters (A, C, F, H, J), all 4 server endpoints, AND all 4 request modes (manual, server_auto, client_auto, taste) — 80 total checks — to uncover any hidden or staged OTA builds."
                        )
                    }
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                    item {
                        Text(
                            text = "Query Matrix Parameters",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        )
                    }
                    item {
                        InfoDetailRow(
                            title = stringResource(R.string.manual_target_region_title),
                            desc = "Regional target payload filter: GLO (Global), EU (Europe/EEA), IN (India), CN (China), RU (Russia)."
                        )
                    }
                    item {
                        InfoDetailRow(
                            title = stringResource(R.string.manual_branch_prefix_title),
                            desc = "First letter of the target OTA version string. 'A' is standard OTA, 'C' is ColorOS/RUI crossover, 'F' is carrier/factory build."
                        )
                    }
                    item {
                        InfoDetailRow(
                            title = stringResource(R.string.manual_endpoint_title),
                            desc = "The regional server host queried: GL (otafs.coloros.com/otaupdater), CN, IN, or EU."
                        )
                    }
                    item {
                        InfoDetailRow(
                            title = stringResource(R.string.manual_nv_id_title),
                            desc = "Oppo/Realme NV carrier ID (ro.build.oplus_nv_id) override. When set, overrides default regional checks (101 for GLO/IN, 01000100 for CN, 10000100 for EU)."
                        )
                    }
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            OtaTopAppBar(
                title = stringResource(R.string.tools_manual_query_title),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = {
                        context.performHapticFeedback()
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        context.performHapticFeedback()
                        showInfoDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = stringResource(R.string.manual_guide_cd),
                            tint = MaterialTheme.colorScheme.primary
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Header Card
            item {
                OtaCard(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
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
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Terminal,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "OTA Query Matrix",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.tools_manual_query_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 2. Hardware Identification Card
            item {
                OtaCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SectionHeader(
                            title = stringResource(R.string.manual_hw_id_title),
                            icon = Icons.Rounded.Smartphone
                        )

                        OtaTextField(
                            value = productModel,
                            onValueChange = { productModel = it },
                            label = { Text(stringResource(R.string.product_model_label)) },
                            placeholder = { Text(stringResource(R.string.manual_model_placeholder)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )

                        OtaTextField(
                            value = productName,
                            onValueChange = { productName = it },
                            label = { Text(stringResource(R.string.product_name_label)) },
                            placeholder = { Text(stringResource(R.string.manual_region_placeholder)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                        )
                    }
                }
            }

            // 3. Firmware Matrix Card (Compact Choice Pills)
            item {
                OtaCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SectionHeader(
                            title = stringResource(R.string.manual_target_region_title),
                            icon = Icons.Rounded.Public
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            regionOptions.forEach { opt ->
                                ChoicePill(
                                    text = opt,
                                    selected = region == opt,
                                    onClick = {
                                        context.performHapticFeedback()
                                        region = opt
                                        val found = RegionData.regions.find { it.displayName == opt }
                                        if (found != null && serverOptions.contains(found.serverCode)) {
                                            server = found.serverCode
                                        }
                                    }
                                )
                            }
                        }

                        SectionHeader(
                            title = stringResource(R.string.manual_branch_prefix_title),
                            icon = Icons.Rounded.Info
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            letterOptions.forEach { opt ->
                                ChoicePill(
                                    text = opt,
                                    selected = versionLetter == opt,
                                    onClick = {
                                        context.performHapticFeedback()
                                        versionLetter = opt
                                    }
                                )
                            }
                        }

                        SectionHeader(
                            title = stringResource(R.string.manual_endpoint_title),
                            icon = Icons.Rounded.Dns
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            serverOptions.forEach { opt ->
                                ChoicePill(
                                    text = opt,
                                    selected = server == opt,
                                    onClick = {
                                        context.performHapticFeedback()
                                        server = opt
                                    }
                                )
                            }
                        }

                        SectionHeader(
                            title = stringResource(R.string.manual_req_mode_title),
                            icon = Icons.Rounded.Tune
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            reqModeOptions.forEach { opt ->
                                ChoicePill(
                                    text = opt,
                                    selected = reqMode == opt,
                                    onClick = {
                                        context.performHapticFeedback()
                                        reqMode = opt
                                    }
                                )
                            }
                        }

                        SectionHeader(
                            title = stringResource(R.string.manual_gray_channel_title),
                            icon = Icons.Rounded.Info
                        )
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            grayOptions.forEach { opt ->
                                ChoicePill(
                                    text = if (opt == "0") "Standard (0)" else "Gray Channel ($opt)",
                                    selected = gray == opt,
                                    onClick = {
                                        context.performHapticFeedback()
                                        gray = opt
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 4. Advanced Configuration Accordion Card
            item {
                OtaCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    context.performHapticFeedback()
                                    showAdvanced = !showAdvanced
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Advanced Parameters",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "NV ID, Carrier Override, Beta & Release Channels",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (showAdvanced) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(
                            visible = showAdvanced,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 18.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                HorizontalDivider(
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )

                                OtaTextField(
                                    value = nvId,
                                    onValueChange = { nvId = it },
                                    label = { Text(stringResource(R.string.manual_nv_carrier_label)) },
                                    placeholder = { Text(stringResource(R.string.manual_nv_placeholder)) }
                                )

                                OtaTextField(
                                    value = imei,
                                    onValueChange = { imei = it },
                                    label = { Text(stringResource(R.string.manual_imei_override_label)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )

                                // Beta Switch
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text(
                                                text = "Beta & Early Access Channel",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Target preview OTA servers when available",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        OtaSwitch(
                                            checked = beta,
                                            onCheckedChange = {
                                                context.performHapticFeedback()
                                                beta = it
                                            }
                                        )
                                    }
                                }

                                Text(stringResource(R.string.manual_lang_code_label), style = MaterialTheme.typography.labelMedium)
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    languageOptions.forEach { opt ->
                                        ChoicePill(
                                            text = opt,
                                            selected = language == opt,
                                            onClick = {
                                                context.performHapticFeedback()
                                                language = opt
                                            }
                                        )
                                    }
                                }


                            }
                        }
                    }
                }
            }

            // 5. Query Action Center (Compact 2x2 Grid)
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OtaTonalButton(
                            text = "Auto-Fill",
                            icon = Icons.Rounded.AutoFixHigh,
                            onClick = {
                                context.performHapticFeedback()
                                val model = DeviceUtils.getSystemProperty("ro.product.model")
                                val name = DeviceUtils.getSystemProperty("ro.product.name")
                                val nvIdProp = DeviceUtils.getSystemProperty("ro.build.oplus_nv_id")
                                val letterProp = DeviceUtils.getOtaVersionLetter()

                                if (model.isNotBlank()) productModel = model
                                if (name.isNotBlank()) productName = name
                                if (nvIdProp.isNotBlank()) nvId = nvIdProp
                                if (letterProp.isNotBlank()) versionLetter = letterProp

                                if (name.isNotBlank()) {
                                    val foundRegion = when {
                                        name.endsWith("EEA", ignoreCase = true) -> RegionData.regions.find { it.displayName == "EU" }
                                        name.endsWith("RU", ignoreCase = true) -> RegionData.regions.find { it.displayName == "RU" }
                                        name.endsWith("IN", ignoreCase = true) -> RegionData.regions.find { it.displayName == "IN" }
                                        name.endsWith("CN", ignoreCase = true) -> RegionData.regions.find { it.displayName == "CN" }
                                        else -> RegionData.regions.find { name.endsWith(it.displayName, ignoreCase = true) }
                                    }
                                    if (foundRegion != null) {
                                        region = foundRegion.displayName
                                        if (serverOptions.contains(foundRegion.serverCode)) {
                                            server = foundRegion.serverCode
                                        }
                                    }
                                }
                                Toast.makeText(context, context.getString(R.string.toast_device_details_fetched), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        )

                        OtaPrimaryButton(
                            text = "Query Now",
                            icon = Icons.Rounded.Search,
                            onClick = {
                                context.performHapticFeedback()
                                if (productModel.isBlank()) {
                                    Toast.makeText(context, context.getString(R.string.manual_err_no_model), Toast.LENGTH_SHORT).show()
                                    return@OtaPrimaryButton
                                }
                                viewModel.sendRequest(
                                    model = productModel,
                                    displayDeviceName = productName.ifBlank { productModel },
                                    otaVersion = versionLetter,
                                    ruiVersion = ruiVersion.toIntOrNull() ?: 4,
                                    region = region,
                                    server = server,
                                    regionsArray = regionOptions.toTypedArray(),
                                    imei = imei.ifBlank { "0" },
                                    beta = beta,
                                    nvId = nvId.ifBlank { null },
                                    language = language,
                                    reqMode = reqMode,
                                    gray = gray.toIntOrNull() ?: 0,
                                    autoShowDialog = true
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OtaTonalButton(
                            text = "Multi-Server",
                            icon = Icons.Rounded.Public,
                            onClick = {
                                context.performHapticFeedback()
                                viewModel.sendRequestAcrossServers(
                                    model = productModel,
                                    otaVersion = versionLetter,
                                    ruiVersion = ruiVersion.toIntOrNull() ?: 4,
                                    region = region,
                                    servers = serverOptions,
                                    regionsArray = regionOptions.toTypedArray(),
                                    imei = imei.ifBlank { "0" },
                                    beta = beta,
                                    nvId = nvId.ifBlank { null },
                                    language = language,
                                    reqMode = reqMode,
                                    gray = gray.toIntOrNull() ?: 0,
                                    displayDeviceName = productName.ifBlank { productModel }
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        )

                        OtaPrimaryButton(
                            text = "Full Scan",
                            icon = Icons.Rounded.Speed,
                            onClick = {
                                context.performHapticFeedback()
                                viewModel.sendRequestAcrossVersionsAndServers(
                                    model = productModel,
                                    displayDeviceName = productName.ifBlank { productModel },
                                    baseOtaVersion = versionLetter,
                                    ruiVersion = ruiVersion.toIntOrNull() ?: 4,
                                    region = region,
                                    servers = serverOptions,
                                    letters = letterOptions,
                                    reqModes = reqModeOptions,
                                    imei = imei.ifBlank { "0" },
                                    beta = beta,
                                    nvId = nvId.ifBlank { null },
                                    language = language,
                                    reqMode = reqMode,
                                    gray = gray.toIntOrNull() ?: 0
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        )
                    }
                }
            }

            // 6. Loading State
            if (uiState.isLoading) {
                item {
                    LoadingState(
                        message = stringResource(R.string.manual_querying_msg),
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
                }
            }

            // 7. Results Section (Deduplicated single vs multi results)
            val hasMultiResults = !uiState.multiResults.isNullOrEmpty()
            val singleSuccessOta = if (!hasMultiResults && uiState.result?.isSuccess == true) uiState.result?.getOrNull() else null

            if (hasMultiResults) {
                item {
                    Text(
                        text = "Multi-Search Results (${uiState.multiResults?.size ?: 0})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                items(uiState.multiResults ?: emptyList()) { ota ->
                    QueryOtaResultCard(
                        ota = ota,
                        productModel = productModel,
                        region = region,
                        viewModel = viewModel
                    )
                }
            } else if (singleSuccessOta != null) {
                item {
                    Text(
                        text = "Query Result",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                item {
                    QueryOtaResultCard(
                        ota = singleSuccessOta,
                        productModel = productModel,
                        region = region,
                        viewModel = viewModel
                    )
                }
            } else if (uiState.result?.isFailure == true) {
                item {
                    ErrorState(
                        message = uiState.result?.exceptionOrNull()?.localizedMessage ?: "No firmware update found for this query.",
                        onRetry = null
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QueryOtaResultCard(
    ota: com.abhinav.otapulse.core.model.OtaUpdate,
    productModel: String,
    region: String,
    viewModel: OtaToolsViewModel
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uiState by viewModel.uiState.collectAsState()

    val rawTitle = ota.versionName ?: ota.realVersionName ?: ota.realOtaVersion ?: "OTA Update"
    val buildTitle = rawTitle.substringBefore(" [").trim()
    val metadataStr = if (rawTitle.contains("[")) rawTitle.substringAfter("[").substringBefore("]") else ""

    OtaCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        onClick = {
            context.performHapticFeedback()
            viewModel.showOtaDetails(ota)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Header: Build Title + Size Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = ota.size.ifBlank { "Unknown" },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Metadata Chips (Branch, Mode, Server)
            if (metadataStr.isNotBlank()) {
                val tags = metadataStr.split("•").map { it.trim() }.filter { it.isNotBlank() }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tags.forEach { tag ->
                        val cleanTag = tag.removePrefix("Mode: ").removePrefix("Server: ").trim()
                        val isBranch = tag.startsWith("Branch", ignoreCase = true)
                        val isServer = tag.startsWith("Server:", ignoreCase = true)
                        val chipColor = when {
                            isBranch -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            isServer -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                            else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                        }
                        val textColor = when {
                            isBranch -> MaterialTheme.colorScheme.primary
                            isServer -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.secondary
                        }
                        Surface(
                            color = chipColor,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = cleanTag,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = textColor,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Specs Row: Android OS & ARB Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Android,
                        contentDescription = stringResource(R.string.manual_android_os_cd),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Android ${ota.realAndroidVersion?.removePrefix("Android ")?.trim() ?: ota.androidVersion ?: "Unknown"}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val arbStatusText = ota.arbStatus ?: "Safe / 0"
                val arbColor = when {
                    arbStatusText.equals("Safe", ignoreCase = true) -> OtaPulseTheme.extendedColors.arbSafe
                    arbStatusText.contains("Protected", ignoreCase = true) -> OtaPulseTheme.extendedColors.arbWarning
                    else -> MaterialTheme.colorScheme.primary
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.VerifiedUser,
                        contentDescription = stringResource(R.string.manual_arb_status_cd),
                        tint = arbColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "ARB: $arbStatusText",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = arbColor
                    )
                }
            }

            // Download URL Code Box (Tap to Copy)
            Surface(
                onClick = {
                    context.performHapticFeedback()
                    clipboardManager.setText(AnnotatedString(ota.downloadUrl))
                    Toast.makeText(context, context.getString(R.string.manual_url_copied_toast), Toast.LENGTH_SHORT).show()
                },
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Link,
                            contentDescription = stringResource(R.string.manual_url_cd),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = ota.downloadUrl.substringAfterLast("/").ifEmpty { ota.downloadUrl },
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = stringResource(R.string.manual_copy_url_cd),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Copy",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Action Buttons: 50% split, short clean labels
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OtaPrimaryButton(
                    text = "Download",
                    icon = Icons.Rounded.Download,
                    onClick = {
                        viewModel.startDownload(ota, productModel, region)
                        Toast.makeText(context, context.getString(R.string.manual_download_queued), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f).height(42.dp),
                    compact = true
                )
                val isExtractLoading = uiState.isFetchingPartitions && uiState.fetchingSource == ota.url
                OtaTonalButton(
                    text = if (isExtractLoading) "Loading..." else "Extract",
                    icon = Icons.Rounded.Unarchive,
                    onClick = { viewModel.fetchExtractablePartitions(ota) },
                    modifier = Modifier.weight(1f).height(42.dp),
                    isLoading = isExtractLoading,
                    compact = true
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ChoicePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoDetailRow(title: String, desc: String) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
