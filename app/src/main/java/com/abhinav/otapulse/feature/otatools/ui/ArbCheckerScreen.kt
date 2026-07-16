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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhinav.otapulse.R
import com.abhinav.otapulse.core.common.performHapticFeedback
import com.abhinav.otapulse.core.ui.components.OtaCard
import com.abhinav.otapulse.core.ui.components.OtaOutlinedButton
import com.abhinav.otapulse.core.ui.components.OtaPrimaryButton
import com.abhinav.otapulse.core.ui.components.OtaTextField
import com.abhinav.otapulse.core.ui.components.OtaTopAppBar
import com.abhinav.otapulse.core.ui.theme.OtaPulseTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArbCheckerScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OtaToolsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val context = LocalContext.current

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

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            OtaTopAppBar(
                title = stringResource(R.string.tools_arb_checker_title),
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
                                    imageVector = Icons.Rounded.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "ARB_CHECKER.sh",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.tools_arb_checker_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Section 1: Check URL
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
                                text = stringResource(R.string.arb_checker_url_title),
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

                        val isUrlChecking = uiState.isCheckingArb && uiState.checkingArbSource == inputUrl.trim()
                        OtaPrimaryButton(
                            text = if (isUrlChecking) "Checking Rollback Index..." else stringResource(R.string.arb_checker_check_url_btn),
                            icon = if (isUrlChecking) null else Icons.Rounded.Search,
                            isLoading = isUrlChecking,
                            onClick = {
                                context.performHapticFeedback()
                                val trimmed = inputUrl.trim()
                                if (trimmed.isBlank()) {
                                    Toast.makeText(context, context.getString(R.string.toast_paste_ota_url_first), Toast.LENGTH_SHORT).show()
                                    return@OtaPrimaryButton
                                }
                                val guessName = guessVersionNameFromUrl(trimmed)
                                viewModel.checkArb(
                                    source = trimmed,
                                    sourceLabel = "Full OTA URL",
                                    displayName = guessName
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Section 2: Check Local ZIP
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
                                text = stringResource(R.string.arb_checker_local_zip_title),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        if (selectedLocalUri == null) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        context.performHapticFeedback()
                                        pickLocalZipLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                                    }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp, horizontal = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.UploadFile,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = "Select Local OTA ZIP",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Tap to browse .zip packages on your device",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        context.performHapticFeedback()
                                        pickLocalZipLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = selectedLocalName.ifBlank { "Selected ZIP File" },
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Tap to pick a different file",
                                            style = MaterialTheme.typography.labelSmall,
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
                        }

                        val isLocalChecking = uiState.isCheckingArb && uiState.checkingArbSource == selectedLocalUri?.toString()
                        OtaPrimaryButton(
                            text = if (isLocalChecking) "Checking Rollback Index..." else stringResource(R.string.arb_checker_check_local_btn),
                            icon = if (isLocalChecking) null else Icons.Rounded.Search,
                            isLoading = isLocalChecking,
                            onClick = {
                                context.performHapticFeedback()
                                val uri = selectedLocalUri
                                if (uri == null) {
                                    Toast.makeText(context, context.getString(R.string.toast_choose_local_zip_first), Toast.LENGTH_SHORT).show()
                                    return@OtaPrimaryButton
                                }
                                viewModel.checkArb(
                                    source = uri.toString(),
                                    sourceLabel = selectedLocalName.ifBlank { "Local OTA ZIP" },
                                    displayName = selectedLocalName.ifBlank { "Local OTA" }
                                )
                            },
                            enabled = selectedLocalUri != null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            uiState.arbCheckResult?.let { result ->
                item {
                    val isSafe = result.arbInfo.isSafe
                    val statusColor = if (isSafe) OtaPulseTheme.extendedColors.arbSafe else MaterialTheme.colorScheme.error

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
                                        .background(statusColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Security,
                                        contentDescription = null,
                                        tint = statusColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Anti-Rollback Verification Result",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(statusColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isSafe) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                                        contentDescription = null,
                                        tint = statusColor,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isSafe) "SAFE TO INSTALL" else "PROTECTED / ANTI-ROLLBACK ACTIVE",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = statusColor
                                    )
                                    val subtitleText = if (result.displayName == result.sourceLabel) {
                                        result.displayName
                                    } else {
                                        "${result.displayName} (${result.sourceLabel})"
                                    }
                                    Text(
                                        text = subtitleText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("ARB Index: ${result.arbInfo.arbIndex}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text("Firmware Version: ${result.arbInfo.major}.${result.arbInfo.minor}", style = MaterialTheme.typography.bodySmall)
                                    if (!result.arbInfo.deviceName.isNullOrBlank()) {
                                        Text("Target Device: ${result.arbInfo.deviceName}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (!result.arbInfo.status.isNullOrBlank()) {
                                        Text("Status Details: ${result.arbInfo.status}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }

                            OtaOutlinedButton(
                                text = "Clear Result",
                                onClick = {
                                    context.performHapticFeedback()
                                    viewModel.clearArbCheckResult()
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
