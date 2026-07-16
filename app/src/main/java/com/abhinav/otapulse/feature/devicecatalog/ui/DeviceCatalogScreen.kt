package com.abhinav.otapulse.feature.devicecatalog.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.abhinav.otapulse.core.ui.ApplyDialogBlurEffect
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhinav.otapulse.R
import com.abhinav.otapulse.core.common.FormatUtils
import com.abhinav.otapulse.core.common.OtaCardData
import com.abhinav.otapulse.core.common.OtaShareHelper
import com.abhinav.otapulse.core.common.performHapticFeedback
import com.abhinav.otapulse.core.model.Device
import com.abhinav.otapulse.core.model.OtaUpdate
import com.abhinav.otapulse.core.model.RegionVariant
import com.abhinav.otapulse.core.ui.components.BentoGrid
import com.abhinav.otapulse.core.ui.components.EmptyState
import com.abhinav.otapulse.core.ui.components.ErrorState
import com.abhinav.otapulse.core.ui.components.FloatingSearchBar
import com.abhinav.otapulse.core.ui.components.LoadingState
import com.abhinav.otapulse.core.ui.components.OtaCard
import com.abhinav.otapulse.core.ui.components.OtaOutlinedButton
import com.abhinav.otapulse.core.ui.components.OtaPrimaryButton
import com.abhinav.otapulse.core.ui.components.OtaTopAppBar
import com.abhinav.otapulse.core.ui.theme.OtaPulseMotion
import com.abhinav.otapulse.core.ui.theme.OtaPulseTheme
import com.abhinav.otapulse.feature.browser.InAppBrowserActivity
import com.abhinav.otapulse.feature.devices.ui.DevicesUiState
import com.abhinav.otapulse.feature.devices.ui.DevicesViewModel
import com.abhinav.otapulse.feature.devices.ui.PendingDownload
import com.abhinav.otapulse.feature.otatools.ui.JsonOutputActivity

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DeviceCatalogScreen(
    onNavigateToAddDevice: (Device?) -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    viewModel: DevicesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    var showSearchInput by rememberSaveable { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(showSearchInput) {
        if (showSearchInput) {
            kotlinx.coroutines.delay(100)
            try {
                searchFocusRequester.requestFocus()
            } catch (e: Exception) {
                // Ignore if not attached yet
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            OtaTopAppBar(
                title = stringResource(R.string.title_devices),
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(
                        onClick = {
                            context.performHapticFeedback()
                            onNavigateToHistory()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = stringResource(R.string.catalog_update_history_cd),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                bottomContent = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Brand Filter Chips directly inside header with full-width scroll & padding
                        val brands = listOf("All", "OnePlus", "Realme", "OPPO")
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
                        ) {
                            items(brands) { brand ->
                                val isSelected = uiState.selectedBrand.equals(brand, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        context.performHapticFeedback()
                                        viewModel.onBrandSelected(brand)
                                    },
                                    label = {
                                        Text(
                                            text = brand,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                                            )
                                        }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    shape = MaterialTheme.shapes.small
                                )
                            }
                        }

                        // Search Bar inside header when active
                        androidx.compose.animation.AnimatedVisibility(visible = showSearchInput || uiState.searchQuery.isNotEmpty()) {
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 8.dp)
                            ) {
                                FloatingSearchBar(
                                    query = uiState.searchQuery,
                                    onQueryChange = { viewModel.onSearchQueryChanged(it) },
                                    placeholder = stringResource(R.string.search_device_hint),
                                    onClear = {
                                        context.performHapticFeedback()
                                        focusManager.clearFocus()
                                    },
                                    focusRequester = searchFocusRequester
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            val navBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = navBarsBottom + 88.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        context.performHapticFeedback()
                        showSearchInput = !showSearchInput
                        if (!showSearchInput) {
                            viewModel.onSearchQueryChanged("")
                            focusManager.clearFocus()
                        }
                    },
                    containerColor = if (showSearchInput) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (showSearchInput) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = if (showSearchInput) Icons.Rounded.Clear else Icons.Rounded.Search,
                        contentDescription = stringResource(R.string.catalog_search_devices_cd)
                    )
                }

                FloatingActionButton(
                    onClick = {
                        context.performHapticFeedback()
                        onNavigateToAddDevice(null)
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.catalog_add_custom_dev_cd)
                    )
                }
            }
        }
    ) { paddingValues ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main Content Area with Pull-To-Refresh support
            PullToRefreshBox(
                isRefreshing = uiState.isSyncingCatalog,
                onRefresh = {
                    context.performHapticFeedback()
                    viewModel.forceSyncCatalog()
                },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    uiState.isLoading && uiState.devices.isEmpty() -> {
                        LoadingState(
                            message = stringResource(R.string.catalog_syncing_msg),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    uiState.errorMessage != null && uiState.devices.isEmpty() -> {
                        ErrorState(
                            message = uiState.errorMessage ?: "Failed to load catalog",
                            onRetry = { viewModel.refreshDevices() },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    uiState.devices.isEmpty() -> {
                        EmptyState(
                            icon = Icons.Rounded.PhoneAndroid,
                            title = stringResource(R.string.catalog_no_devices_title),
                            message = stringResource(R.string.catalog_no_devices_msg),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        val navBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 96.dp + navBarsBottom // Padding for Extended FAB & Bottom Bar
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = uiState.devices,
                                key = { it.name }
                            ) { device ->
                                DeviceItemCard(
                                    device = device,
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    onNavigateToEditDevice = { onNavigateToAddDevice(it) },
                                    onDeleteCustomDevice = { viewModel.deleteCustomDevice(it.name) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // OTA Details Bottom Sheet
    uiState.showOtaDetailsDialog?.let { dialogData ->
        OtaDetailsSheet(
            ota = dialogData.otaUpdate,
            onDismiss = { viewModel.clearOtaDetailsDialog() },
            onDownload = { selected ->
                viewModel.startDownload(selected, dialogData.deviceName, dialogData.regionName)
                Toast.makeText(context, context.getString(R.string.catalog_download_started), Toast.LENGTH_SHORT).show()
                viewModel.clearOtaDetailsDialog()
            },
            onCopyLink = { url ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.catalog_ota_url_label), url))
                Toast.makeText(context, context.getString(R.string.toast_link_copied), Toast.LENGTH_SHORT).show()
            },
            onViewChangelog = { url ->
                if (url.isNullOrBlank()) {
                    Toast.makeText(context, context.getString(R.string.catalog_changelog_unavail), Toast.LENGTH_SHORT).show()
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
            isFetchingPartitions = uiState.isFetchingPartitions,
            onFetchPartitions = { selected -> viewModel.fetchExtractablePartitions(selected) },
            partitionDialogData = uiState.showPartitionSelectDialog,
            onDismissPartitionDialog = { viewModel.clearPartitionSelectDialog() },
            onExtractPartitions = { url, ver, parts ->
                viewModel.extractPartitions(url, ver, parts, dialogData.regionName)
            },
            isStartingExtraction = uiState.isStartingExtraction,
            onClearStartingExtraction = { viewModel.clearStartingExtraction() }
        )
    }

    // Overwrite Confirmation Dialog
    uiState.pendingDownload?.let { pending ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelPendingDownload() },
            title = {
                Text(
                    text = "File Already Exists",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "The file '${pending.targetFile.name}' has already been downloaded. Do you want to overwrite it?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                ApplyDialogBlurEffect()
                TextButton(onClick = {
                    context.performHapticFeedback()
                    viewModel.confirmOverwriteDownload()
                }) {
                    Text(stringResource(R.string.catalog_overwrite_label), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    context.performHapticFeedback()
                    viewModel.cancelPendingDownload()
                }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeviceItemCard(
    device: Device,
    uiState: DevicesUiState,
    viewModel: DevicesViewModel,
    onNavigateToEditDevice: (Device) -> Unit,
    onDeleteCustomDevice: (Device) -> Unit
) {
    val context = LocalContext.current
    var isExpanded by rememberSaveable(device.name) { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val defaultVersion = remember(device.name, device.firmwareGroups) {
        device.firmwareGroups.keys.maxOrNull()
    }
    var selectedVersion by remember(device.name, device.firmwareGroups) {
        mutableStateOf(defaultVersion)
    }
    var selectedVariant by remember(device.name) {
        mutableStateOf<RegionVariant?>(null)
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_custom_device_title)) },
            text = { Text(stringResource(R.string.delete_custom_device_message, device.name)) },
            confirmButton = {
                ApplyDialogBlurEffect()
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDeleteCustomDevice(device)
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    OtaCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .animateContentSize(OtaPulseMotion.SpringMediumSize),
        shape = RoundedCornerShape(20.dp),
        onClick = {
            context.performHapticFeedback()
            isExpanded = !isExpanded
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = selectedVariant?.productName ?: "Tap to view variants",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            context.performHapticFeedback()
                            viewModel.toggleFavorite(device.name)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (device.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = stringResource(R.string.catalog_toggle_fav_cd),
                            tint = if (device.isFavorite) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (device.isCustom) {
                        Box {
                            IconButton(
                                onClick = {
                                    context.performHapticFeedback()
                                    showMenu = true
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = stringResource(R.string.history_more_options_cd),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.catalog_edit_device_btn)) },
                                    leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onNavigateToEditDevice(device)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        showDeleteConfirm = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Expanded Section
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Android Versions
                    if (device.firmwareGroups.keys.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Android Version",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                device.firmwareGroups.keys.forEach { version ->
                                    val isSelected = version == selectedVersion
                                    Surface(
                                        onClick = {
                                            context.performHapticFeedback()
                                            selectedVersion = version
                                            selectedVariant = null
                                        },
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ) {
                                        Text(
                                            text = version,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Regional Variants
                    val variants = selectedVersion?.let { device.firmwareGroups[it] } ?: emptyList()
                    if (variants.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Regional Variant",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                variants.forEach { variant ->
                                    val isSelected = variant == selectedVariant
                                    Surface(
                                        onClick = {
                                            context.performHapticFeedback()
                                            selectedVariant = variant
                                            viewModel.fetchOtaDetails(device, variant)
                                        },
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                        contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ) {
                                        Text(
                                            text = variant.displayName,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Loading Details or Error
                    if (device.isLoadingDetails) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Fetching update info...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (selectedVariant != null) {
                        val deviceKey = "${device.name}_${selectedVariant!!.displayName}"
                        val otaResult = uiState.otaDetails[deviceKey]
                        if (otaResult?.isFailure == true) {
                            val errorMsg = otaResult.exceptionOrNull()?.message ?: "Failed to retrieve update."
                            val displayMsg = when {
                                errorMsg.contains("No update", ignoreCase = true) -> "No new update is available."
                                errorMsg.contains("Server Error: 2004", ignoreCase = true) -> "No update found for this variant."
                                errorMsg.contains("network", ignoreCase = true) -> "Network error. Check connection."
                                else -> errorMsg
                            }
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = displayMsg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                OtaOutlinedButton(
                                    text = "Retry",
                                    onClick = {
                                        context.performHapticFeedback()
                                        viewModel.fetchOtaDetails(device, selectedVariant!!)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
