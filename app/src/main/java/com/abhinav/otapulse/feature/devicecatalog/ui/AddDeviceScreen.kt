package com.abhinav.otapulse.feature.devicecatalog.ui

import com.abhinav.otapulse.R

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.abhinav.otapulse.core.ui.ApplyDialogBlurEffect
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhinav.otapulse.catalog.model.PredefinedDevice
import com.abhinav.otapulse.catalog.model.RegionData
import com.abhinav.otapulse.catalog.model.RegionInfo
import com.abhinav.otapulse.core.common.performHapticFeedback
import com.abhinav.otapulse.core.model.RegionVariant
import com.abhinav.otapulse.core.ui.components.EmptyState
import com.abhinav.otapulse.core.ui.components.OtaCard
import com.abhinav.otapulse.core.ui.components.OtaPrimaryButton
import com.abhinav.otapulse.core.ui.components.OtaTextField
import com.abhinav.otapulse.core.ui.components.OtaTonalButton
import com.abhinav.otapulse.core.ui.components.OtaTopAppBar
import com.abhinav.otapulse.feature.devices.ui.AddDeviceViewModel
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddDeviceScreen(
    onNavigateBack: () -> Unit = {},
    deviceToEdit: PredefinedDevice? = null,
    viewModel: AddDeviceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    var showAddGroupDialog by remember { mutableStateOf(false) }
    var groupToEdit by remember { mutableStateOf<String?>(null) }
    var groupToDelete by remember { mutableStateOf<String?>(null) }
    var groupToAddVariant by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(deviceToEdit) {
        if (deviceToEdit != null) {
            viewModel.setEditDevice(deviceToEdit)
        }
    }

    LaunchedEffect(uiState.isSaveSuccess) {
        if (uiState.isSaveSuccess) {
            val msg = if (uiState.isEditMode) "Device updated successfully!" else "Device saved successfully!"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.resetSaveSuccess()
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            OtaTopAppBar(
                title = if (uiState.isEditMode) "Edit Custom Device" else "Add Custom Device",
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = {
                        context.performHapticFeedback()
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    OtaPrimaryButton(
                        text = if (uiState.isEditMode) "Update Device" else "Save Device",
                        onClick = {
                            focusManager.clearFocus()
                            context.performHapticFeedback()
                            viewModel.saveDevice(uiState.deviceName)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Basic Info Section
            OtaCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Device Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OtaTextField(
                        value = uiState.deviceName,
                        onValueChange = { viewModel.onDeviceNameChanged(it) },
                        label = { Text(stringResource(R.string.add_dev_device_name_label)) },
                        placeholder = { Text(stringResource(R.string.add_dev_enter_mkt_name)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        )
                    )
                }
            }

            // Firmware Groups Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Firmware Groups (${uiState.firmwareGroups.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OtaTonalButton(
                        text = "Add Group",
                        onClick = {
                            context.performHapticFeedback()
                            showAddGroupDialog = true
                        },
                        icon = Icons.Rounded.Add
                    )
                }

                if (uiState.firmwareGroups.isEmpty()) {
                    EmptyState(
                        icon = Icons.Rounded.Add,
                        title = stringResource(R.string.add_dev_no_fw_groups),
                        message = stringResource(R.string.add_dev_no_fw_groups_msg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    )
                } else {
                    uiState.firmwareGroups.forEach { (groupName, variants) ->
                        OtaCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Group Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = groupName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                context.performHapticFeedback()
                                                groupToEdit = groupName
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Edit,
                                                contentDescription = stringResource(R.string.add_dev_edit_group_cd),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                context.performHapticFeedback()
                                                groupToDelete = groupName
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Delete,
                                                contentDescription = stringResource(R.string.add_dev_delete_group_cd),
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                // Variants List
                                if (variants.isEmpty()) {
                                    Text(
                                        text = "No regional variants added yet.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        variants.forEach { variant ->
                                            Surface(
                                                shape = MaterialTheme.shapes.small,
                                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = variant.displayName,
                                                            style = MaterialTheme.typography.labelLarge,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = "${variant.productModel} • ${variant.productName}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            context.performHapticFeedback()
                                                            viewModel.removeVariantFromGroup(groupName, variant)
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Delete,
                                                            contentDescription = stringResource(R.string.add_dev_remove_variant_cd),
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Add Variant Button
                                OtaTonalButton(
                                    text = "Add Variant",
                                    onClick = {
                                        context.performHapticFeedback()
                                        groupToAddVariant = groupName
                                    },
                                    icon = Icons.Rounded.Add,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddGroupDialog) {
        var groupInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddGroupDialog = false },
            title = { Text(stringResource(R.string.add_dev_add_fw_group_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.add_dev_enter_ver_name), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    OtaTextField(
                        value = groupInput,
                        onValueChange = { groupInput = it },
                        placeholder = { Text(stringResource(R.string.add_dev_ver_placeholder)) }
                    )
                }
            },
            confirmButton = {
                ApplyDialogBlurEffect()
                TextButton(onClick = {
                    showAddGroupDialog = false
                    viewModel.addFirmwareGroup(groupInput.trim())
                }) {
                    Text(stringResource(R.string.action_add), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGroupDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    groupToEdit?.let { oldName ->
        var newName by remember(oldName) { mutableStateOf(oldName) }
        AlertDialog(
            onDismissRequest = { groupToEdit = null },
            title = { Text(stringResource(R.string.add_dev_edit_fw_group_title)) },
            text = {
                OtaTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.add_dev_group_name_label)) }
                )
            },
            confirmButton = {
                ApplyDialogBlurEffect()
                TextButton(onClick = {
                    val nameToSave = newName.trim()
                    groupToEdit = null
                    viewModel.editFirmwareGroup(oldName, nameToSave)
                }) {
                    Text(stringResource(R.string.action_save), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToEdit = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    groupToDelete?.let { groupName ->
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            title = { Text(stringResource(R.string.add_dev_delete_group_title)) },
            text = { Text(stringResource(R.string.add_dev_delete_group_msg, groupName)) },
            confirmButton = {
                ApplyDialogBlurEffect()
                TextButton(onClick = {
                    groupToDelete = null
                    viewModel.deleteFirmwareGroup(groupName)
                }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    groupToAddVariant?.let { groupName ->
        AddVariantDialog(
            groupName = groupName,
            onDismiss = { groupToAddVariant = null },
            onAddVariant = { model, name, region, letter, reqMode, gray, server ->
                groupToAddVariant = null
                viewModel.addVariantToGroup(
                    androidVersion = groupName,
                    productModel = model,
                    productName = name,
                    selectedRegion = region,
                    versionLetter = letter,
                    reqMode = reqMode,
                    gray = gray,
                    server = server
                )
            }
        )
    }
}

@Composable
private fun AddVariantDialog(
    groupName: String,
    onDismiss: () -> Unit,
    onAddVariant: (model: String, name: String, region: RegionInfo, letter: String, reqMode: String, gray: Int, server: String) -> Unit
) {
    var productModel by remember { mutableStateOf("") }
    var productName by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf<RegionInfo?>(RegionData.regions.firstOrNull()) }
    
    val servers = listOf("GL", "CN", "IN", "EU")
    var selectedServer by remember { mutableStateOf("GL") }
    
    val versionLetters = listOf("A", "C", "F", "H", "J")
    var selectedLetter by remember { mutableStateOf("A") }
    
    val reqModes = listOf("manual", "server_auto", "client_auto", "taste")
    var selectedReqMode by remember { mutableStateOf("manual") }
    
    val grayValues = listOf(0, 1)
    var selectedGray by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Add Variant to $groupName",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Configure firmware query parameters",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AddVariantSectionHeader(
                    title = stringResource(R.string.add_dev_hw_id_title),
                    icon = Icons.Rounded.Smartphone
                )

                    OtaTextField(
                        value = productModel,
                        onValueChange = { productModel = it },
                        label = { Text(stringResource(R.string.add_dev_model_label)) },
                        placeholder = { Text(stringResource(R.string.add_dev_model_placeholder)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    OtaTextField(
                        value = productName,
                        onValueChange = { productName = it },
                        label = { Text(stringResource(R.string.add_dev_name_label)) },
                        placeholder = { Text(stringResource(R.string.add_dev_name_placeholder)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                AddVariantSectionHeader(
                    title = stringResource(R.string.add_dev_region_code_title),
                    icon = Icons.Rounded.Public
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RegionData.regions.forEach { region ->
                        AddVariantChoicePill(
                            text = region.displayName,
                            selected = selectedRegion?.displayName == region.displayName,
                            onClick = {
                                selectedRegion = region
                                if (servers.contains(region.serverCode)) {
                                    selectedServer = region.serverCode
                                }
                            }
                        )
                    }
                }

                AddVariantSectionHeader(
                    title = stringResource(R.string.add_dev_branch_prefix_title),
                    icon = Icons.Rounded.Info
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    versionLetters.forEach { letter ->
                        AddVariantChoicePill(
                            text = letter,
                            selected = selectedLetter == letter,
                            onClick = { selectedLetter = letter }
                        )
                    }
                }

                AddVariantSectionHeader(
                    title = stringResource(R.string.add_dev_server_endpoint_title),
                    icon = Icons.Rounded.Dns
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    servers.forEach { server ->
                        AddVariantChoicePill(
                            text = server,
                            selected = selectedServer == server,
                            onClick = { selectedServer = server }
                        )
                    }
                }

                AddVariantSectionHeader(
                    title = stringResource(R.string.add_dev_query_mode_title),
                    icon = Icons.Rounded.Sync
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    reqModes.forEach { mode ->
                        AddVariantChoicePill(
                            text = mode,
                            selected = selectedReqMode == mode,
                            onClick = { selectedReqMode = mode }
                        )
                    }
                }

                AddVariantSectionHeader(
                    title = stringResource(R.string.add_dev_gray_flag_title),
                    icon = Icons.Rounded.Flag
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    grayValues.forEach { valGray ->
                        AddVariantChoicePill(
                            text = valGray.toString(),
                            selected = selectedGray == valGray,
                            onClick = { selectedGray = valGray }
                        )
                    }
                }
            }
        },
        confirmButton = {
            ApplyDialogBlurEffect()
            TextButton(
                onClick = {
                    if (selectedRegion != null) {
                        onAddVariant(
                            productModel.trim(),
                            productName.trim(),
                            selectedRegion!!,
                            selectedLetter,
                            selectedReqMode,
                            selectedGray,
                            selectedServer
                        )
                    }
                }
            ) {
                Text(stringResource(R.string.add_dev_add_variant_btn), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun AddVariantSectionHeader(title: String, icon: ImageVector, modifier: Modifier = Modifier) {
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
private fun AddVariantChoicePill(
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