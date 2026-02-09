package com.abhinav.otapulse.ui.custom

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.abhinav.otapulse.databinding.FragmentCustomUpdateBinding
import com.abhinav.otapulse.domain.model.OtaUpdate
import com.abhinav.otapulse.util.DeviceUtils
import com.abhinav.otapulse.util.RegionData
import com.abhinav.otapulse.util.setHapticClickListener
import com.google.android.material.transition.MaterialFadeThrough
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CustomUpdateFragment : Fragment() {

    private var _binding: FragmentCustomUpdateBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CustomUpdateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Use Material FadeThrough for standard tab transitions
        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomUpdateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinners()
        setupClickListeners()
        observeViewModel()
        runEnterAnimation()
    }

    /**
     * Runs a staggered entrance animation for the main views.
     */
    private fun runEnterAnimation() {
        // Elements to animate in order
        val viewsToAnimate = listOf(
            binding.tvHeaderTitle,
            binding.tvHeaderSubtitle,
            binding.cardInputForm
        )

        viewsToAnimate.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 50f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(index * 75L) // Stagger by 75ms
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun setupSpinners() {
        val ruiVersions = listOf("2", "3", "4", "5", "6", "7")
        val ruiAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, ruiVersions)
        binding.spinnerRuiVersion.setAdapter(ruiAdapter)

        val regions = RegionData.regions.map { it.displayName }
        val regionAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, regions)
        binding.spinnerRegion.setAdapter(regionAdapter)

        val letters = listOf("A", "C", "F", "H")
        val letterAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, letters)
        binding.spinnerVersionLetter.setAdapter(letterAdapter)

        // NEW: Server Options
        val servers = listOf("GL", "CN", "IN", "EU")
        val serverAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, servers)
        binding.spinnerServer.setAdapter(serverAdapter)

        // Defaults
        binding.spinnerRuiVersion.setText("4", false)
        binding.spinnerRegion.setText("GLO", false)
        binding.spinnerVersionLetter.setText("F", false)
        binding.spinnerServer.setText("GL", false)

        // NEW: Smart Server Selection based on Region
        binding.spinnerRegion.setOnItemClickListener { _, _, position, _ ->
            val selectedRegionName = regionAdapter.getItem(position)
            val regionInfo = RegionData.regions.find { it.displayName == selectedRegionName }

            // If the region exists in data, auto-select its default server code
            regionInfo?.let {
                // Map the serverCode (from RegionData) to our spinner options
                // If specific map isn't found, keep current selection
                if (servers.contains(it.serverCode)) {
                    binding.spinnerServer.setText(it.serverCode, false)
                }
            }
        }

        // NEW: Advanced Options Logic
        binding.checkboxAdvancedOptions.setOnCheckedChangeListener { _, isChecked ->
            binding.advancedOptionsContainer.isVisible = isChecked
            // Animate layout changes
            // binding.root.transitionLayout() // Optional, if using TransitionManager
        }

        // Setup Language Spinner
        val languages = listOf("en-EN", "zh-CN", "ru-RU", "hi-IN", "es-ES")
        val languageAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, languages)
        binding.spinnerLanguage.setAdapter(languageAdapter)
        binding.spinnerLanguage.setText("en-EN", false)
    }

    private fun setupClickListeners() {
        binding.btnAutoFill.setHapticClickListener {
            // Fetch properties
            val model = DeviceUtils.getSystemProperty("ro.product.model") // e.g. RMX3840
            val name = DeviceUtils.getSystemProperty("ro.product.name")   // e.g. RMX3840EEA
            val nvId = DeviceUtils.getSystemProperty("ro.build.oplus_nv_id") // NEW: NV ID

            // Populate fields
            if (model.isNotBlank()) binding.inputProductModel.setText(model)
            if (name.isNotBlank()) binding.inputProductName.setText(name)
            if (nvId.isNotBlank()) binding.inputNvid.setText(nvId)

            // Smart Region Selection based on Product Name suffix
            if (name.isNotBlank()) {
                val foundRegion = when {
                    name.endsWith("EEA", ignoreCase = true) -> RegionData.regions.find { it.displayName == "EU" }
                    name.endsWith("RU", ignoreCase = true) -> RegionData.regions.find { it.displayName == "RU" }
                    name.endsWith("IN", ignoreCase = true) -> RegionData.regions.find { it.displayName == "IN" }
                    name.endsWith("CN", ignoreCase = true) -> RegionData.regions.find { it.displayName == "CN" }
                    name.endsWith("TR", ignoreCase = true) -> RegionData.regions.find { it.displayName == "TR" }
                    name.endsWith("TW", ignoreCase = true) -> RegionData.regions.find { it.displayName == "TW" }
                    else -> RegionData.regions.find { name.endsWith(it.displayName, ignoreCase = true) }
                }

                if (foundRegion != null) {
                    binding.spinnerRegion.setText(foundRegion.displayName, false)
                    // Trigger manual server update logic
                    if (listOf("GL", "CN", "IN", "EU").contains(foundRegion.serverCode)) {
                        binding.spinnerServer.setText(foundRegion.serverCode, false)
                    }
                }
            }

            Toast.makeText(requireContext(), "Device details fetched", Toast.LENGTH_SHORT).show()
        }

        binding.buttonSubmit.setHapticClickListener {
            val modelInput = binding.inputProductModel.text.toString().trim()
            val nameInput = binding.inputProductName.text.toString().trim()
            val ruiVersionStr = binding.spinnerRuiVersion.text.toString().trim()
            val region = binding.spinnerRegion.text.toString().trim()
            val letter = binding.spinnerVersionLetter.text.toString().trim()
            val server = binding.spinnerServer.text.toString().trim() // NEW

            // 1. Determine "API Model Parameter"
            // ALWAYS use Product Name (e.g. RMX3840EEA) if available, otherwise Model.
            val apiModelParam = if (nameInput.isNotBlank()) nameInput else modelInput

            // 2. Determine "OTA Version String"
            val otaVersionString = constructOtaString(apiModelParam, region, letter)

            if (apiModelParam.isBlank() || otaVersionString.isBlank() || ruiVersionStr.isBlank() || region.isBlank() || server.isBlank()) {
                Toast.makeText(requireContext(), "Please fill required fields", Toast.LENGTH_SHORT).show()
                return@setHapticClickListener
            }

            val ruiVersion = ruiVersionStr.toIntOrNull() ?: 4
            val regionsArray = RegionData.regions.map { it.displayName }.toTypedArray()

            // Advanced Options
            val isBeta = binding.checkboxBeta.isChecked
            val imei = binding.inputImei.text.toString().trim().takeIf { it.isNotBlank() } ?: "0"
            
            // NV ID Logic:
            // 1. User Input (Priority)
            // 2. System Property (ro.build.oplus_nv_id)
            // 3. Null (falls back to RegionData default in OtaApi)
            val inputNvId = binding.inputNvid.text.toString().trim()
            val systemNvId = DeviceUtils.getSystemProperty("ro.build.oplus_nv_id")
            val finalNvId = inputNvId.ifBlank { systemNvId }.takeIf { it.isNotBlank() }

            val language = binding.spinnerLanguage.text.toString().trim().takeIf { it.isNotBlank() } ?: "en-EN"

            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(binding.root.windowToken, 0)

            // Pass 'apiModelParam' (e.g. RMX3840EEA) as the model for the request body
            // Pass 'otaVersionString' (e.g. RMX3840NV44...) as the firmware version
            // Pass 'server' to allow forcing a different server
            viewModel.sendRequest(
                model = apiModelParam,
                otaVersion = otaVersionString,
                ruiVersion = ruiVersion,
                region = region,
                server = server,
                regionsArray = regionsArray,
                imei = imei,
                beta = isBeta,
                nvId = finalNvId,
                language = language
            )
        }

        binding.btnDownloadOta.setHapticClickListener {
            val state = viewModel.uiState.value
            state.result?.getOrNull()?.let { ota ->
                // Use the user-entered model and region for the download
                val deviceName = binding.inputProductModel.text.toString().trim()
                val regionName = binding.spinnerRegion.text.toString().trim()

                viewModel.startDownload(ota, deviceName, regionName)
                Toast.makeText(requireContext(), "Download started", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCopyLink.setHapticClickListener {
            viewModel.uiState.value.result?.getOrNull()?.let { ota ->
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("OTA Download Link", ota.url)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(requireContext(), "Link copied to clipboard!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnChangelog.setHapticClickListener {
            viewModel.uiState.value.result?.getOrNull()?.let { ota ->
                ota.panelUrl?.let { url ->
                    if (url.isNotEmpty()) {
                        try {
                            val browserIntent = Intent(Intent.ACTION_VIEW, url.toUri())
                            startActivity(browserIntent)
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Cannot open link", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "No changelog available.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /**
     * Constructs the standard OTA string.
     * Logic:
     * 1. Start with the "Input ID" (e.g., RMX3840EEA).
     * 2. Identify and strip the specific region suffix to get the Base Model (e.g., RMX3840).
     * 3. Append the correct NV ID (e.g., NV44).
     * 4. Result: RMX3840NV44...
     */
    private fun constructOtaString(rawId: String, region: String, letter: String): String {
        val regionInfo = RegionData.regions.find { it.displayName == region }
        val nvId = regionInfo?.nvid ?: "0000"

        // Define suffixes to strip based on the selected region or common patterns
        val suffixToStrip = when (region) {
            "EU" -> "EEA" // Specific case for Europe
            "IN" -> "IN"
            "RU" -> "RU"
            "TR" -> "TR"
            "CN" -> "CN"
            else -> regionInfo?.displayName ?: ""
        }

        // Strip the suffix if present
        val cleanBase = if (suffixToStrip.isNotEmpty() && rawId.endsWith(suffixToStrip, ignoreCase = true)) {
            rawId.dropLast(suffixToStrip.length)
        } else {
            rawId
        }

        // Check if system has NV ID (ro.build.oplus_nv_id)
        // If present, user requested to REMOVE NVID from the finalBase (i.e. do not append it)
        val systemNvId = DeviceUtils.getSystemProperty("ro.build.oplus_nv_id")
        if (systemNvId.isNotBlank()) {
            return "${cleanBase}_11.${letter}.01_0001_100001010000"
        }

        // Construct base: CleanModel + NVID
        // Avoid double-appending if the user typed the NV ID manually
        val finalBase = if (cleanBase.contains(nvId, ignoreCase = true)) cleanBase else "${cleanBase}${nvId}"

        return "${finalBase}_11.${letter}.01_0001_100001010000"
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // UI Logic for loading state inside the button
                    if (state.isLoading) {
                        binding.progressBar.isVisible = true
                        binding.buttonSubmit.isEnabled = false
                        binding.buttonSubmit.text = "" // Hide text
                    } else {
                        binding.progressBar.isVisible = false
                        binding.buttonSubmit.isEnabled = true
                        binding.buttonSubmit.text = "Check" // Restore text
                    }

                    if (state.result != null) {
                        state.result.onSuccess { ota ->
                            binding.otaDetailsContainer.isVisible = true
                            binding.errorCard.isVisible = false
                            bindOtaDetails(ota)
                        }.onFailure { error ->
                            binding.otaDetailsContainer.isVisible = false
                            binding.errorCard.isVisible = true
                            binding.errorTextView.text = formatErrorMessage(error.message)
                        }
                    } else {
                        binding.otaDetailsContainer.isVisible = false
                        binding.errorCard.isVisible = false
                    }
                }
            }
        }
    }

    private fun bindOtaDetails(ota: OtaUpdate) {
        binding.tvComponentName.text = "OTA Found"
        binding.tvVersionName.text = ota.versionName ?: "Unknown Version"
        binding.tvAndroidVersion.text = ota.realAndroidVersion ?: "Unknown"
        binding.tvRealOsVersion.text = ota.realOsVersion ?: "Unknown"
        binding.tvSecurityPatch.text = ota.securityPatch ?: "Unknown"
        binding.tvSize.text = ota.size
        binding.tvMd5.text = ota.md5
    }

    private fun formatErrorMessage(msg: String?): String {
        return when {
            msg == null -> "Unknown Error"
            msg.contains("Server Error: 2004") -> "No update found for these details."
            msg.contains("resolve") || msg.contains("connect") -> "Network error. Please check your connection."
            else -> msg
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}