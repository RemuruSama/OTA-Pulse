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

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.abhinav.otapulse.R
import com.abhinav.otapulse.core.common.PermissionHelper
import com.abhinav.otapulse.core.common.openExternalBrowser
import com.abhinav.otapulse.core.common.setHapticClickListener
import com.abhinav.otapulse.core.notifications.DownloadNotificationHelper
import com.abhinav.otapulse.core.preferences.AppSettingsPreferences
import com.abhinav.otapulse.core.preferences.ThemePreferences
import com.abhinav.otapulse.core.ui.applyBackgroundBlur
import com.abhinav.otapulse.feature.browser.InAppBrowserActivity
import com.abhinav.otapulse.feature.downloads.domain.DownloadRepository
import com.abhinav.otapulse.feature.settings.AppUpdateRepository
import com.abhinav.otapulse.navigation.Screen
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var downloadRepository: DownloadRepository

    @Inject
    lateinit var downloadNotificationHelper: DownloadNotificationHelper

    @Inject
    lateinit var permissionHelper: PermissionHelper

    @Inject
    lateinit var appUpdateRepository: AppUpdateRepository

    @Inject
    lateinit var okHttpClient: OkHttpClient

    @Inject
    lateinit var themePreferences: ThemePreferences

    @Inject
    lateinit var appSettingsPreferences: AppSettingsPreferences

    private val navigationEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)

    companion object {
        private const val TAG = "MainActivity"
        const val ACTION_OPEN_DOWNLOADS = "com.abhinav.otapulse.ACTION_OPEN_DOWNLOADS"
        private const val OTA_PULSE_REPO_URL = "https://github.com/RemuruSama/OTA-Pulse"
        private const val DONATION_URL = "https://paypal.me/Abhinavftp?country.x=IN&locale.x=en_GB"
    }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (!isGranted) {
                Toast.makeText(this, getString(R.string.notification_permission_denied), Toast.LENGTH_LONG).show()
            }
            checkBatteryOptimization()
        }

    private val manageStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (android.os.Environment.isExternalStorageManager()) {
                Toast.makeText(this, getString(R.string.storage_permission_granted), Toast.LENGTH_SHORT).show()
                checkNotificationPermission()
            } else {
                Toast.makeText(this, getString(R.string.storage_permission_required), Toast.LENGTH_LONG).show()
            }
        }
    }

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        val themeSettings = themePreferences.getThemeSettings()
        AppCompatDelegate.setDefaultNightMode(themeSettings.nightMode)
        if (themeSettings.amoledDark && isNightModeActive()) {
            theme.applyStyle(R.style.ThemeOverlay_OTAPulse_Amoled, true)
        }

        setContent {
            OtaPulseApp(
                themePreferences = themePreferences,
                appSettingsPreferences = appSettingsPreferences,
                downloadRepository = downloadRepository,
                appUpdateRepository = appUpdateRepository,
                navigationEvent = navigationEvent
            )
        }

        handleFirstLaunchPermissions()
        observeAppUpdates()
        handleIntent(intent)

        if (savedInstanceState == null) {
            checkForAppUpdates()
        }
    }

    private fun checkForAppUpdates() {
        if (!appSettingsPreferences.getAppSettings().autoUpdateCheck) return
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val currentVersion = packageInfo.versionName ?: "1.0.0"
            viewModel.checkForUpdate(currentVersion)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for app updates", e)
        }
    }

    private fun observeAppUpdates() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.appUpdateState.collectLatest { updateInfo ->
                    if (updateInfo != null && !isDestroyed && !isFinishing) {
                        navigationEvent.tryEmit(
                            Screen.AppUpdate.createRoute(
                                version = updateInfo.version,
                                url = updateInfo.downloadUrl,
                                changelog = updateInfo.changelog
                            )
                        )
                        downloadNotificationHelper.showAppUpdateNotification(updateInfo)
                        viewModel.clearUpdateState()
                    }
                }
            }
        }
    }



    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?): Boolean {
        when (intent?.action) {
            ACTION_OPEN_DOWNLOADS -> {
                navigationEvent.tryEmit(Screen.Downloads.route)
                return true
            }
            "com.abhinav.otapulse.ACTION_SHOW_UPDATE_DIALOG" -> {
                val version = intent.getStringExtra("update_version") ?: return false
                val url = intent.getStringExtra("update_url") ?: return false
                val changelog = intent.getStringExtra("update_changelog") ?: return false
                navigationEvent.tryEmit(Screen.AppUpdate.createRoute(version, url, changelog))
                return true
            }
            "com.abhinav.otapulse.ACTION_SHOW_UPDATER" -> {
                navigationEvent.tryEmit(Screen.AppUpdate.createRoute())
                return true
            }
        }
        return false
    }

    private fun handleFirstLaunchPermissions() {
        checkStoragePermission()
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!android.os.Environment.isExternalStorageManager()) {
                MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.permission_needed))
                    .setMessage(getString(R.string.broad_file_access_prompt))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.settings)) { _, _ ->
                        try {
                            val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                            intent.data = Uri.parse("package:$packageName")
                            manageStoragePermissionLauncher.launch(intent)
                        } catch (e: Exception) {
                            val intent = Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                            manageStoragePermissionLauncher.launch(intent)
                        }
                    }
                    .setNegativeButton(R.string.exit_action) { _, _ ->
                        finish()
                    }
                    .show().applyBackgroundBlur()
            } else {
                checkNotificationPermission()
            }
        } else {
            checkNotificationPermission()
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            checkBatteryOptimization()
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            checkBatteryOptimization()
            return
        }

        val canRequestInApp = !permissionHelper.wasNotificationPermissionRequested() ||
            shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.notification_permission_title)
            .setMessage(
                if (canRequestInApp) {
                    getString(R.string.notification_permission_message)
                } else {
                    getString(R.string.notification_permission_settings_message)
                }
            )
            .setPositiveButton(if (canRequestInApp) R.string.grant_action else R.string.settings) { _, _ ->
                if (canRequestInApp) {
                    permissionHelper.markNotificationPermissionRequested()
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    openAppNotificationSettings()
                }
            }
            .setNegativeButton(R.string.later_action) { dialog, _ -> 
                dialog.dismiss()
                checkBatteryOptimization()
            }
            .show().applyBackgroundBlur()
    }

    private fun checkBatteryOptimization() {
        val prefs = getSharedPreferences("app_setup_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("has_prompted_battery_optimization", false)) return

        if (!appSettingsPreferences.getAppSettings().autoSoftwareUpdateCheck) return

        val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.battery_optimization_dialog_title)
                .setMessage(R.string.battery_optimization_dialog_message)
                .setPositiveButton(R.string.allow) { _, _ ->
                    prefs.edit().putBoolean("has_prompted_battery_optimization", true).apply()
                    val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                }
                .setNegativeButton(R.string.later_action) { _, _ ->
                    prefs.edit().putBoolean("has_prompted_battery_optimization", true).apply()
                }
                .show().applyBackgroundBlur()
        }
    }

    private fun openAppNotificationSettings() {
        try {
            startActivity(
                Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = "package:$packageName".toUri()
                }
            )
        } catch (_: Exception) {
            Toast.makeText(this, getString(R.string.cannot_open_app_settings), Toast.LENGTH_SHORT).show()
        }
    }


    fun openInAppBrowser(url: String, title: String? = null) {
        if (url.isBlank()) {
            Toast.makeText(this, getString(R.string.could_not_open_link), Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(InAppBrowserActivity.createIntent(this, url, title))
    }

    fun navigateToLibraries() {
        navigationEvent.tryEmit(Screen.Libraries.route)
    }

    private fun isNightModeActive(): Boolean {
        val uiMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return uiMode == Configuration.UI_MODE_NIGHT_YES
    }
}
