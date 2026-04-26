package com.abhinav.otapulse.feature.updates.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.abhinav.otapulse.R
import com.abhinav.otapulse.catalog.model.RegionData
import com.abhinav.otapulse.core.common.DeviceUtils
import com.abhinav.otapulse.core.common.PermissionHelper
import com.abhinav.otapulse.core.common.openInAppBrowser
import com.abhinav.otapulse.core.common.setHapticClickListener
import com.abhinav.otapulse.core.model.OtaUpdate
import com.abhinav.otapulse.core.ui.applyBackgroundBlur
import com.abhinav.otapulse.databinding.FragmentHomeUpdateBinding
import com.abhinav.otapulse.feature.otatools.ui.JsonOutputActivity
import com.abhinav.otapulse.feature.otatools.ui.OtaToolsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HomeUpdateFragment : Fragment() {
    private val serverSearchOrder = listOf("EU", "GL", "IN", "CN")

    private var _binding: FragmentHomeUpdateBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OtaToolsViewModel by viewModels()

    @Inject
    lateinit var permissionHelper: PermissionHelper

    private var pendingDownload: (() -> Unit)? = null

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            proceedWithPendingDownloadIfPermissionsGranted()
        } else {
            Toast.makeText(requireContext(), getString(R.string.permission_denied_download_error), Toast.LENGTH_SHORT).show()
            pendingDownload = null
        }
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            proceedWithPendingDownloadIfPermissionsGranted()
        } else {
            Toast.makeText(requireContext(), getString(R.string.notification_permission_denied), Toast.LENGTH_SHORT).show()
            pendingDownload = null
        }
    }

    private val manageStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (android.os.Environment.isExternalStorageManager()) {
                if (checkAndRequestPermissions()) {
                    proceedWithPendingDownloadIfPermissionsGranted()
                }
            } else {
                Toast.makeText(requireContext(), getString(R.string.permission_denied_download_error), Toast.LENGTH_SHORT).show()
                pendingDownload = null
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeUpdateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpinners()
        populateDeviceInputs()
        setupClickListeners()
        observeViewModel()
        runEnterAnimation()
        setupBlurEffects()
    }

    private fun setupBlurEffects() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.viewCardBlur.setRenderEffect(
                RenderEffect.createBlurEffect(100f, 100f, Shader.TileMode.CLAMP)
            )
        }
    }

    private fun runEnterAnimation() {
        val viewsToAnimate = listOfNotNull(
            binding.cardUpdatePreview,
            binding.buttonSubmit
        )
        com.abhinav.otapulse.core.common.AnimationUtils.animateEntrance(viewsToAnimate)
    }

    private fun setupSpinners() {
        val letters = listOf("A", "C", "F", "H", "J")
        binding.spinnerVersionLetter.setAdapter(
            ArrayAdapter(requireContext(), R.layout.item_spinner_dropdown, android.R.id.text1, letters)
        )

        val reqModes = listOf("manual", "server_auto", "client_auto", "taste")
        binding.spinnerReqMode.setAdapter(
            ArrayAdapter(requireContext(), R.layout.item_spinner_dropdown, android.R.id.text1, reqModes)
        )

        binding.spinnerVersionLetter.setText("A", false)
        binding.spinnerReqMode.setText(defaultReqMode(), false)
    }

    private fun populateDeviceInputs() {
        val deviceName = DeviceUtils.getDeviceName()
        val model = DeviceUtils.getSystemProperty("ro.product.model")
        val name = DeviceUtils.getSystemProperty("ro.product.name")
        val nvId = DeviceUtils.getSystemProperty("ro.build.oplus_nv_id")
        val otaVersionLetter = DeviceUtils.getOtaVersionLetter()
        val osVersion = DeviceUtils.getOsVersion()
        val displayOtaVersion = DeviceUtils.getDisplayOtaVersion()
        val fallbackOtaVersion = DeviceUtils.getOtaVersion()

        if (binding.inputProductModel.text.isNullOrBlank() && model.isNotBlank()) {
            binding.inputProductModel.setText(model)
        }
        if (binding.inputProductName.text.isNullOrBlank() && name.isNotBlank()) {
            binding.inputProductName.setText(name)
        }
        if (binding.inputNvid.text.isNullOrBlank() && nvId.isNotBlank()) {
            binding.inputNvid.setText(nvId)
        }
        if (otaVersionLetter.isNotBlank()) {
            binding.spinnerVersionLetter.setText(otaVersionLetter, false)
        }
        if (isOnePlusDevice()) {
            binding.spinnerReqMode.setText("taste", false)
        }

        binding.tvPanelDevice.text = deviceName.ifBlank { getString(R.string.unknown) }
        binding.tvPanelName.text = name.ifBlank { name.ifBlank { getString(R.string.unknown) } }
        binding.tvPanelVersionValue.text = osVersion.ifBlank {
            displayOtaVersion.ifBlank {
                fallbackOtaVersion.ifBlank { getString(R.string.unknown_version) }
            }
        }
    }

    private fun setupClickListeners() {
        binding.buttonSubmit.setHapticClickListener {
            val modelInput = binding.inputProductModel.text.toString().trim()
            val nameInput = binding.inputProductName.text.toString().trim()
            val letter = binding.spinnerVersionLetter.text.toString().trim()
            val inputNvId = binding.inputNvid.text.toString().trim()
            val reqMode = binding.spinnerReqMode.text.toString().trim().ifBlank { defaultReqMode() }
            val systemNvId = DeviceUtils.getSystemProperty("ro.build.oplus_nv_id")
            val region = inferRegionFromNvId(inputNvId.ifBlank { systemNvId })
            val apiModelParam = if (nameInput.isNotBlank()) nameInput else modelInput
            val otaVersionString = constructOtaString(modelInput, letter)

            if (modelInput.isBlank() || apiModelParam.isBlank() || region.isBlank() || otaVersionString.isBlank()) {
                Toast.makeText(requireContext(), getString(R.string.toast_please_fill_required_fields), Toast.LENGTH_SHORT).show()
                return@setHapticClickListener
            }

            (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                ?.hideSoftInputFromWindow(binding.root.windowToken, 0)

            val finalNvId = inputNvId.ifBlank { systemNvId }
            val customSearchOrder = if (finalNvId == "10010111") {
                listOf("CN") + (serverSearchOrder - "CN")
            } else {
                serverSearchOrder
            }

            viewModel.sendRequestAcrossServers(
                model = apiModelParam,
                otaVersion = otaVersionString,
                ruiVersion = 4,
                region = region,
                servers = customSearchOrder,
                regionsArray = RegionData.regions.map { it.displayName }.toTypedArray(),
                imei = "0",
                beta = false,
                nvId = finalNvId.takeIf { it.isNotBlank() },
                language = "en-EN",
                reqMode = reqMode,
                gray = 0,
                autoShowDialog = false
            )
        }

    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.isLoading) {
                        binding.progressBar.indicatorColor = binding.buttonSubmit.currentTextColor
                        binding.progressBar.isVisible = true
                        binding.buttonSubmit.isEnabled = false
                        binding.buttonSubmit.text = ""
                        
                        binding.dividerUpdate.isVisible = false
                        binding.layoutUpdateAvailable.isVisible = false
                    } else {
                        binding.progressBar.isVisible = false
                        binding.buttonSubmit.isEnabled = true
                        binding.buttonSubmit.text = getString(R.string.btn_check_for_update)
                    }

                    if (state.result != null) {
                        state.result.onSuccess { ota ->
                            binding.errorCard.isVisible = false
                            
                            binding.dividerUpdate.isVisible = true
                            binding.layoutUpdateAvailable.isVisible = true
                            binding.tvUpdateVersionValue.text = ota.versionName ?: getString(R.string.unknown_version)
                            binding.btnViewUpdate.setHapticClickListener {
                                showResultDialog(ota)
                            }
                        }.onFailure { error ->
                            binding.errorCard.isVisible = true
                            binding.errorTextView.text = formatErrorMessage(error.message)
                            
                            binding.dividerUpdate.isVisible = false
                            binding.layoutUpdateAvailable.isVisible = false
                        }
                    } else {
                        binding.errorCard.isVisible = false
                    }

                    state.showOtaDetailsDialog?.let { ota ->
                        showResultDialog(ota)
                        viewModel.clearOtaDetailsDialog()
                    }

                    state.userMessage?.let { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        viewModel.clearUserMessage()
                    }
                }
            }
        }
    }

    private fun showResultDialog(ota: OtaUpdate) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_ota_details, null)

        val tvComponentName = dialogView.findViewById<TextView>(R.id.tvComponentName)
        val tvVersionName = dialogView.findViewById<TextView>(R.id.tvVersionName)
        val tvAndroidVersion = dialogView.findViewById<TextView>(R.id.tvAndroidVersion)
        val tvSecurityPatch = dialogView.findViewById<TextView>(R.id.tvSecurityPatch)
        val tvArbStatus = dialogView.findViewById<TextView>(R.id.tvArbStatus)
        val tvSize = dialogView.findViewById<TextView>(R.id.tvSize)
        val tvMd5 = dialogView.findViewById<TextView>(R.id.tvMd5)

        val btnDownloadOta = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDownloadOta)
        val btnCopyLink = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCopyLink)
        val btnChangelog = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnChangelog)
        val btnShare = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnShare)
        val btnViewJsonTop = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnViewJsonTop)

        val partitionSelectorRow = dialogView.findViewById<View>(R.id.partitionSelectorRow)
        partitionSelectorRow.isVisible = false

        val deviceLabel = binding.inputProductModel.text?.toString()?.trim().orEmpty()
            .ifBlank { binding.inputProductName.text?.toString()?.trim().orEmpty() }
            .ifBlank { getString(R.string.unknown) }
        val regionName = inferRegionFromNvId(binding.inputNvid.text?.toString().orEmpty())

        tvComponentName.text = getString(R.string.ota_details_device, deviceLabel)
        tvVersionName.text = ota.versionName ?: getString(R.string.unknown_version)
        tvAndroidVersion.text = ota.realAndroidVersion ?: getString(R.string.unknown)
        tvSecurityPatch.text = ota.securityPatch ?: getString(R.string.unknown)

        val arbStatusText = ota.arbStatus ?: "N/A"
        tvArbStatus.text = arbStatusText
        if (arbStatusText.equals("Safe", ignoreCase = true)) {
            tvArbStatus.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
        } else if (arbStatusText.contains("Protected", ignoreCase = true)) {
            tvArbStatus.setTextColor(android.graphics.Color.parseColor("#B00020"))
        }

        tvMd5.text = ota.md5.ifBlank { "N/A" }
        tvSize.text = ota.size

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .show()

        dialog.applyBackgroundBlur()

        val isLargeScreen = resources.configuration.smallestScreenWidthDp >= 600
        val maxDialogWidthPx = (720 * resources.displayMetrics.density).toInt()
        val targetDialogWidth = if (isLargeScreen) {
            minOf((resources.displayMetrics.widthPixels * 0.68f).toInt(), maxDialogWidthPx)
        } else {
            (resources.displayMetrics.widthPixels * 1.0f).toInt()
        }
        dialog.window?.setLayout(
            targetDialogWidth,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        btnDownloadOta.setHapticClickListener {
            pendingDownload = {
                viewModel.startDownload(ota, deviceLabel, regionName)
                dialog.dismiss()
            }
            if (checkAndRequestPermissions()) {
                pendingDownload?.invoke()
                pendingDownload = null
            }
        }

        btnCopyLink.setHapticClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("OTA Link", ota.url))
            Toast.makeText(requireContext(), getString(R.string.toast_link_copied), Toast.LENGTH_SHORT).show()
        }

        btnChangelog.setHapticClickListener {
            ota.panelUrl?.let { url ->
                openInAppBrowser(url, getString(R.string.ota_details_title))
            } ?: Toast.makeText(requireContext(), getString(R.string.changelog_not_available), Toast.LENGTH_SHORT).show()
        }

        btnShare.setHapticClickListener {
            val shareText = """
                🚀 𝗢𝗧𝗔 𝗣𝘂𝗹𝘀𝗲 | 𝗨𝗽𝗱𝗮𝘁𝗲 𝗔𝗹𝗲𝗿𝘁

                • 𝗩𝗲𝗿: ${ota.versionName ?: "Unknown"}
                • 𝗔𝗻𝗱𝗿𝗼𝗶𝗱: ${ota.realAndroidVersion ?: "Unknown"}
                • 𝗦𝗲𝗰𝘂𝗿𝗶𝘁𝘆 𝗣𝗮𝘁𝗰𝗵: ${ota.securityPatch ?: "Unknown"}
                • 𝗦𝗶𝘇𝗲: ${ota.size}
                • 𝗔𝗥𝗕 𝗦𝘁𝗮𝘁𝘂𝘀: ${ota.arbStatus ?: "N/A"}

                ━━━━━━━━━━━━━━━━━
                • 𝗖𝗵𝗮𝗻𝗴𝗲𝗹𝗼𝗴: ${ota.panelUrl ?: "Not available"}

                ━━━━━━━━━━━━━━━━━
                • 𝗗𝗼𝘄𝗻𝗹𝗼𝗮𝗱: ${ota.url}

                ━━━━━━━━━━━━━━━━━
                • @abhinav_v1
            """.trimIndent()
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            startActivity(Intent.createChooser(shareIntent, "Share OTA Update for $deviceLabel"))
        }

        btnViewJsonTop.setHapticClickListener {
            if (ota.rawJson.isNullOrBlank()) {
                Toast.makeText(requireContext(), getString(R.string.json_output_unavailable), Toast.LENGTH_SHORT).show()
            } else {
                startActivity(JsonOutputActivity.createIntent(requireContext(), ota, regionName))
            }
        }
    }

    private fun defaultReqMode(): String = if (isOnePlusDevice()) "taste" else "manual"

    private fun isOnePlusDevice(): Boolean =
        DeviceUtils.getDeviceBrand().equals("OnePlus", ignoreCase = true)

    private fun inferRegionFromNvId(nvId: String): String {
        val normalizedNvId = nvId.trim()

        val nvRegion = RegionData.regions.firstOrNull {
            it.nvid.equals(normalizedNvId, ignoreCase = true)
        }?.displayName
        return nvRegion ?: "GLO"
    }

    private fun constructOtaString(rawId: String, letter: String): String {
        val suffixesToStrip = listOf("EEA", "IN", "RU", "TR", "CN", "EU", "TW", "MEA", "SA", "SG", "TH", "LATAM", "BR", "MY", "ID", "KZ", "OCA", "VN", "GLO")
            .distinct()
        var baseModel = rawId
        for (suffix in suffixesToStrip) {
            if (baseModel.endsWith(suffix, ignoreCase = true)) {
                baseModel = baseModel.dropLast(suffix.length)
                break
            }
        }

        val cleanBase = baseModel.replace(Regex("NV[0-9A-Z]{2}$", RegexOption.IGNORE_CASE), "")
        return "${cleanBase}_11.${letter}.01_0001_100001010000"
    }

    private fun checkAndRequestPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!android.os.Environment.isExternalStorageManager()) {
                requestManageAllFilesAccess()
                return false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissionHelper.hasNotificationPermission()) {
                requestNotificationPermission()
                return false
            }
            return true
        }

        val permissions = mutableListOf<String>()
        if (!permissionHelper.hasStoragePermission()) permissions.addAll(permissionHelper.getRequiredStoragePermissions())
        if (permissions.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissions.toTypedArray())
            return false
        }
        return true
    }

    private fun requestNotificationPermission() {
        val canRequestInApp = !permissionHelper.wasNotificationPermissionRequested() ||
            shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)

        if (canRequestInApp) {
            permissionHelper.markNotificationPermissionRequested()
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }

        openAppSettingsForNotificationPermission()
    }

    private fun requestManageAllFilesAccess() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.permission_needed)
            .setMessage(R.string.broad_file_access_prompt)
            .setPositiveButton(R.string.settings) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:${requireContext().packageName}")
                    manageStoragePermissionLauncher.launch(intent)
                } catch (_: Exception) {
                    manageStoragePermissionLauncher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun openAppSettingsForNotificationPermission() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.notification_permission_title)
            .setMessage(R.string.notification_permission_settings_message)
            .setPositiveButton(R.string.settings) { _, _ ->
                try {
                    startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", requireContext().packageName, null)
                        }
                    )
                } catch (_: Exception) {
                    Toast.makeText(requireContext(), getString(R.string.cannot_open_app_settings), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun proceedWithPendingDownloadIfPermissionsGranted() {
        if (pendingDownload != null) {
            if (permissionHelper.hasNotificationPermission() && permissionHelper.hasStoragePermission()) {
                pendingDownload?.invoke()
            } else {
                Toast.makeText(requireContext(), getString(R.string.permission_denied_download_error), Toast.LENGTH_SHORT).show()
            }
            pendingDownload = null
        }
    }

    private fun formatErrorMessage(msg: String?): String {
        return when {
            msg == null -> getString(R.string.software_update_error_unknown)
            msg.contains("2004") -> getString(R.string.software_update_error_no_update)
            msg.contains("resolve", ignoreCase = true) -> getString(R.string.software_update_error_network)
            else -> msg
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
