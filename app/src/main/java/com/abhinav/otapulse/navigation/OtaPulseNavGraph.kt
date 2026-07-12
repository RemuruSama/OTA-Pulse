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

package com.abhinav.otapulse.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.abhinav.otapulse.core.common.toPredefined

/**
 * The main navigation graph for OTA Pulse.
 * Links all top-level bottom navigation destinations and secondary deep-link screens.
 */
@Composable
fun OtaPulseNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.HomeUpdate.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { NavigationAnimations.defaultEnterTransition() },
        exitTransition = { NavigationAnimations.defaultExitTransition() },
        popEnterTransition = { NavigationAnimations.defaultPopEnterTransition() },
        popExitTransition = { NavigationAnimations.defaultPopExitTransition() }
    ) {
        // ── Top Level Bottom Nav Destinations ──────────────────────────────
        composable(
            route = Screen.HomeUpdate.route,
            enterTransition = { NavigationAnimations.bottomNavEnterTransition() },
            exitTransition = { NavigationAnimations.bottomNavExitTransition() }
        ) {
            com.abhinav.otapulse.feature.updates.ui.HomeUpdateScreen(
                onNavigateToHistory = { navController.navigate(Screen.History.route) }
            )
        }

        composable(
            route = Screen.DeviceCatalog.route,
            enterTransition = { NavigationAnimations.bottomNavEnterTransition() },
            exitTransition = { NavigationAnimations.bottomNavExitTransition() }
        ) {
            com.abhinav.otapulse.feature.devicecatalog.ui.DeviceCatalogScreen(
                onNavigateToAddDevice = { device ->
                    if (device != null) {
                        navController.currentBackStackEntry?.savedStateHandle?.set("edit_device", device.toPredefined())
                    } else {
                        navController.currentBackStackEntry?.savedStateHandle?.remove<Any>("edit_device")
                    }
                    navController.navigate(Screen.AddDevice.route)
                },
                onNavigateToHistory = { navController.navigate(Screen.History.route) }
            )
        }

        composable(
            route = Screen.OtaTools.route,
            enterTransition = { NavigationAnimations.bottomNavEnterTransition() },
            exitTransition = { NavigationAnimations.bottomNavExitTransition() }
        ) {
            com.abhinav.otapulse.feature.otatools.ui.OtaToolsScreen(
                onNavigateToManualQuery = { navController.navigate(Screen.ManualQuery.route) },
                onNavigateToExtraction = { navController.navigate(Screen.Extraction.route) },
                onNavigateToLinkResolver = { navController.navigate(Screen.LinkResolver.route) },
                onNavigateToArbChecker = { navController.navigate(Screen.ArbChecker.route) },
                onNavigateToDownloads = { navController.navigate(Screen.Downloads.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) }
            )
        }

        composable(
            route = Screen.About.route,
            enterTransition = { NavigationAnimations.bottomNavEnterTransition() },
            exitTransition = { NavigationAnimations.bottomNavExitTransition() }
        ) {
            com.abhinav.otapulse.feature.about.ui.AboutScreen(
                onNavigateToAppUpdate = { info ->
                    if (info != null) {
                        navController.navigate(
                            Screen.AppUpdate.createRoute(
                                version = info.version,
                                url = info.downloadUrl,
                                changelog = info.changelog
                            )
                        )
                    } else {
                        navController.navigate(Screen.AppUpdate.createRoute())
                    }
                }
            )
        }

        composable(
            route = Screen.Settings.route,
            enterTransition = { NavigationAnimations.bottomNavEnterTransition() },
            exitTransition = { NavigationAnimations.bottomNavExitTransition() }
        ) {
            com.abhinav.otapulse.feature.settings.ui.SettingsScreen(
                onNavigateToLibraries = { navController.navigate(Screen.Libraries.route) }
            )
        }

        // ── Secondary / Tool Destinations ──────────────────────────────────
        composable(route = Screen.Downloads.route) {
            com.abhinav.otapulse.feature.downloads.ui.DownloadsScreen()
        }

        composable(route = Screen.History.route) {
            com.abhinav.otapulse.feature.history.ui.HistoryScreen()
        }

        composable(route = Screen.Libraries.route) {
            com.abhinav.otapulse.feature.settings.libraries.ui.LibrariesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.Extraction.route) {
            com.abhinav.otapulse.feature.otatools.ui.PartitionExtractionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.ManualQuery.route) {
            com.abhinav.otapulse.feature.otatools.ui.ManualQueryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.LinkResolver.route) {
            com.abhinav.otapulse.feature.otatools.ui.LinkResolverScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.ArbChecker.route) {
            com.abhinav.otapulse.feature.otatools.ui.ArbCheckerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.AddDevice.route) {
            val deviceToEdit = navController.previousBackStackEntry?.savedStateHandle?.get<com.abhinav.otapulse.catalog.model.PredefinedDevice>("edit_device")
            com.abhinav.otapulse.feature.devicecatalog.ui.AddDeviceScreen(
                onNavigateBack = { navController.popBackStack() },
                deviceToEdit = deviceToEdit
            )
        }

        // ── Parameterized Destinations ─────────────────────────────────────
        composable(
            route = Screen.DeviceDetail.route,
            arguments = listOf(
                navArgument(Screen.DeviceDetail.ARG_DEVICE_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString(Screen.DeviceDetail.ARG_DEVICE_ID) ?: ""
            PlaceholderScreen("Device Detail: $deviceId")
        }

        composable(
            route = Screen.AppUpdate.route,
            arguments = listOf(
                navArgument(Screen.AppUpdate.ARG_VERSION) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(Screen.AppUpdate.ARG_URL) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(Screen.AppUpdate.ARG_CHANGELOG) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val version = backStackEntry.arguments?.getString(Screen.AppUpdate.ARG_VERSION) ?: ""
            val url = backStackEntry.arguments?.getString(Screen.AppUpdate.ARG_URL) ?: ""
            val changelog = backStackEntry.arguments?.getString(Screen.AppUpdate.ARG_CHANGELOG) ?: ""
            com.abhinav.otapulse.feature.settings.ui.AppUpdateScreen(
                versionArg = version,
                urlArg = url,
                changelogArg = changelog,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
