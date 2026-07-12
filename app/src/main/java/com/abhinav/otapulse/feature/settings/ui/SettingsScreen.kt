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

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Gradient
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhinav.otapulse.R
import com.abhinav.otapulse.core.common.LocaleHelper
import com.abhinav.otapulse.core.common.performHapticFeedback
import com.abhinav.otapulse.core.preferences.AppSettings
import com.abhinav.otapulse.core.preferences.ThemeSettings
import com.abhinav.otapulse.core.ui.components.OtaCard
import com.abhinav.otapulse.core.ui.components.OtaOutlinedButton
import com.abhinav.otapulse.core.ui.components.OtaPrimaryButton
import com.abhinav.otapulse.core.ui.components.OtaSwitch
import com.abhinav.otapulse.core.ui.components.OtaTopAppBar
import com.abhinav.otapulse.core.ui.theme.ThemeMode
import com.abhinav.otapulse.feature.settings.SettingsViewModel
import androidx.compose.ui.tooling.preview.Preview
import com.abhinav.otapulse.core.ui.theme.OtaPulseTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    onNavigateToLibraries: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themeSettings by viewModel.themeSettings.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val context = LocalContext.current
    val pm = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
    val isIgnoringBattery = remember { pm.isIgnoringBatteryOptimizations(context.packageName) }
    
    val webViewVersion = remember {
        WebView.getCurrentWebViewPackage()?.versionName ?: context.getString(R.string.browser_webview_version_unavailable)
    }

    SettingsContent(
        themeSettings = themeSettings,
        appSettings = appSettings,
        onNavigateToLibraries = onNavigateToLibraries,
        onSetThemeMode = viewModel::setThemeMode,
        onSetNightMode = viewModel::setNightMode,
        onSetAmoledDark = viewModel::setAmoledDark,
        onSetDynamicColor = viewModel::setDynamicColor,
        onSetGradientBackground = viewModel::setGradientBackground,
        onSetAdvancedMode = viewModel::setAdvancedMode,
        onSetAutoUpdateCheck = viewModel::setAutoUpdateCheck,
        onSetAutoSoftwareUpdateCheck = viewModel::setAutoSoftwareUpdateCheck,
        onSetArbDetection = viewModel::setArbDetection,
        onSetBrowserDesktopMode = viewModel::setBrowserDesktopMode,
        onSetBrowserShowControls = viewModel::setBrowserShowControls,
        onSetCheckIntervalHours = viewModel::setCheckIntervalHours,
        onExportToFile = viewModel::exportToFile,
        onImportFromFile = viewModel::importFromFile,
        isIgnoringBattery = isIgnoringBattery,
        webViewVersion = webViewVersion,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    themeSettings: ThemeSettings,
    appSettings: AppSettings,
    onNavigateToLibraries: () -> Unit,
    modifier: Modifier = Modifier,
    onSetThemeMode: (ThemeMode) -> Unit = {},
    onSetNightMode: (Int) -> Unit = {},
    onSetAmoledDark: (Boolean) -> Unit = {},
    onSetDynamicColor: (Boolean) -> Unit = {},
    onSetGradientBackground: (Boolean) -> Unit = {},
    onSetAdvancedMode: (Boolean) -> Unit = {},
    onSetAutoUpdateCheck: (Boolean) -> Unit = {},
    onSetAutoSoftwareUpdateCheck: (Boolean) -> Unit = {},
    onSetArbDetection: (Boolean) -> Unit = {},
    onSetBrowserDesktopMode: (Boolean) -> Unit = {},
    onSetBrowserShowControls: (Boolean) -> Unit = {},
    onSetCheckIntervalHours: (Long) -> Unit = {},
    onExportToFile: (Uri) -> Unit = {},
    onImportFromFile: (Uri) -> Unit = {},
    isIgnoringBattery: Boolean = false,
    webViewVersion: String = ""
) {
    val context = LocalContext.current

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showIntervalDialog by remember { mutableStateOf(false) }
    var showDeveloperDialog by remember { mutableStateOf(false) }
    var showContributorsDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { onExportToFile(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { pendingImportUri = it }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        topBar = {
            OtaTopAppBar(
                title = stringResource(R.string.title_settings),
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = Color.Transparent,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppearanceSection(
                themeSettings = themeSettings,
                onThemeModeChanged = onSetThemeMode,
                onNightModeChanged = onSetNightMode,
                onAmoledChanged = onSetAmoledDark,
                onDynamicColorChanged = onSetDynamicColor,
                onGradientChanged = onSetGradientBackground
            )

            GeneralSection(
                appSettings = appSettings,
                isIgnoringBattery = isIgnoringBattery,
                onLanguageClick = { showLanguageDialog = true },
                onAdvancedModeChanged = onSetAdvancedMode,
                onAutoUpdateChanged = onSetAutoUpdateCheck,
                onAutoSoftwareUpdateChanged = onSetAutoSoftwareUpdateCheck,
                onIntervalClick = { showIntervalDialog = true },
                onArbDetectionChanged = onSetArbDetection
            )

            BrowserSection(
                appSettings = appSettings,
                webViewVersion = webViewVersion,
                onDesktopModeChanged = onSetBrowserDesktopMode,
                onShowControlsChanged = onSetBrowserShowControls
            )

            DataSection(
                onExportClick = {
                    val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    val dateString = dateFormat.format(Date())
                    exportLauncher.launch("otapulse_backup_$dateString.json")
                },
                onImportClick = {
                    importLauncher.launch(arrayOf("application/json"))
                }
            )

            AboutSection(
                onLibrariesClick = onNavigateToLibraries,
                onDeveloperClick = { showDeveloperDialog = true }
            )

            Spacer(modifier = Modifier.height(84.dp))
        }
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = { tag ->
                LocaleHelper.applyLocale(context, tag)
            }
        )
    }

    if (showIntervalDialog) {
        CheckIntervalDialog(
            currentHours = appSettings.checkIntervalHours,
            onDismiss = { showIntervalDialog = false },
            onIntervalSelected = { hours ->
                onSetCheckIntervalHours(hours)
            }
        )
    }

    if (showDeveloperDialog) {
        DeveloperDialog(onDismiss = { showDeveloperDialog = false })
    }

    if (showContributorsDialog) {
        ContributorsDialog(onDismiss = { showContributorsDialog = false })
    }

    pendingImportUri?.let { uri ->
        ImportConfirmationDialog(
            onDismiss = { pendingImportUri = null },
            onConfirm = {
                onImportFromFile(uri)
                pendingImportUri = null
            }
        )
    }
}

@Composable
private fun AppearanceSection(
    themeSettings: ThemeSettings,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onNightModeChanged: (Int) -> Unit,
    onAmoledChanged: (Boolean) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onGradientChanged: (Boolean) -> Unit
) {
    val isDark = themeSettings.nightMode == AppCompatDelegate.MODE_NIGHT_YES ||
        (themeSettings.nightMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM && androidx.compose.foundation.isSystemInDarkTheme())

    OtaCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Section Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Appearance & Theme Engine",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Visual engine, colors, and aesthetics",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Theme Engine Selector Header
            Text(
                text = "VISUAL ENGINE",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ThemeEngineCard(
                    title = "Material You",
                    subtitle = "Dynamic • Adaptive",
                    icon = Icons.Rounded.Palette,
                    tint = MaterialTheme.colorScheme.primary,
                    selected = themeSettings.themeMode == ThemeMode.MATERIAL_YOU,
                    modifier = Modifier.weight(1f),
                    onClick = { onThemeModeChanged(ThemeMode.MATERIAL_YOU) }
                )
                ThemeEngineCard(
                    title = "Holographic",
                    subtitle = "Glassmorphism • Neon",
                    icon = Icons.Rounded.AutoAwesome,
                    tint = Color(0xFF00E5FF),
                    selected = themeSettings.themeMode == ThemeMode.HOLOGRAPHIC,
                    modifier = Modifier.weight(1f),
                    onClick = { onThemeModeChanged(ThemeMode.HOLOGRAPHIC) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Color Scheme Mode
            Text(
                text = "COLOR SCHEME MODE",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SegmentedModeItem(
                        selected = themeSettings.nightMode == AppCompatDelegate.MODE_NIGHT_NO,
                        text = "Light",
                        icon = Icons.Rounded.LightMode,
                        modifier = Modifier.weight(1f),
                        enabled = themeSettings.themeMode != ThemeMode.HOLOGRAPHIC,
                        onClick = { onNightModeChanged(AppCompatDelegate.MODE_NIGHT_NO) }
                    )
                    SegmentedModeItem(
                        selected = themeSettings.nightMode == AppCompatDelegate.MODE_NIGHT_YES,
                        text = "Dark",
                        icon = Icons.Rounded.DarkMode,
                        modifier = Modifier.weight(1f),
                        onClick = { onNightModeChanged(AppCompatDelegate.MODE_NIGHT_YES) }
                    )
                    SegmentedModeItem(
                        selected = themeSettings.nightMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
                        text = "System",
                        icon = Icons.Rounded.SettingsBrightness,
                        modifier = Modifier.weight(1f),
                        enabled = themeSettings.themeMode != ThemeMode.HOLOGRAPHIC,
                        onClick = { onNightModeChanged(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Visual Enhancements
            Text(
                text = "VISUAL ENHANCEMENTS",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    EnhancedToggleRow(
                        title = stringResource(R.string.settings_amoled_title),
                        subtitle = stringResource(R.string.settings_amoled_desc),
                        icon = Icons.Rounded.DarkMode,
                        tint = MaterialTheme.colorScheme.primary,
                        checked = themeSettings.amoledDark,
                        onCheckedChange = onAmoledChanged,
                        enabled = isDark || themeSettings.themeMode == ThemeMode.HOLOGRAPHIC
                    )

                    if (themeSettings.themeMode == ThemeMode.MATERIAL_YOU && Build.VERSION.SDK_INT >= 31) {
                        EnhancedToggleRow(
                            title = "Dynamic Accent Colors",
                            subtitle = "Extract personalized color palette from wallpaper",
                            icon = Icons.Rounded.ColorLens,
                            tint = MaterialTheme.colorScheme.primary,
                            checked = themeSettings.dynamicColor,
                            onCheckedChange = onDynamicColorChanged
                        )
                    }

                    if (themeSettings.themeMode == ThemeMode.HOLOGRAPHIC) {
                        EnhancedToggleRow(
                            title = stringResource(R.string.settings_gradient_bg_title),
                            subtitle = stringResource(R.string.settings_gradient_bg_desc),
                            icon = Icons.Rounded.Gradient,
                            tint = Color(0xFF00E5FF),
                            checked = themeSettings.gradientBackground,
                            onCheckedChange = onGradientChanged
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeEngineCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor = if (selected) tint else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val bgColor = if (selected) tint.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = tint.copy(alpha = 0.2f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (selected) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = tint
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ACTIVE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.surface
                            )
                        }
                    }
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SegmentedModeItem(
    selected: Boolean,
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val bgColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = contentColor
            )
        }
    }
}

@Composable
private fun EnhancedToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .alpha(if (enabled) 1f else 0.45f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = tint.copy(alpha = 0.15f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        OtaSwitch(
            checked = checked,
            onCheckedChange = { onCheckedChange(it) },
            enabled = enabled
        )
    }
}

@Composable
private fun GeneralSection(
    appSettings: AppSettings,
    isIgnoringBattery: Boolean,
    onLanguageClick: () -> Unit,
    onAdvancedModeChanged: (Boolean) -> Unit,
    onAutoUpdateChanged: (Boolean) -> Unit,
    onAutoSoftwareUpdateChanged: (Boolean) -> Unit,
    onIntervalClick: () -> Unit,
    onArbDetectionChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val currentLocaleTag = LocaleHelper.getSelectedLocale(context)
    val langName = LocaleHelper.getDisplayName(context, currentLocaleTag)

    OtaCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "General",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            SettingsClickableRow(
                title = stringResource(R.string.settings_app_language),
                subtitle = langName,
                onClick = onLanguageClick
            )

            SettingsToggleRow(
                title = stringResource(R.string.settings_advanced_mode),
                subtitle = stringResource(R.string.settings_advanced_mode_desc),
                checked = appSettings.advancedMode,
                onCheckedChange = onAdvancedModeChanged
            )

            SettingsToggleRow(
                title = stringResource(R.string.settings_auto_check_updates),
                subtitle = stringResource(R.string.settings_auto_check_updates_desc),
                checked = appSettings.autoUpdateCheck,
                onCheckedChange = onAutoUpdateChanged
            )

            SettingsToggleRow(
                title = stringResource(R.string.settings_auto_software_update),
                subtitle = stringResource(R.string.settings_auto_software_update_desc),
                checked = appSettings.autoSoftwareUpdateCheck,
                onCheckedChange = onAutoSoftwareUpdateChanged
            )

            if (appSettings.autoSoftwareUpdateCheck) {
                val hours = appSettings.checkIntervalHours
                val intervalLabel = when (hours) {
                    1L -> stringResource(R.string.settings_check_interval_1h)
                    3L -> stringResource(R.string.settings_check_interval_3h)
                    6L -> stringResource(R.string.settings_check_interval_6h)
                    12L -> stringResource(R.string.settings_check_interval_12h)
                    24L -> stringResource(R.string.settings_check_interval_24h)
                    else -> "${hours}h"
                }
                SettingsClickableRow(
                    title = stringResource(R.string.settings_check_interval_title),
                    subtitle = intervalLabel,
                    onClick = onIntervalClick
                )

                SettingsClickableRow(
                    title = stringResource(R.string.settings_battery_optimization),
                    subtitle = if (isIgnoringBattery) stringResource(R.string.settings_battery_optimization_ignored)
                               else stringResource(R.string.settings_battery_optimization_desc),
                    onClick = {
                        if (!isIgnoringBattery) {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                            intent.data = Uri.parse("package:${context.packageName}")
                            context.startActivity(intent)
                        } else {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(intent)
                        }
                    }
                )
            }

            SettingsToggleRow(
                title = stringResource(R.string.settings_arb_detection),
                subtitle = stringResource(R.string.settings_arb_detection_desc),
                checked = appSettings.arbDetection,
                onCheckedChange = onArbDetectionChanged
            )
        }
    }
}

@Composable
private fun BrowserSection(
    appSettings: AppSettings,
    webViewVersion: String,
    onDesktopModeChanged: (Boolean) -> Unit,
    onShowControlsChanged: (Boolean) -> Unit
) {
    OtaCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.browser_settings_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            SettingsToggleRow(
                title = stringResource(R.string.browser_desktop_mode_title),
                subtitle = stringResource(R.string.browser_desktop_mode_summary),
                checked = appSettings.browserDesktopMode,
                onCheckedChange = onDesktopModeChanged
            )

            SettingsToggleRow(
                title = stringResource(R.string.browser_controls_title),
                subtitle = stringResource(R.string.browser_controls_summary),
                checked = appSettings.browserShowControls,
                onCheckedChange = onShowControlsChanged
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.browser_webview_version_title),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = webViewVersion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DataSection(
    onExportClick: () -> Unit,
    onImportClick: () -> Unit
) {
    OtaCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Backup & Restore",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            SettingsClickableRow(
                title = stringResource(R.string.settings_export_backup),
                subtitle = stringResource(R.string.settings_export_backup_desc),
                onClick = onExportClick
            )

            SettingsClickableRow(
                title = stringResource(R.string.settings_import_backup),
                subtitle = stringResource(R.string.settings_import_backup_desc),
                onClick = onImportClick
            )
        }
    }
}

@Composable
private fun AboutSection(
    onLibrariesClick: () -> Unit,
    onDeveloperClick: () -> Unit
) {
    OtaCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "About & Community",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            SettingsClickableRow(
                title = stringResource(R.string.libraries_title),
                onClick = onLibrariesClick
            )

            SettingsClickableRow(
                title = stringResource(R.string.developer_title),
                subtitle = "Abhinav Verma",
                onClick = onDeveloperClick
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) {
                onCheckedChange(!checked)
            }
            .padding(vertical = 12.dp)
            .alpha(if (enabled) 1f else 0.5f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        OtaSwitch(
            checked = checked,
            onCheckedChange = { onCheckedChange(it) },
            enabled = enabled
        )
    }
}

@Composable
private fun SettingsClickableRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp)
            .alpha(if (enabled) 1f else 0.5f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    OtaPulseTheme(themeMode = ThemeMode.MATERIAL_YOU) {
        SettingsContent(
            themeSettings = ThemeSettings(),
            appSettings = AppSettings(),
            onNavigateToLibraries = {},
            webViewVersion = "125.0.6422.165"
        )
    }
}
