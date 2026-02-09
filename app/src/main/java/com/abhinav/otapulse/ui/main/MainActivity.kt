package com.abhinav.otapulse.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.abhinav.otapulse.R
import com.abhinav.otapulse.databinding.ActivityMainBinding
import com.abhinav.otapulse.domain.repository.DownloadRepository
import com.abhinav.otapulse.ui.about.AboutFragment
import com.abhinav.otapulse.ui.custom.CustomUpdateFragment
import com.abhinav.otapulse.ui.device.DeviceFragment
import com.abhinav.otapulse.ui.devices.AddDeviceFragment
import com.abhinav.otapulse.ui.devices.DevicesFragment
import com.abhinav.otapulse.ui.downloads.DownloadsFragment
import com.abhinav.otapulse.ui.settings.SettingsFragment
import com.abhinav.otapulse.ui.settings.libraries.LibrariesFragment

import com.abhinav.otapulse.util.setHapticClickListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tonyodev.fetch2.Status
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject
    lateinit var downloadRepository: DownloadRepository

    private lateinit var binding: ActivityMainBinding
    private var lastSelectedItemId = 0
    private var isDownloading: Boolean = false
    private val DOWNLOADS_SCREEN_ID = -1
    private lateinit var appSettingsPrefs: SharedPreferences
    private lateinit var mainAppPrefs: SharedPreferences

    companion object {
        private const val PREFS_NAME = "ota_pulse_app_prefs"
        private const val KEY_FIRST_LAUNCH = "is_first_launch"
    }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (!isGranted) {
                Toast.makeText(this, "Notification permission denied. You might miss download updates.", Toast.LENGTH_LONG).show()
            }
        }

    private val manageStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (android.os.Environment.isExternalStorageManager()) {
                Toast.makeText(this, "Storage permission granted", Toast.LENGTH_SHORT).show()
                // Check for notification permission next if needed
                checkNotificationPermission()
            } else {
                Toast.makeText(this, "Storage permission is required for downloads", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val viewModel: com.abhinav.otapulse.ui.main.MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        mainAppPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        appSettingsPrefs = getSharedPreferences(SettingsFragment.APP_SETTINGS_PREFS, Context.MODE_PRIVATE)
        appSettingsPrefs.registerOnSharedPreferenceChangeListener(this)

        val themePrefs = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val nightMode = themePrefs.getInt("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(nightMode)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupEdgeToEdge()
        setupNavigation()
        updateCustomUpdateTabVisibility()
        observeDownloads()
        handleFirstLaunchPermissions()

        // AUTO UPDATE CHECK
        checkForAppUpdates()
        observeAppUpdates()

        if (savedInstanceState == null) {
            navigateToFragment(R.id.navigation_device, false)
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
                if (supportFragmentManager.backStackEntryCount > 1) {
                    supportFragmentManager.popBackStack()
                } else {
                    finish()
                }
            }
        })
    }

    private fun checkForAppUpdates() {
        val isAutoUpdateEnabled = appSettingsPrefs.getBoolean(SettingsFragment.PREF_AUTO_UPDATE_CHECK, true)
        if (!isAutoUpdateEnabled) return

        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val currentVersion = packageInfo.versionName ?: "1.0.0"
            viewModel.checkForUpdate(currentVersion)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun observeAppUpdates() {
        lifecycleScope.launch {
            viewModel.appUpdateState.collectLatest { updateInfo ->
                if (updateInfo != null && !isDestroyed && !isFinishing) {
                    showUpdateDialog(updateInfo)
                    viewModel.clearUpdateState()
                }
            }
        }
    }

    private fun showUpdateDialog(info: com.abhinav.otapulse.domain.model.AppUpdateInfo) {
        MaterialAlertDialogBuilder(this)
            .setTitle("New Version Available: ${info.version}")
            .setMessage("A new update is available!\n\nChangelog:\n${info.changelog}")
            .setPositiveButton("Download") { _, _ ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl))
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Could not open download link", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun handleFirstLaunchPermissions() {
        // We now check permissions on every launch (or at least check if missing) to ensure app functionality
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
                    .setNegativeButton("Exit") { _, _ ->
                        finish()
                    }
                    .show()
            } else {
                checkNotificationPermission()
            }
        } else {
            // For older Android versions, we rely on runtime permissions requested at point of use or here if needed.
            // But usually WRITE_EXTERNAL_STORAGE is sufficient and requested in Fragment.
            checkNotificationPermission()
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                 // Only ask if it's really the first launch or we want to persist, 
                 // but typically better to ask on context. 
                 // However, existing logic asked on first launch. Let's keep it but slightly relaxed or just ask.
                 if (mainAppPrefs.getBoolean(KEY_FIRST_LAUNCH, true)) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Notification Permission")
                        .setMessage("OTA Pulse uses notifications to keep you informed about download progress. Please grant this permission.")
                        .setPositiveButton("Grant") { _, _ ->
                            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        .setNegativeButton("Later") { dialog, _ -> dialog.dismiss() }
                        .show()
                    mainAppPrefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
                 }
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == SettingsFragment.PREF_ADVANCED_MODE_ENABLED) {
            updateCustomUpdateTabVisibility()
        }
    }

    private fun updateCustomUpdateTabVisibility() {
        val isAdvancedModeEnabled = appSettingsPrefs.getBoolean(SettingsFragment.PREF_ADVANCED_MODE_ENABLED, false)
        val customUpdateMenuItem = binding.bottomNavigation.menu.findItem(R.id.navigation_custom_update)
        customUpdateMenuItem?.isVisible = isAdvancedModeEnabled

        if (!isAdvancedModeEnabled && lastSelectedItemId == R.id.navigation_custom_update) {
            if (supportFragmentManager.findFragmentById(R.id.fragment_container) is CustomUpdateFragment) {
                navigateToFragment(R.id.navigation_device, false)
            }
        }
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainContainer) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = insets.left, top = insets.top, right = insets.right, bottom = insets.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun setupNavigation() {
        binding.downloadsFab.setHapticClickListener {
            navigateToFragment(DOWNLOADS_SCREEN_ID)
        }
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (item.itemId == lastSelectedItemId && item.itemId != R.id.navigation_device) return@setOnItemSelectedListener false
            if (!item.isVisible) return@setOnItemSelectedListener false
            navigateToFragment(item.itemId)
            true
        }
    }

    private fun navigateToFragment(itemId: Int, addToBackStack: Boolean = true) {
        val selectedFragment: Fragment = when (itemId) {
            DOWNLOADS_SCREEN_ID -> DownloadsFragment()
            R.id.navigation_devices -> DevicesFragment()
            R.id.navigation_custom_update -> {
                if (!appSettingsPrefs.getBoolean(SettingsFragment.PREF_ADVANCED_MODE_ENABLED, false)) {
                    DeviceFragment()
                } else {
                    CustomUpdateFragment()
                }
            }
            R.id.navigation_about -> AboutFragment()
            R.id.navigation_settings -> SettingsFragment()
            R.id.navigation_settings -> SettingsFragment()
            R.id.navigation_libraries -> LibrariesFragment()
            else -> DeviceFragment()
        }

        supportFragmentManager.beginTransaction().apply {
            setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
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

    private fun updateBottomNavSelection() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        val itemId = when (currentFragment) {
            is DeviceFragment -> R.id.navigation_device
            is DevicesFragment -> R.id.navigation_devices
            is DownloadsFragment -> lastSelectedItemId
            is CustomUpdateFragment -> R.id.navigation_custom_update
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
        val showFab = fragment is DevicesFragment || fragment is CustomUpdateFragment || fragment is DeviceFragment
        // Also show on DeviceFragment now, or keep same logic?
        // Original logic: val showToolbarButton = fragment is DevicesFragment || fragment is CustomUpdateFragment
        // Let's stick to original logic for consistency unless user asked otherwise.
        // Wait, user just said "move".
        // Let's keep it visible on relevant screens.
        val showDownloadsButton = fragment !is DownloadsFragment && fragment !is SettingsFragment && fragment !is AboutFragment
         // Actually, let's just use the same logic as before but maybe expand it if it makes sense for a floating FAB?
         // Original: fragment is DevicesFragment || fragment is CustomUpdateFragment
         // Let's extend it to DeviceFragment too since it's "floating" now and accessible.
         // Actually, let's Stick to the requested change: "move ... to top of bottom navigation".
         // I will make it visible on the main tabs: Device, Devices, CustomUpdate.
         // And hide on Settings, About, Downloads.

        val isMainTab = fragment is DeviceFragment || fragment is DevicesFragment || fragment is CustomUpdateFragment
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
                isDownloading = downloads.any { it.status == Status.DOWNLOADING || it.status == Status.QUEUED }
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
}