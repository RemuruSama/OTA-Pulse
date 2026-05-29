package com.abhinav.otapulse.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.abhinav.otapulse.R
import com.abhinav.otapulse.databinding.ActivityMainBinding
import com.abhinav.otapulse.databinding.DialogSupportDeveloperBinding
import com.abhinav.otapulse.feature.downloads.domain.DownloadRepository
import com.abhinav.otapulse.feature.about.AboutFragment
import com.abhinav.otapulse.feature.otatools.ui.OtaToolsFragment
import com.abhinav.otapulse.feature.devices.ui.DevicesFragment
import com.abhinav.otapulse.feature.downloads.ui.DownloadsFragment
import com.abhinav.otapulse.feature.settings.SettingsFragment
import com.abhinav.otapulse.feature.settings.libraries.LibrariesFragment
import com.abhinav.otapulse.feature.updates.ui.HomeUpdateFragment
import com.abhinav.otapulse.core.common.PermissionHelper
import com.abhinav.otapulse.core.notifications.DownloadNotificationHelper
import com.abhinav.otapulse.feature.browser.InAppBrowserActivity
import com.abhinav.otapulse.core.common.openExternalBrowser
import io.noties.markwon.Markwon

import com.abhinav.otapulse.core.common.setHapticClickListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.abhinav.otapulse.core.download.DownloadStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject
    lateinit var downloadRepository: DownloadRepository

    @Inject
    lateinit var downloadNotificationHelper: DownloadNotificationHelper

    @Inject
    lateinit var permissionHelper: PermissionHelper

    private lateinit var binding: ActivityMainBinding
    private var lastSelectedItemId = 0
    private var isDownloading: Boolean = false
    private val DOWNLOADS_SCREEN_ID = -1
    private lateinit var appSettingsPrefs: SharedPreferences

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
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        appSettingsPrefs = getSharedPreferences(SettingsFragment.APP_SETTINGS_PREFS, Context.MODE_PRIVATE)
        appSettingsPrefs.registerOnSharedPreferenceChangeListener(this)

        val themePrefs = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val nightMode = themePrefs.getInt("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(nightMode)

        // Apply AMOLED overlay if enabled and currently in dark mode
        val isAmoled = themePrefs.getBoolean(SettingsFragment.PREF_AMOLED_MODE, false)
        if (isAmoled && isNightModeActive()) {
            theme.applyStyle(R.style.ThemeOverlay_OTAPulse_Amoled, true)
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupEdgeToEdge()
        setupNavigation()
        updateOtaToolsTabVisibility()
        observeDownloads()
        handleFirstLaunchPermissions()

        // AUTO UPDATE CHECK
        checkForAppUpdates()
        observeAppUpdates()

        val handledIntent = handleIntent(intent)

        if (savedInstanceState == null) {
            if (!handledIntent) {
                navigateToFragment(R.id.navigation_update, false)
            }
        } else {
            supportFragmentManager.findFragmentById(R.id.fragment_container)?.let {
                updateToolbarForFragment(it)
            }
        }

        supportFragmentManager.addOnBackStackChangedListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            currentFragment?.let {
                updateToolbarForFragment(it)
                updateBottomNavSelection()
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                } else {
                    finish()
                }
            }
        })

        if (savedInstanceState == null) {
            binding.root.post {
                if (!isDestroyed && !isFinishing) {
                    showSupportDeveloperDialog()
                }
            }
        }
    }

    private fun checkForAppUpdates() {
        val isAutoUpdateEnabled = appSettingsPrefs.getBoolean(SettingsFragment.PREF_AUTO_UPDATE_CHECK, true)
        if (!isAutoUpdateEnabled) return

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
            viewModel.appUpdateState.collectLatest { updateInfo ->
                if (updateInfo != null && !isDestroyed && !isFinishing) {
                    showUpdateDialog(updateInfo)
                    downloadNotificationHelper.showAppUpdateNotification(updateInfo)
                    viewModel.clearUpdateState()
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
                navigateToFragment(DOWNLOADS_SCREEN_ID, false)
                return true
            }
            "com.abhinav.otapulse.ACTION_SHOW_UPDATE_DIALOG" -> {
                val version = intent.getStringExtra("update_version") ?: return false
                val url = intent.getStringExtra("update_url") ?: return false
                val changelog = intent.getStringExtra("update_changelog") ?: return false
                val info = com.abhinav.otapulse.core.model.AppUpdateInfo(version, url, changelog)
                showUpdateDialog(info)
                return true
            }
        }
        return false
    }

    private fun showUpdateDialog(info: com.abhinav.otapulse.core.model.AppUpdateInfo) {
        val markwon = Markwon.create(this)
        val fullMessage = info.changelog
        val markdownChangelog = markwon.toMarkdown(fullMessage)

        @Suppress("DEPRECATION") // Suppress for older API compat
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.update_available_title, info.version))
            .setMessage(markdownChangelog)
            .setPositiveButton(R.string.download_action) { _, _ ->
                try {
                    openExternalBrowser(info.downloadUrl)
                } catch (e: Exception) {
                    Toast.makeText(this, getString(R.string.could_not_open_download_link), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.later_action, null)
            .show()
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
                    .show()
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
            .show()
    }

    private fun checkBatteryOptimization() {
        val prefs = getSharedPreferences("app_setup_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("has_prompted_battery_optimization", false)) return

        val isAutoUpdateEnabled = appSettingsPrefs.getBoolean(SettingsFragment.PREF_AUTO_SOFTWARE_UPDATE_CHECK, true)
        if (!isAutoUpdateEnabled) return

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
                .show()
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

    private fun showSupportDeveloperDialog() {
        val prefs = getSharedPreferences("support_dialog_prefs", Context.MODE_PRIVATE)
        val lastShown = prefs.getLong("last_shown_time", 0L)
        val oneDayMs = 24 * 60 * 60 * 1000L
        if (System.currentTimeMillis() - lastShown < oneDayMs) return

        val dialogBinding = DialogSupportDeveloperBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()
        dialog.setCanceledOnTouchOutside(false)

        val dismiss = {
            prefs.edit().putLong("last_shown_time", System.currentTimeMillis()).apply()
            dialog.dismiss()
        }

        dialogBinding.btnClose.setHapticClickListener { dismiss() }
        dialogBinding.btnMaybeLater.setHapticClickListener { dismiss() }
        dialogBinding.btnStarGithub.setHapticClickListener {
            openExternalBrowser(OTA_PULSE_REPO_URL)
            dismiss()
        }
        dialogBinding.btnDonate.setHapticClickListener {
            openExternalBrowser(DONATION_URL)
            dismiss()
        }

        dialog.show()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == SettingsFragment.PREF_ADVANCED_MODE_ENABLED) {
            updateOtaToolsTabVisibility()
        }
    }

    private fun updateOtaToolsTabVisibility() {
        val isAdvancedModeEnabled = appSettingsPrefs.getBoolean(SettingsFragment.PREF_ADVANCED_MODE_ENABLED, true)
        val otaToolsMenuItem = binding.bottomNavigation.menu.findItem(R.id.navigation_ota_tools)
        otaToolsMenuItem?.isVisible = isAdvancedModeEnabled

        if (!isAdvancedModeEnabled && lastSelectedItemId == R.id.navigation_ota_tools) {
            if (supportFragmentManager.findFragmentById(R.id.fragment_container) is OtaToolsFragment) {
                navigateToFragment(R.id.navigation_update, false)
            }
        }
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainContainer) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply top inset to the container so content sits below the status bar
            view.updatePadding(left = insets.left, top = insets.top, right = insets.right, bottom = 0)
            // Apply bottom inset as padding on the bottom nav so its background
            // extends seamlessly into the gesture navigation bar area
            binding.bottomNavigation.updatePadding(bottom = insets.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun setupNavigation() {
        binding.downloadsFab.setHapticClickListener {
            navigateToFragment(DOWNLOADS_SCREEN_ID)
        }
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (item.itemId == lastSelectedItemId) return@setOnItemSelectedListener false
            if (!item.isVisible) return@setOnItemSelectedListener false
            navigateToFragment(item.itemId)
            true
        }
    }

    private fun getTabIndex(itemId: Int): Int {
        return when (itemId) {
            R.id.navigation_update -> 0
            R.id.navigation_devices -> 1
            R.id.navigation_ota_tools -> 2
            R.id.navigation_about -> 3
            R.id.navigation_settings -> 4
            else -> -1
        }
    }

    private fun isMainTab(itemId: Int): Boolean {
        return itemId == R.id.navigation_update ||
               itemId == R.id.navigation_devices ||
               itemId == R.id.navigation_ota_tools ||
               itemId == R.id.navigation_about ||
               itemId == R.id.navigation_settings
    }

    private fun navigateToFragment(itemId: Int, addToBackStack: Boolean = true) {
        val selectedFragment: Fragment = when (itemId) {
            DOWNLOADS_SCREEN_ID -> DownloadsFragment()
            R.id.navigation_update -> HomeUpdateFragment()
            R.id.navigation_devices -> DevicesFragment()
            R.id.navigation_ota_tools -> {
                if (!appSettingsPrefs.getBoolean(SettingsFragment.PREF_ADVANCED_MODE_ENABLED, true)) {
                    HomeUpdateFragment()
                } else {
                    OtaToolsFragment()
                }
            }
            R.id.navigation_about -> AboutFragment()
            R.id.navigation_settings -> SettingsFragment()
            R.id.navigation_libraries -> LibrariesFragment()
            else -> HomeUpdateFragment()
        }

        // Pop any existing back stack entry for tab switches so it never grows unboundedly
        if (addToBackStack && isMainTab(itemId) && supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        }

        val currentIndex = getTabIndex(lastSelectedItemId)
        val newIndex = getTabIndex(itemId)

        supportFragmentManager.beginTransaction().apply {
            setReorderingAllowed(true) // plays enter+exit animations concurrently — fixes blank flash
            if (currentIndex != -1 && newIndex != -1 && newIndex < currentIndex) {
                setCustomAnimations(
                    R.anim.nav_pop_enter,
                    R.anim.nav_pop_exit,
                    R.anim.nav_enter,
                    R.anim.nav_exit
                )
            } else {
                setCustomAnimations(
                    R.anim.nav_enter,
                    R.anim.nav_exit,
                    R.anim.nav_pop_enter,
                    R.anim.nav_pop_exit
                )
            }
            replace(R.id.fragment_container, selectedFragment)
            if (addToBackStack) {
                addToBackStack(null)
            }
            commit()
        }

        lastSelectedItemId = itemId
        if (itemId != DOWNLOADS_SCREEN_ID) {
            binding.bottomNavigation.menu.findItem(itemId)?.isChecked = true
        }
        updateToolbarForFragment(selectedFragment)
    }

    fun openInAppBrowser(url: String, title: String? = null) {
        if (url.isBlank()) {
            Toast.makeText(this, getString(R.string.could_not_open_link), Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(InAppBrowserActivity.createIntent(this, url, title))
    }

    private fun updateBottomNavSelection() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        val itemId = when (currentFragment) {
            is HomeUpdateFragment -> R.id.navigation_update
            is DevicesFragment -> R.id.navigation_devices
            is DownloadsFragment -> lastSelectedItemId
            is OtaToolsFragment -> R.id.navigation_ota_tools
            is AboutFragment -> R.id.navigation_about
            is SettingsFragment -> R.id.navigation_settings
            else -> return
        }
        binding.bottomNavigation.menu.findItem(itemId)?.let {
            if (it.isVisible) {
                it.isChecked = true
                lastSelectedItemId = itemId
            }
        }
    }

    private fun updateToolbarForFragment(fragment: Fragment) {
        // Show FAB on main content tabs, hide on secondary screens
        val isMainTab = fragment is HomeUpdateFragment || fragment is DevicesFragment || fragment is OtaToolsFragment
        if (isMainTab) {
            binding.downloadsFab.show()
        } else {
            binding.downloadsFab.hide()
        }
        updateNotificationDotVisibility()
    }

    fun navigateToLibraries() {
        navigateToFragment(R.id.navigation_libraries)
    }

    private fun observeDownloads() {
        lifecycleScope.launch {
            downloadRepository.allDownloads.collectLatest { downloads ->
                isDownloading = downloads.any { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED }
                updateNotificationDotVisibility()
            }
        }
    }

    private fun updateNotificationDotVisibility() {
        val isFabVisible = binding.downloadsFab.visibility == View.VISIBLE
        binding.notificationDot.visibility = if (isDownloading && isFabVisible) View.VISIBLE else View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        appSettingsPrefs.unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun isNightModeActive(): Boolean {
        val uiMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return uiMode == Configuration.UI_MODE_NIGHT_YES
    }
}
