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

package com.abhinav.otapulse.feature.settings.ui

import com.abhinav.otapulse.R
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.abhinav.otapulse.core.common.openExternalBrowser
import com.abhinav.otapulse.core.common.performHapticFeedback
import com.abhinav.otapulse.core.network.AppUpdateDownloader
import com.abhinav.otapulse.core.network.GitHubUpdater
import com.abhinav.otapulse.core.ui.components.OtaCard
import com.abhinav.otapulse.core.ui.components.OtaOutlinedButton
import com.abhinav.otapulse.core.ui.components.OtaPrimaryButton
import com.abhinav.otapulse.core.ui.components.OtaTopAppBar
import io.noties.markwon.Markwon
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.io.File
import java.net.URLDecoder
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUpdateScreen(
    versionArg: String = "",
    urlArg: String = "",
    changelogArg: String = "",
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val okHttpClient = remember { OkHttpClient() }

    // Decode arguments that were URL encoded in Screen.kt
    val decodedUrl = remember(urlArg) {
        try { URLDecoder.decode(urlArg, "UTF-8") } catch (e: Exception) { urlArg }
    }
    val decodedChangelog = remember(changelogArg) {
        try { URLDecoder.decode(changelogArg, "UTF-8") } catch (e: Exception) { changelogArg }
    }

    var version by remember { mutableStateOf(versionArg) }
    var url by remember { mutableStateOf(decodedUrl) }
    var changelog by remember { mutableStateOf(decodedChangelog) }

    var isChecking by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0) }
    var downloadedFile by remember { mutableStateOf<File?>(null) }
    var downloadError by remember { mutableStateOf<String?>(null) }

    val currentVersion = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    val isUpdateAvailable = url.isNotBlank() && version.isNotBlank() && !version.equals("latest", ignoreCase = true)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            OtaTopAppBar(
                title = stringResource(R.string.app_update_title),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = {
                        context.performHapticFeedback()
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.app_update_back_cd),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!isUpdateAvailable) {
                // Latest Version / Check State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.size(96.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(52.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "You're up to date!",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        val displayCurrentVersion = if (currentVersion.startsWith("v", ignoreCase = true)) currentVersion else "v$currentVersion"
                        Text(
                            text = "Current Version: $displayCurrentVersion",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    OtaPrimaryButton(
                        text = if (isChecking) "Checking GitHub..." else "Check for Updates Now",
                        icon = Icons.Rounded.SystemUpdate,
                        enabled = !isChecking,
                        onClick = {
                            context.performHapticFeedback()
                            isChecking = true
                            GitHubUpdater.checkForUpdate(currentVersion, okHttpClient) { info ->
                                isChecking = false
                                if (info != null) {
                                    version = info.version
                                    url = info.downloadUrl
                                    changelog = info.changelog
                                } else {
                                    Toast.makeText(context, context.getString(R.string.app_update_no_newer), Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                }
            } else {
                // Update Available State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Banner Card
                    OtaCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        imageVector = Icons.Rounded.SystemUpdate,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "New Release Available",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val displayVersion = if (version.startsWith("v", ignoreCase = true)) version else "v$version"
                                Text(
                                    text = displayVersion,
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Changelog Section
                    Text(
                        text = "RELEASE NOTES & CHANGELOG",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )

                    OtaCard(modifier = Modifier.fillMaxWidth()) {
                        val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    TextView(ctx).apply {
                                        setTextColor(textColor)
                                        textSize = 14f
                                    }
                                },
                                update = { textView ->
                                    textView.setTextColor(textColor)
                                    val markwon = Markwon.create(textView.context)
                                    markwon.setMarkdown(textView, changelog.ifBlank { "No detailed changelog provided." })
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Download/Install Progress State
                    if (isDownloading || downloadedFile != null || downloadError != null) {
                        OtaCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (isDownloading) {
                                    Text(
                                        text = "Downloading update package...",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    LinearProgressIndicator(
                                        progress = { downloadProgress / 100f },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    Text(
                                        text = "$downloadProgress%",
                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                } else if (downloadedFile != null) {
                                    Text(
                                        text = "Download Complete!",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Tap below to launch Android package installer.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OtaPrimaryButton(
                                        text = "Install APK Now",
                                        icon = Icons.Rounded.InstallMobile,
                                        onClick = {
                                            context.performHapticFeedback()
                                            installApk(context, downloadedFile!!)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else if (downloadError != null) {
                                    Text(
                                        text = "Download Failed: $downloadError",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    OtaOutlinedButton(
                                        text = "Download via Browser",
                                        onClick = {
                                            context.performHapticFeedback()
                                            try {
                                                context.openExternalBrowser(url)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, context.getString(R.string.app_update_err_browser), Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Actions (Hidden when downloading/done)
                    if (!isDownloading && downloadedFile == null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OtaOutlinedButton(
                                text = "Later",
                                onClick = {
                                    context.performHapticFeedback()
                                    onNavigateBack()
                                },
                                modifier = Modifier.weight(1f)
                            )

                            OtaPrimaryButton(
                                text = "Update Now",
                                icon = Icons.Rounded.Download,
                                onClick = {
                                    context.performHapticFeedback()
                                    isDownloading = true
                                    downloadError = null
                                    val fileName = "otapulse_update_$version.apk"
                                    val targetFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)

                                    scope.launch {
                                        AppUpdateDownloader.downloadApk(url, targetFile, okHttpClient)
                                            .collect { state ->
                                                when (state) {
                                                    is AppUpdateDownloader.DownloadState.Downloading -> {
                                                        downloadProgress = state.progress
                                                    }
                                                    is AppUpdateDownloader.DownloadState.Success -> {
                                                        isDownloading = false
                                                        downloadedFile = state.file
                                                        installApk(context, state.file)
                                                    }
                                                    is AppUpdateDownloader.DownloadState.Error -> {
                                                        isDownloading = false
                                                        downloadError = state.exception.message ?: "Unknown error"
                                                    }
                                                    else -> {}
                                                }
                                            }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

private fun installApk(context: Context, apkFile: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e("AppUpdateScreen", "Failed to start install intent", e)
        Toast.makeText(context, context.getString(R.string.app_update_err_install), Toast.LENGTH_SHORT).show()
    }
}
