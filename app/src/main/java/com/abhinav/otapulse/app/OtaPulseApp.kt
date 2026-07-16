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

package com.abhinav.otapulse.app

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.abhinav.otapulse.core.download.DownloadStatus
import com.abhinav.otapulse.feature.downloads.domain.DownloadRepository
import com.abhinav.otapulse.core.preferences.AppSettingsPreferences
import com.abhinav.otapulse.core.preferences.ThemePreferences
import com.abhinav.otapulse.core.ui.theme.OtaPulseTheme
import com.abhinav.otapulse.core.ui.theme.ThemeMode
import com.abhinav.otapulse.core.ui.theme.HoloPrimary
import com.abhinav.otapulse.core.ui.theme.HoloSecondary
import com.abhinav.otapulse.core.ui.theme.holographicEdgeBrush
import com.abhinav.otapulse.core.common.performHapticFeedback
import com.abhinav.otapulse.navigation.OtaPulseNavGraph
import com.abhinav.otapulse.navigation.Screen
import com.abhinav.otapulse.navigation.otaPulseBottomNavItems
import kotlinx.coroutines.flow.Flow

import androidx.compose.ui.platform.LocalContext
import com.abhinav.otapulse.feature.about.ui.WhatsNewHelper
import com.abhinav.otapulse.feature.about.ui.WhatsNewSheet
import com.abhinav.otapulse.feature.settings.AppUpdateRepository
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource

@Composable
fun OtaPulseApp(
    themePreferences: ThemePreferences,
    appSettingsPreferences: AppSettingsPreferences,
    downloadRepository: DownloadRepository,
    appUpdateRepository: AppUpdateRepository,
    navigationEvent: Flow<String>
) {
    val themeSettings by themePreferences.themeSettingsFlow.collectAsState(initial = themePreferences.getThemeSettings())
    val appSettings by appSettingsPreferences.appSettingsFlow.collectAsState(initial = appSettingsPreferences.getAppSettings())
    val downloads by downloadRepository.allDownloads.collectAsState(initial = emptyList())
    val isDownloading = remember(downloads) {
        downloads.any { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val context = LocalContext.current

    var whatsNewData by remember { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(navigationEvent) {
        navigationEvent.collect { route ->
            navController.navigate(route) {
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val currentVersion = packageInfo.versionName ?: return@LaunchedEffect
            if (WhatsNewHelper.shouldShow(context, currentVersion)) {
                val result = appUpdateRepository.fetchChangelog(currentVersion)
                result.onSuccess { changelog ->
                    if (!changelog.isNullOrBlank()) {
                        whatsNewData = Pair(currentVersion, changelog)
                    } else {
                        WhatsNewHelper.markShown(context, currentVersion)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    val visibleBottomNavItems = remember(appSettings.advancedMode) {
        otaPulseBottomNavItems.filter { it.screen != Screen.OtaTools || appSettings.advancedMode }
    }

    val topLevelRoutes = remember(visibleBottomNavItems) {
        visibleBottomNavItems.map { it.screen.route }.toSet()
    }
    val showBottomBar = currentRoute in topLevelRoutes || currentRoute == null

    OtaPulseTheme(
        themeMode = themeSettings.themeMode,
        darkTheme = when (themeSettings.nightMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> false
            AppCompatDelegate.MODE_NIGHT_YES -> true
            else -> isSystemInDarkTheme()
        },
        amoledDark = themeSettings.amoledDark,
        dynamicColor = themeSettings.dynamicColor,
        seedColor = Color(themeSettings.seedColor),
        paletteStyle = themeSettings.paletteStyle
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
                contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                floatingActionButton = {
                    val showFab = showBottomBar &&
                        currentDestination?.route != Screen.About.route &&
                        currentDestination?.route != Screen.Settings.route &&
                        currentDestination?.route != Screen.DeviceCatalog.route
                    AnimatedVisibility(
                        visible = showFab,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        val navBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                        FloatingActionButton(
                            onClick = { navController.navigate(Screen.Downloads.route) },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(bottom = navBarsBottom + 88.dp)
                        ) {
                            BadgedBox(
                                badge = {
                                    if (isDownloading) {
                                        Badge()
                                    }
                                }
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = "Downloads")
                            }
                        }
                    }
                }
            ) { innerPadding ->
                OtaPulseNavGraph(
                    navController = navController,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            // Overlay the floating bottom navigation bar directly on top of the page content
            // so the background and cards flow all the way to the bottom of the screen behind the pill
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                OtaPulseBottomBar(
                    navController = navController,
                    currentDestination = currentDestination,
                    isHolographic = themeSettings.themeMode == ThemeMode.HOLOGRAPHIC,
                    items = visibleBottomNavItems
                )
            }

            if (whatsNewData != null) {
                WhatsNewSheet(
                    isVisible = true,
                    onDismiss = { whatsNewData = null },
                    version = whatsNewData!!.first,
                    changelog = whatsNewData!!.second
                )
            }
        }
    }
}

@Composable
private fun OtaPulseBottomBar(
    navController: NavHostController,
    currentDestination: NavDestination?,
    isHolographic: Boolean,
    items: List<com.abhinav.otapulse.navigation.BottomNavItem>
) {
    val context = LocalContext.current
    val navBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = navBarsBottom + 16.dp, top = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Use clip+background instead of Surface to avoid any color bleeding
        // outside the shape (shadow glow, tonal overlay, border bleed)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(
                    color = if (isHolographic)
                        Color(0xFF0D0D1A).copy(alpha = 0.88f)
                    else
                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f)
                )
                // Only apply shadow on Material mode - dark theme shadow is neutral (black)
                .then(
                    if (!isHolographic) Modifier.then(Modifier) else Modifier
                )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
                    val pillColor = if (isHolographic) {
                        HoloPrimary.copy(alpha = 0.28f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                    val iconColor = if (isHolographic) {
                        if (selected) HoloSecondary else Color.White.copy(alpha = 0.50f)
                    } else {
                        if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(color = if (selected) pillColor else Color.Transparent)
                            .clickable {
                                if (!selected) {
                                    context.performHapticFeedback()
                                    navController.navigate(item.screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.iconRes != null) {
                            Icon(
                                painter = painterResource(id = item.iconRes),
                                contentDescription = item.label,
                                tint = iconColor,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.icon,
                                contentDescription = item.label,
                                tint = iconColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
