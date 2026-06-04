package com.abhinav.otapulse.feature.settings

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.abhinav.otapulse.R
import com.abhinav.otapulse.databinding.DialogContributorsBinding
import com.abhinav.otapulse.databinding.DialogDeveloperBinding
import com.abhinav.otapulse.databinding.FragmentSettingsBinding
import android.graphics.drawable.AnimatedVectorDrawable
import com.abhinav.otapulse.core.common.performHapticFeedback
import com.abhinav.otapulse.core.common.openExternalBrowser
import com.abhinav.otapulse.core.common.setHapticClickListener
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var appSettingsPrefs: SharedPreferences

    companion object {
        const val APP_SETTINGS_PREFS = "app_settings_prefs"
        const val PREF_ADVANCED_MODE_ENABLED = "advanced_mode_enabled"
        const val PREF_AUTO_UPDATE_CHECK = "auto_update_check_enabled"
        const val PREF_ARB_DETECTION_ENABLED = "arb_detection_enabled"
        const val PREF_BROWSER_DESKTOP_MODE = "browser_desktop_mode"
        const val PREF_BROWSER_SHOW_CONTROLS = "browser_show_controls"
        const val PREF_AUTO_SOFTWARE_UPDATE_CHECK = "auto_software_update_check_enabled"
        const val PREF_CHECK_INTERVAL_HOURS = "check_interval_hours"
        const val DEFAULT_CHECK_INTERVAL_HOURS = 6L
        const val PREF_AMOLED_MODE = "amoled_mode"
    }

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { exportToFile(it) }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { showImportConfirmationDialog(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        appSettingsPrefs = requireActivity().getSharedPreferences(APP_SETTINGS_PREFS, Context.MODE_PRIVATE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateThemeSelection()
        setupClickListeners()
        observeViewModel()
        setupAdvancedModeSwitch()
        setupAutoUpdateSwitch()
        setupAutoSoftwareUpdateSwitch()
        setupCheckIntervalSelector()
        setupArbDetectionSwitch()
        setupBrowserDesktopModeSwitch()
        setupBrowserControlsSwitch()
        setupAmoledSwitch()
        setupLanguageSelection()
        setupBatteryOptimization()
        bindWebViewVersion()
        runEnterAnimation()
    }

    private fun setupLanguageSelection() {
        val currentLocaleTag = com.abhinav.otapulse.core.common.LocaleHelper.getSelectedLocale(requireContext())
        binding.currentLanguageValue.text = com.abhinav.otapulse.core.common.LocaleHelper.getDisplayName(requireContext(), currentLocaleTag)
        
        binding.languageButton.setHapticClickListener {
            showLanguageSelectionDialog()
        }
    }

    private fun showLanguageSelectionDialog() {
        val languages = linkedMapOf(
            "system" to getString(R.string.language_option_system),
            "ar"    to getString(R.string.lang_ar),
            "bn"    to getString(R.string.lang_bn),
            "zh"    to getString(R.string.lang_zh),
            "zh-TW" to getString(R.string.lang_zh_TW),
            "fr"    to getString(R.string.lang_fr),
            "de"    to getString(R.string.lang_de),
            "hi"    to getString(R.string.lang_hi),
            "id"    to getString(R.string.lang_id),
            "it"    to getString(R.string.lang_it),
            "ja"    to getString(R.string.lang_ja),
            "ms"    to getString(R.string.lang_ms),
            "pt-BR" to getString(R.string.lang_pt_BR),
            "pt-PT" to getString(R.string.lang_pt_PT),
            "ru"    to getString(R.string.lang_ru),
            "es"    to getString(R.string.lang_es),
            "fil"   to getString(R.string.lang_fil),
            "th"    to getString(R.string.lang_th),
            "tr"    to getString(R.string.lang_tr),
            "ur"    to getString(R.string.lang_ur),
            "vi"    to getString(R.string.lang_vi)
        )

        val keys = languages.keys.toTypedArray()
        val values = languages.values.toTypedArray()
        val currentLocaleTag = com.abhinav.otapulse.core.common.LocaleHelper.getSelectedLocale(requireContext())
        val checkedItem = keys.indexOf(currentLocaleTag).coerceAtLeast(0)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_app_language)
            .setSingleChoiceItems(values, checkedItem) { dialog, which ->
                val selectedKey = keys[which]
                com.abhinav.otapulse.core.common.LocaleHelper.applyLocale(requireContext(), selectedKey)
                dialog.dismiss()
                // Activity will be recreated automatically by setApplicationLocales
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun runEnterAnimation() {
        val viewsToAnimate = listOfNotNull(
            binding.appearanceHeader,
            binding.themeCard,
            binding.generalHeader,
            binding.generalCard,
            binding.browserHeader,
            binding.browserCard,
            binding.dataHeader,
            binding.dataCard,
            binding.infoHeader
        )
        // Filter out nulls in case some views are not bound or don't exist in layout yet
        com.abhinav.otapulse.core.common.AnimationUtils.animateEntrance(viewsToAnimate)
    }

    private fun setupAdvancedModeSwitch() {
        val isAdvancedModeEnabled = appSettingsPrefs.getBoolean(PREF_ADVANCED_MODE_ENABLED, true)
        binding.advancedModeSwitch.isChecked = isAdvancedModeEnabled

        binding.advancedModeSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            buttonView.performHapticFeedback()
            appSettingsPrefs.edit().putBoolean(PREF_ADVANCED_MODE_ENABLED, isChecked).apply()
            Toast.makeText(requireContext(), getString(if (isChecked) R.string.advanced_mode_enabled else R.string.advanced_mode_disabled), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupAutoUpdateSwitch() {
        // Default to TRUE (enabled)
        val isAutoUpdateEnabled = appSettingsPrefs.getBoolean(PREF_AUTO_UPDATE_CHECK, true)

        // Use safe call ?. as ViewBinding might treat this as nullable
        binding.autoUpdateSwitch.isChecked = isAutoUpdateEnabled

        binding.autoUpdateSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            buttonView.performHapticFeedback()
            appSettingsPrefs.edit().putBoolean(PREF_AUTO_UPDATE_CHECK, isChecked).apply()
        }
    }

    private fun setupAutoSoftwareUpdateSwitch() {
        val isEnabled = appSettingsPrefs.getBoolean(PREF_AUTO_SOFTWARE_UPDATE_CHECK, true)
        binding.autoSoftwareUpdateSwitch.isChecked = isEnabled

        binding.autoSoftwareUpdateSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            buttonView.performHapticFeedback()
            appSettingsPrefs.edit().putBoolean(PREF_AUTO_SOFTWARE_UPDATE_CHECK, isChecked).apply()

            val workManager = androidx.work.WorkManager.getInstance(requireContext())
            if (isChecked) {
                enqueueUpdateCheckWork(workManager)
                val pm = requireContext().getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                if (!pm.isIgnoringBatteryOptimizations(requireContext().packageName)) {
                    showBatteryOptimizationDialog()
                }
            } else {
                workManager.cancelUniqueWork(
                    com.abhinav.otapulse.core.worker.SoftwareUpdateCheckWorker.WORK_NAME
                )
            }
            updateCheckIntervalVisibility(isChecked)
        }
        updateCheckIntervalVisibility(isEnabled)
    }

    private fun setupCheckIntervalSelector() {
        updateCheckIntervalLabel()
        binding.checkIntervalRow?.setHapticClickListener {
            showCheckIntervalDialog()
        }
    }

    private fun updateCheckIntervalLabel() {
        val hours = getCheckIntervalHours()
        val label = when (hours) {
            1L -> getString(R.string.settings_check_interval_1h)
            3L -> getString(R.string.settings_check_interval_3h)
            6L -> getString(R.string.settings_check_interval_6h)
            12L -> getString(R.string.settings_check_interval_12h)
            24L -> getString(R.string.settings_check_interval_24h)
            else -> getString(R.string.settings_check_interval_6h)
        }
        binding.checkIntervalValue?.text = label
    }

    private fun updateCheckIntervalVisibility(isEnabled: Boolean) {
        binding.checkIntervalRow?.visibility = if (isEnabled) View.VISIBLE else View.GONE
    }

    private fun showCheckIntervalDialog() {
        val hourValues = longArrayOf(1, 3, 6, 12, 24)
        val labels = arrayOf(
            getString(R.string.settings_check_interval_1h),
            getString(R.string.settings_check_interval_3h),
            getString(R.string.settings_check_interval_6h),
            getString(R.string.settings_check_interval_12h),
            getString(R.string.settings_check_interval_24h)
        )
        val currentHours = getCheckIntervalHours()
        val checkedItem = hourValues.indexOf(currentHours).coerceAtLeast(0)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_check_interval_title)
            .setSingleChoiceItems(labels, checkedItem) { dialog, which ->
                val selectedHours = hourValues[which]
                appSettingsPrefs.edit().putLong(PREF_CHECK_INTERVAL_HOURS, selectedHours).apply()
                updateCheckIntervalLabel()

                // Re-enqueue work with new interval
                val workManager = androidx.work.WorkManager.getInstance(requireContext())
                enqueueUpdateCheckWork(workManager, replaceExisting = true)

                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun getCheckIntervalHours(): Long {
        return appSettingsPrefs.getLong(PREF_CHECK_INTERVAL_HOURS, DEFAULT_CHECK_INTERVAL_HOURS)
    }

    private fun enqueueUpdateCheckWork(
        workManager: androidx.work.WorkManager,
        replaceExisting: Boolean = false
    ) {
        val intervalHours = getCheckIntervalHours()
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()
        val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.abhinav.otapulse.core.worker.SoftwareUpdateCheckWorker>(
            intervalHours, java.util.concurrent.TimeUnit.HOURS
        ).setConstraints(constraints)
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                10000L,
                java.util.concurrent.TimeUnit.MILLISECONDS
            )
            .build()
        workManager.enqueueUniquePeriodicWork(
            com.abhinav.otapulse.core.worker.SoftwareUpdateCheckWorker.WORK_NAME,
            if (replaceExisting) androidx.work.ExistingPeriodicWorkPolicy.UPDATE
            else androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun setupBatteryOptimization() {
        updateBatteryOptimizationUI()

        binding.batteryOptimizationLayout?.setHapticClickListener {
            val pm = requireContext().getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            if (!pm.isIgnoringBatteryOptimizations(requireContext().packageName)) {
                showBatteryOptimizationDialog()
            } else {
                val intent = android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                startActivity(intent)
            }
        }
    }

    private fun showBatteryOptimizationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.battery_optimization_dialog_title)
            .setMessage(R.string.battery_optimization_dialog_message)
            .setPositiveButton(R.string.allow) { _, _ ->
                val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = android.net.Uri.parse("package:${requireContext().packageName}")
                startActivity(intent)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun updateBatteryOptimizationUI() {
        val pm = requireContext().getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        val isIgnoring = pm.isIgnoringBatteryOptimizations(requireContext().packageName)
        
        binding.batteryOptimizationStatus?.text = getString(
            if (isIgnoring) R.string.settings_battery_optimization_ignored else R.string.settings_battery_optimization_desc
        )
        binding.batteryOptimizationIcon?.setImageResource(
            if (isIgnoring) R.drawable.ic_check_circle else R.drawable.ic_chevron_right
        )
    }

    override fun onResume() {
        super.onResume()
        if (::appSettingsPrefs.isInitialized) {
            updateBatteryOptimizationUI()
        }
    }

    private fun setupArbDetectionSwitch() {
        val isArbDetectionEnabled = appSettingsPrefs.getBoolean(PREF_ARB_DETECTION_ENABLED, true)
        binding.arbDetectionSwitch.isChecked = isArbDetectionEnabled

        binding.arbDetectionSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            buttonView.performHapticFeedback()
            appSettingsPrefs.edit().putBoolean(PREF_ARB_DETECTION_ENABLED, isChecked).apply()
            Toast.makeText(requireContext(), getString(if (isChecked) R.string.toast_arb_detection_enabled else R.string.toast_arb_detection_disabled), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBrowserDesktopModeSwitch() {
        val isEnabled = appSettingsPrefs.getBoolean(PREF_BROWSER_DESKTOP_MODE, false)
        binding.browserDesktopModeSwitch.isChecked = isEnabled

        binding.browserDesktopModeSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            buttonView.performHapticFeedback()
            appSettingsPrefs.edit().putBoolean(PREF_BROWSER_DESKTOP_MODE, isChecked).apply()
        }
    }

    private fun setupBrowserControlsSwitch() {
        val isEnabled = appSettingsPrefs.getBoolean(PREF_BROWSER_SHOW_CONTROLS, true)
        binding.browserControlsSwitch.isChecked = isEnabled

        binding.browserControlsSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            buttonView.performHapticFeedback()
            appSettingsPrefs.edit().putBoolean(PREF_BROWSER_SHOW_CONTROLS, isChecked).apply()
        }
    }

    private fun bindWebViewVersion() {
        val versionName = WebView.getCurrentWebViewPackage()?.versionName
            ?: getString(R.string.browser_webview_version_unavailable)
        binding.browserWebviewVersionValue.text = versionName
    }

    private fun setupClickListeners() {
        binding.lightThemeCard.setHapticClickListener { setTheme(AppCompatDelegate.MODE_NIGHT_NO) }
        binding.darkThemeCard.setHapticClickListener { setTheme(AppCompatDelegate.MODE_NIGHT_YES) }
        binding.autoThemeCard.setHapticClickListener { setTheme(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY) }
        binding.systemDefaultThemeCard.setHapticClickListener { setTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) }

        binding.exportButton.setHapticClickListener {
            exportLauncher.launch(createBackupFileName())
        }
        binding.importButton.setHapticClickListener {
            importLauncher.launch(arrayOf("application/json"))
        }

        // About Section Listeners
        binding.librariesButton.let {
            it.setHapticClickListener {
                (requireActivity() as? com.abhinav.otapulse.app.MainActivity)?.navigateToLibraries()
                // android.widget.Toast.makeText(requireContext(), "Libraries Clicked", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        binding.developerButton.let {
            it.setHapticClickListener { showDeveloperDialog() }
        }
        binding.contributorsButton.let {
            it.setHapticClickListener { showContributorsDialog() }
        }
    }

    private fun showDeveloperDialog() {
        val dialogBinding = DialogDeveloperBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        // Start avatar animation
        (dialogBinding.developerAvatar.drawable as? AnimatedVectorDrawable)?.start()

        dialogBinding.btnGithub.setHapticClickListener {
            openExternalBrowser("https://github.com/RemuruSama")
        }

        dialog.show()
    }

    private fun showContributorsDialog() {
        val dialogBinding = DialogContributorsBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnGithub.setHapticClickListener {
            openExternalBrowser("https://github.com/RemuruSama/OTA-Pulse")
        }

        dialogBinding.btnClose.setHapticClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    state.toastMessage?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                        viewModel.clearToastMessage()
                    }
                }
            }
        }
    }

    private fun setTheme(nightMode: Int) {
        val themePrefs = requireActivity().getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        // If switching to light mode, auto-disable AMOLED
        if (nightMode == AppCompatDelegate.MODE_NIGHT_NO) {
            themePrefs.edit()
                .putInt("night_mode", nightMode)
                .putBoolean(PREF_AMOLED_MODE, false)
                .apply()
            binding.amoledSwitch?.isChecked = false
        } else {
            themePrefs.edit().putInt("night_mode", nightMode).apply()
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
        updateThemeSelection()
    }

    private fun updateThemeSelection() {
        val themePrefs = requireActivity().getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val currentNightMode = themePrefs.getInt("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        val selectedColor = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimaryContainer, 0)
        val unselectedColor = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorSurfaceVariant, 0)

        binding.lightThemeCard.setCardBackgroundColor(unselectedColor)
        binding.darkThemeCard.setCardBackgroundColor(unselectedColor)
        binding.autoThemeCard.setCardBackgroundColor(unselectedColor)
        binding.systemDefaultThemeCard.setCardBackgroundColor(unselectedColor)

        when (currentNightMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> binding.lightThemeCard.setCardBackgroundColor(selectedColor)
            AppCompatDelegate.MODE_NIGHT_YES -> binding.darkThemeCard.setCardBackgroundColor(selectedColor)
            AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY -> binding.autoThemeCard.setCardBackgroundColor(selectedColor)
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> binding.systemDefaultThemeCard.setCardBackgroundColor(selectedColor)
        }
        updateThemeDescription(currentNightMode)
    }

    private fun updateThemeDescription(nightMode: Int) {
        binding.themeDescription.visibility = View.VISIBLE
        val themePrefs = requireActivity().getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val isAmoled = themePrefs.getBoolean(PREF_AMOLED_MODE, false)
        val description = if (isAmoled && nightMode != AppCompatDelegate.MODE_NIGHT_NO) {
            getString(R.string.amoled_theme_description)
        } else {
            when (nightMode) {
                AppCompatDelegate.MODE_NIGHT_NO -> getString(R.string.light_theme_description)
                AppCompatDelegate.MODE_NIGHT_YES -> getString(R.string.dark_theme_description)
                AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY -> getString(R.string.auto_theme_description)
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> getString(R.string.system_theme_description)
                else -> getString(R.string.system_theme_description)
            }
        }
        binding.themeDescription.text = description
    }

    private fun setupAmoledSwitch() {
        val themePrefs = requireActivity().getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val isAmoled = themePrefs.getBoolean(PREF_AMOLED_MODE, false)
        binding.amoledSwitch?.isChecked = isAmoled

        binding.amoledSwitch?.setOnCheckedChangeListener { buttonView, isChecked ->
            buttonView.performHapticFeedback()
            themePrefs.edit().putBoolean(PREF_AMOLED_MODE, isChecked).apply()
            if (isChecked) {
                // Force dark mode when enabling AMOLED
                val currentMode = themePrefs.getInt("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                if (currentMode == AppCompatDelegate.MODE_NIGHT_NO) {
                    themePrefs.edit().putInt("night_mode", AppCompatDelegate.MODE_NIGHT_YES).apply()
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
            }
            // Recreate activity to apply / remove the AMOLED overlay
            requireActivity().recreate()
        }
    }

    private fun createBackupFileName(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val dateString = dateFormat.format(Date())
        return "otapulse_backup_$dateString.json"
    }

    private fun exportToFile(uri: Uri) {
        try {
            requireActivity().contentResolver.openFileDescriptor(uri, "w")?.use {
                FileOutputStream(it.fileDescriptor).use { stream ->
                    stream.write(viewModel.getCustomDevicesAsJson().toByteArray())
                }
            }
            viewModel.onExportSuccess()
        } catch (e: Exception) {
            viewModel.onExportFailed(e.message)
        }
    }

    private fun importFromFile(uri: Uri) {
        try {
            val stringBuilder = StringBuilder()
            requireActivity().contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line)
                        line = reader.readLine()
                    }
                }
            }
            viewModel.importCustomDevices(stringBuilder.toString())
        } catch (e: Exception) {
            viewModel.onImportFailed(e.message)
        }
    }

    private fun showImportConfirmationDialog(uri: Uri) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.import_overwrite_title)
            .setMessage(R.string.import_overwrite_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.import_action) { _, _ ->
                importFromFile(uri)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
