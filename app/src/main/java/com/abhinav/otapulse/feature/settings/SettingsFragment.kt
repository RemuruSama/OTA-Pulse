package com.abhinav.otapulse.feature.settings

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        setupArbDetectionSwitch()
        runEnterAnimation()
    }

    private fun runEnterAnimation() {
        val viewsToAnimate = listOfNotNull(
            binding.appearanceHeader,
            binding.themeCard,
            binding.generalHeader,
            binding.generalCard,
            binding.dataHeader,
            binding.dataCard,
            binding.infoHeader,
            binding.aboutCard
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
        binding.autoUpdateSwitch?.isChecked = isAutoUpdateEnabled

        binding.autoUpdateSwitch?.setOnCheckedChangeListener { buttonView, isChecked ->
            buttonView.performHapticFeedback()
            appSettingsPrefs.edit().putBoolean(PREF_AUTO_UPDATE_CHECK, isChecked).apply()
        }
    }

    private fun setupArbDetectionSwitch() {
        val isArbDetectionEnabled = appSettingsPrefs.getBoolean(PREF_ARB_DETECTION_ENABLED, true)
        binding.arbDetectionSwitch?.isChecked = isArbDetectionEnabled

        binding.arbDetectionSwitch?.setOnCheckedChangeListener { buttonView, isChecked ->
            buttonView.performHapticFeedback()
            appSettingsPrefs.edit().putBoolean(PREF_ARB_DETECTION_ENABLED, isChecked).apply()
            Toast.makeText(requireContext(), if (isChecked) "ARB Detection Enabled" else "ARB Detection Disabled", Toast.LENGTH_SHORT).show()
        }
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
        binding.librariesButton?.let {
            it.setHapticClickListener {
                (requireActivity() as? com.abhinav.otapulse.app.MainActivity)?.navigateToLibraries()
                // android.widget.Toast.makeText(requireContext(), "Libraries Clicked", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        binding.developerButton?.let {
            it.setHapticClickListener { showDeveloperDialog() }
        }
        binding.contributorsButton?.let {
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
            val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse("https://github.com/RemuruSama"))
            startActivity(browserIntent)
        }

        dialog.show()
    }

    private fun showContributorsDialog() {
        val dialogBinding = DialogContributorsBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnGithub.setHapticClickListener {
            val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse("https://github.com/RemuruSama/OTA-Pulse"))
            startActivity(browserIntent)
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
        AppCompatDelegate.setDefaultNightMode(nightMode)
        val themePrefs = requireActivity().getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        themePrefs.edit().putInt("night_mode", nightMode).apply()
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
        val description = when (nightMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> getString(R.string.light_theme_description)
            AppCompatDelegate.MODE_NIGHT_YES -> getString(R.string.dark_theme_description)
            AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY -> getString(R.string.auto_theme_description)
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> getString(R.string.system_theme_description)
            else -> getString(R.string.system_theme_description)
        }
        binding.themeDescription.text = description
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
