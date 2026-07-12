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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.ui.graphics.vector.ImageVector
import com.abhinav.otapulse.R
import java.net.URLEncoder

/**
 * Type-safe navigation routes for OTA Pulse.
 * Uses string-based routes with argument formatting as a lightweight, robust alternative
 * to requiring serialization plugins.
 */
sealed class Screen(val route: String) {
    // Bottom nav destinations (top-level)
    data object HomeUpdate : Screen("home_update")
    data object DeviceCatalog : Screen("device_catalog")
    data object OtaTools : Screen("ota_tools")
    data object About : Screen("about")
    data object Settings : Screen("settings")

    // Secondary/deep-link destinations
    data object Downloads : Screen("downloads")
    data object History : Screen("history")
    data object Libraries : Screen("libraries")
    data object Extraction : Screen("extraction")
    data object ManualQuery : Screen("manual_query")
    data object LinkResolver : Screen("link_resolver")
    data object ArbChecker : Screen("arb_checker")
    data object AddDevice : Screen("add_device")

    // Routes with arguments
    data object DeviceDetail : Screen("device_detail/{deviceId}") {
        const val ARG_DEVICE_ID = "deviceId"
        fun createRoute(deviceId: String): String = "device_detail/$deviceId"
    }

    data object AppUpdate : Screen("app_update?version={version}&url={url}&changelog={changelog}") {
        const val ARG_VERSION = "version"
        const val ARG_URL = "url"
        const val ARG_CHANGELOG = "changelog"

        fun createRoute(
            version: String = "",
            url: String = "",
            changelog: String = ""
        ): String {
            val encodedUrl = try { URLEncoder.encode(url, "UTF-8") } catch (e: Exception) { url }
            val encodedChangelog = try { URLEncoder.encode(changelog, "UTF-8") } catch (e: Exception) { changelog }
            return "app_update?version=$version&url=$encodedUrl&changelog=$encodedChangelog"
        }
    }

    companion object {
        /** Top-level destinations shown in bottom navigation. */
        val bottomNavItems = listOf(HomeUpdate, DeviceCatalog, OtaTools, About, Settings)
    }
}

/**
 * Represents an item in the bottom navigation bar.
 */
data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val iconRes: Int? = null
)

/**
 * The configuration of bottom navigation items with icons and labels.
 */
val otaPulseBottomNavItems = listOf(
    BottomNavItem(
        screen = Screen.HomeUpdate,
        label = "Update",
        icon = Icons.Outlined.SystemUpdate,
        selectedIcon = Icons.Filled.SystemUpdate
    ),
    BottomNavItem(
        screen = Screen.DeviceCatalog,
        label = "Devices",
        icon = Icons.Outlined.Devices,
        selectedIcon = Icons.Filled.Devices
    ),
    BottomNavItem(
        screen = Screen.OtaTools,
        label = "Tools",
        icon = Icons.Outlined.Build,
        selectedIcon = Icons.Filled.Build,
        iconRes = R.drawable.ic_tools
    ),
    BottomNavItem(
        screen = Screen.About,
        label = "About",
        icon = Icons.Outlined.Info,
        selectedIcon = Icons.Filled.Info
    ),
    BottomNavItem(
        screen = Screen.Settings,
        label = "Settings",
        icon = Icons.Outlined.Settings,
        selectedIcon = Icons.Filled.Settings
    )
)
