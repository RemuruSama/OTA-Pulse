package com.abhinav.otapulse.feature.otatools.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.abhinav.otapulse.R
import com.abhinav.otapulse.databinding.FragmentManualQueryBinding
import com.abhinav.otapulse.core.model.OtaUpdate
import com.abhinav.otapulse.ota.payload.PartitionInfo
import com.abhinav.otapulse.core.common.DeviceUtils
import com.abhinav.otapulse.core.common.FormatUtils
import com.abhinav.otapulse.core.common.PermissionHelper
import com.abhinav.otapulse.core.common.openInAppBrowser
import com.abhinav.otapulse.catalog.model.RegionData
import com.abhinav.otapulse.core.common.setHapticClickListener
import com.abhinav.otapulse.core.ui.applyBackgroundBlur
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ManualQueryFragment : Fragment() {

    private var _binding: FragmentManualQueryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OtaToolsViewModel by viewModels()

    @Inject
    lateinit var permissionHelper: PermissionHelper

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    private val manageStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ -> }

    private var selectedLocalZipUri: Uri? = null
    private var selectedLocalZipName: String = ""

    private val pickLocalZipLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@registerForActivityResult
        try {
            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
        }
        selectedLocalZipUri = uri
        selectedLocalZipName = resolveDisplayName(uri)
        updateSelectedLocalZipSummary(uri)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManualQueryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurePage()
        setupSpinners()
        setupClickListeners()
        observeViewModel()
        runEnterAnimation()
    }

    private fun configurePage() {
        binding.tvBracketLabel.text = "MANUAL_QUERY.sh"
        binding.tvHeaderTitle.text = "Manual\nQuery"
        binding.tvHeaderSubtitle.text = "Query OTA servers with custom device parameters."
        binding.layoutPartitionExtractionSection.isVisible = false
        binding.layoutPartitionUrlInput.isVisible = false
        binding.cardLocalZip.isVisible = false
        binding.layoutPartitionButtons.isVisible = false
        binding.layoutLinkResolverSection.isVisible = false
        binding.layoutResolverInput.isVisible = false
        binding.btnResolveLink.isVisible = false
        binding.cardResolverResult.isVisible = false
    }

    private fun runEnterAnimation() {
        val viewsToAnimate = listOfNotNull(
            binding.tvBracketLabel,
            binding.tvHeaderTitle,
            binding.tvHeaderSubtitle,
            binding.ivHeroIcon
        )
        com.abhinav.otapulse.core.common.AnimationUtils.animateEntrance(viewsToAnimate)
    }

    private fun setupSpinners() {
        val ruiVersions = listOf("2", "3", "4", "5", "6", "7")
        val ruiAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, ruiVersions)
        binding.spinnerRuiVersion.setAdapter(ruiAdapter)

        val regions = RegionData.regions.map { it.displayName }
        val regionAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, regions)
        binding.spinnerRegion.setAdapter(regionAdapter)

        val letters = listOf("A", "C", "F", "H", "J")
        val letterAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, letters)
        binding.spinnerVersionLetter.setAdapter(letterAdapter)

        val servers = listOf("GL", "CN", "IN", "EU")
        val serverAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, servers)
        binding.spinnerServer.setAdapter(serverAdapter)

        binding.spinnerRuiVersion.setText("4", false)
        binding.spinnerRegion.setText("GLO", false)
        binding.spinnerVersionLetter.setText("A", false)
        binding.spinnerServer.setText("GL", false)

        binding.spinnerRegion.setOnItemClickListener { _, _, position, _ ->
            val selectedRegionName = regionAdapter.getItem(position)
            val regionInfo = RegionData.regions.find { it.displayName == selectedRegionName }
            regionInfo?.let {
                if (servers.contains(it.serverCode)) {
                    binding.spinnerServer.setText(it.serverCode, false)
                }
            }
        }

        val languages = listOf("en-EN", "zh-CN", "ru-RU", "hi-IN", "es-ES")
        val languageAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, languages)
        binding.spinnerLanguage.setAdapter(languageAdapter)
        binding.spinnerLanguage.setText("en-EN", false)

        val reqModes = listOf("manual", "server_auto", "client_auto", "taste")
        val reqModeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, reqModes)
        binding.spinnerReqMode.setAdapter(reqModeAdapter)
        binding.spinnerReqMode.setText("manual", false)

        val grayValues = listOf("0", "1")
        val grayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, grayValues)
        binding.spinnerGray.setAdapter(grayAdapter)
        binding.spinnerGray.setText("0", false)
    }

    private fun setupClickListeners() {
        binding.btnPickLocalZip.setHapticClickListener {
            pickLocalZipLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
        }

        binding.btnLoadUrlPartitions.setHapticClickListener {
            val source = binding.inputPartitionUrl.text?.toString()?.trim().orEmpty()
            if (source.isBlank()) {
                Toast.makeText(requireContext(), "Paste a full OTA URL first", Toast.LENGTH_SHORT).show()
                return@setHapticClickListener
            }
            viewModel.fetchExtractablePartitions(
                source = source,
                versionName = guessVersionName(source),
                sourceLabel = "Full OTA URL"
            )
        }

        binding.btnLoadLocalPartitions.setHapticClickListener {
            val localUri = selectedLocalZipUri
            if (localUri == null) {
                Toast.makeText(requireContext(), "Choose a local OTA ZIP first", Toast.LENGTH_SHORT).show()
                return@setHapticClickListener
            }
            viewModel.fetchExtractablePartitions(
                source = localUri.toString(),
                versionName = selectedLocalZipName.ifBlank { "Local OTA" },
                sourceLabel = selectedLocalZipName.ifBlank { "Local OTA ZIP" }
            )
        }

        binding.btnResolveLink.setHapticClickListener {
            viewModel.resolveLink(binding.inputResolverUrl.text?.toString().orEmpty())
        }

        binding.btnCopyResolvedUrl.setHapticClickListener {
            val resolvedUrl = viewModel.uiState.value.resolverResult?.resolvedUrl ?: return@setHapticClickListener
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Resolved OTA Link", resolvedUrl))
            Toast.makeText(requireContext(), "Resolved link copied", Toast.LENGTH_SHORT).show()
        }

        binding.btnOpenResolvedUrl.setHapticClickListener {
            val resolvedUrl = viewModel.uiState.value.resolverResult?.resolvedUrl ?: return@setHapticClickListener
            try {
                openInAppBrowser(resolvedUrl, getString(R.string.in_app_browser_title))
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "Could not open resolved link", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnAutoFill.setHapticClickListener {
            val model = DeviceUtils.getSystemProperty("ro.product.model")
            val name = DeviceUtils.getSystemProperty("ro.product.name")
            val nvId = DeviceUtils.getSystemProperty("ro.build.oplus_nv_id")
            val otaVersionLetter = DeviceUtils.getOtaVersionLetter()

            if (model.isNotBlank()) binding.inputProductModel.setText(model)
            if (name.isNotBlank()) binding.inputProductName.setText(name)
            if (nvId.isNotBlank()) binding.inputNvid.setText(nvId)
            if (otaVersionLetter.isNotBlank()) binding.spinnerVersionLetter.setText(otaVersionLetter, false)

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
            val server = binding.spinnerServer.text.toString().trim()

            val inputNvId = binding.inputNvid.text.toString().trim()
            val systemNvId = DeviceUtils.getSystemProperty("ro.build.oplus_nv_id")

            val apiModelParam = if (nameInput.isNotBlank()) nameInput else modelInput
            val otaVersionString = constructOtaString(modelInput, region, letter)

            if (modelInput.isBlank() || apiModelParam.isBlank() || otaVersionString.isBlank() || ruiVersionStr.isBlank() || region.isBlank() || server.isBlank()) {
                Toast.makeText(requireContext(), "Please fill required fields", Toast.LENGTH_SHORT).show()
                return@setHapticClickListener
            }

            val ruiVersion = ruiVersionStr.toIntOrNull() ?: 4
            val regionsArray = RegionData.regions.map { it.displayName }.toTypedArray()
            val isBeta = binding.switchBeta.isChecked
            val imei = binding.inputImei.text.toString().trim().takeIf { it.isNotBlank() } ?: "0"
            val finalNvId = inputNvId.ifBlank { systemNvId }.takeIf { it.isNotBlank() }
            val language = binding.spinnerLanguage.text.toString().trim().takeIf { it.isNotBlank() } ?: "en-EN"
            val reqMode = binding.spinnerReqMode.text.toString().trim().takeIf { it.isNotBlank() } ?: "manual"
            val gray = binding.spinnerGray.text.toString().trim().toIntOrNull() ?: 0

            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(binding.root.windowToken, 0)

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
                language = language,
                reqMode = reqMode,
                gray = gray
            )
        }
    }

    private fun constructOtaString(rawId: String, region: String, letter: String): String {
        val regionInfo = RegionData.regions.find { it.displayName == region }
        val targetNvId = regionInfo?.nvid ?: "0000"

        val suffixesToStrip = listOf("EEA", "IN", "RU", "TR", "CN", region, regionInfo?.displayName)
            .filterNotNull()
            .filter { it.isNotEmpty() }
            .distinct()

        var baseModel = rawId
        for (suffix in suffixesToStrip) {
            if (baseModel.endsWith(suffix, ignoreCase = true)) {
                baseModel = baseModel.dropLast(suffix.length)
                break
            }
        }

        // Strip any existing NV prefix (e.g. NV1B, NV44) if present
        val nvPattern = Regex("NV[0-9A-Z]{2}$", RegexOption.IGNORE_CASE)
        val cleanBase = baseModel.replace(nvPattern, "")

        // The firmware string (otaVersion) always uses the shorthand regional NV ID (e.g. NV1B)
        // Custom Binary Carry IDs (e.g. 00011011) are sent as metadata, not in the version string.
        val finalBase = if (targetNvId != "0" && targetNvId != "0000") "${cleanBase}${targetNvId}" else cleanBase
        return "${finalBase}_11.${letter}.01_0001_100001010000"
    }

    private var workInfoJob: kotlinx.coroutines.Job? = null
    private var popupAlreadyShownForData = false
    private var selectedPartition: PartitionInfo? = null
    private var activeExtractionWorkId: java.util.UUID? = null

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.isLoading) {
                        (binding.progressBar as? com.abhinav.otapulse.core.ui.WavyCircularProgressIndicator)?.indicatorColor = binding.buttonSubmit.currentTextColor ?: android.graphics.Color.WHITE
                        binding.progressBar.isVisible = true
                        binding.buttonSubmit.isEnabled = false
                        binding.buttonSubmit.text = ""
                    } else {
                        binding.progressBar.isVisible = false
                        binding.buttonSubmit.isEnabled = true
                        binding.buttonSubmit.text = "Check for Update"
                    }

                    if (state.result != null) {
                        state.result.onSuccess {
                            binding.errorCard.isVisible = false
                        }.onFailure { error ->
                            binding.errorCard.isVisible = true
                            binding.errorTextView.text = formatErrorMessage(error.message)
                        }
                    } else {
                        binding.errorCard.isVisible = false
                    }

                    if (state.showOtaDetailsDialog != null) {
                        showOtaDetailsDialog(state.showOtaDetailsDialog)
                        viewModel.clearOtaDetailsDialog()
                    }

                    binding.cardResolverResult.isVisible = state.resolverResult != null
                    state.resolverResult?.let { resolved ->
                        binding.tvResolverFilename.text = resolved.fileName ?: "Resolved OTA link"
                        binding.tvResolvedUrl.text = resolved.resolvedUrl
                    }

                    state.userMessage?.let { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        viewModel.clearUserMessage()
                    }
                }
            }
        }
    }

    private fun updateSelectedLocalZipSummary(uri: Uri) {
        val name = selectedLocalZipName.ifBlank { "Selected OTA ZIP" }
        val sizeText = resolveFileSize(uri)?.let { FormatUtils.formatSize(it) }?.let { " • $it" }.orEmpty()
        binding.tvLocalZipSummary.text = "$name$sizeText"
    }

    private fun resolveDisplayName(uri: Uri): String {
        requireContext().contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        return cursor.getString(index) ?: "Local OTA"
                    }
                }
            }
        return uri.lastPathSegment ?: "Local OTA"
    }

    private fun resolveFileSize(uri: Uri): Long? {
        requireContext().contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (index >= 0 && !cursor.isNull(index)) {
                        return cursor.getLong(index)
                    }
                }
            }
        return null
    }


    private fun guessVersionName(source: String): String {
        val cleaned = source.substringAfterLast('/').substringBefore('?')
        return cleaned.ifBlank { "Remote OTA" }
    }

    private fun showOtaDetailsDialog(ota: OtaUpdate) {
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

        val layoutPartitionSelector = dialogView.findViewById<View>(R.id.layoutPartitionSelector)
        val tvSelectedPartitionName = dialogView.findViewById<TextView>(R.id.tvSelectedPartitionName)
        val tvSelectedPartitionSize = dialogView.findViewById<TextView>(R.id.tvSelectedPartitionSize)
        val ivExpand = dialogView.findViewById<View>(R.id.ivExpand)
        val selectorProgress = dialogView.findViewById<View>(R.id.selectorProgress)
        val btnExtractSelected = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnExtractSelected)
        val btnExtractProgress = dialogView.findViewById<com.abhinav.otapulse.core.ui.WavyCircularProgressIndicator>(R.id.btnExtractProgress)
        val extractionProgressBar = dialogView.findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(R.id.extractionProgressBar)

        val deviceName = binding.inputProductModel.text.toString().trim()
        val regionName = binding.spinnerRegion.text.toString().trim()
        fun activePartitionSource(): String {
            val partitionData = viewModel.uiState.value.showPartitionSelectDialog
            return if (partitionData != null) partitionData.source else ota.url
        }
        fun activePartitionVersion(): String =
            viewModel.uiState.value.showPartitionSelectDialog?.versionName ?: (ota.versionName ?: "Custom")

        tvComponentName.text = "OTA Details: $deviceName"
        tvVersionName.text = ota.versionName ?: "Unknown Version"
        tvAndroidVersion.text = ota.realAndroidVersion ?: "Unknown"
        tvSecurityPatch.text = ota.securityPatch ?: "Unknown"
        val arbStatusText = ota.arbStatus ?: "N/A"
        tvArbStatus.text = arbStatusText
        if (arbStatusText.equals("Safe", ignoreCase = true)) {
            tvArbStatus.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
        } else if (arbStatusText.contains("Protected", ignoreCase = true)) {
            tvArbStatus.setTextColor(android.graphics.Color.parseColor("#B00020"))
        }

        tvMd5.text = ota.md5.ifBlank { "N/A" }
        tvMd5.setTextColor(tvAndroidVersion.currentTextColor)

        tvSize.text = ota.size

        // ── Shared helpers ────────────────────────────────────────────────────




        // ── Partition progress observer ───────────────────────────────────────

        fun resetExtractButton() {
            btnExtractSelected.isEnabled = selectedPartition != null
            btnExtractSelected.text = "Extract"
            btnExtractSelected.icon = null
            extractionProgressBar.isVisible = false
        }

        fun isShowingCancelState(): Boolean = activeExtractionWorkId != null &&
            (extractionProgressBar.isVisible || viewModel.uiState.value.isStartingExtraction)

        fun showCancelState(progress: Int? = null, indeterminate: Boolean = false) {
            btnExtractSelected.isEnabled = true
            btnExtractSelected.text = if (progress != null && progress in 0..99) {
                "$progress%"
            } else {
                ""
            }
            btnExtractSelected.setIconResource(R.drawable.ic_cancel_circle)
            extractionProgressBar.isIndeterminate = indeterminate
            extractionProgressBar.isVisible = true
            if (progress != null) {
                extractionProgressBar.progress = progress
            }
        }

        suspend fun observePartitionProgress(item: PartitionInfo, workId: java.util.UUID) {
            androidx.work.WorkManager.getInstance(requireContext())
                .getWorkInfoByIdFlow(workId)
                .collect { info ->

                    if (info != null) {
                        viewModel.clearStartingExtraction()

                        if (info.state == androidx.work.WorkInfo.State.SUCCEEDED) {
                            activeExtractionWorkId = null
                            resetExtractButton()
                            Toast.makeText(requireContext(), "${item.name}.img extracted successfully", Toast.LENGTH_SHORT).show()
                            workInfoJob?.cancel()
                            return@collect
                        } else if (info.state == androidx.work.WorkInfo.State.CANCELLED) {
                            activeExtractionWorkId = null
                            resetExtractButton()
                            Toast.makeText(requireContext(), "Extraction cancelled", Toast.LENGTH_SHORT).show()
                            workInfoJob?.cancel()
                            return@collect
                        } else if (info.state == androidx.work.WorkInfo.State.FAILED) {
                            activeExtractionWorkId = null
                            resetExtractButton()
                            Toast.makeText(requireContext(), "Extraction failed", Toast.LENGTH_SHORT).show()
                            workInfoJob?.cancel()
                            return@collect
                        }

                        val progress = info.progress.getInt(com.abhinav.otapulse.arb.worker.PartitionExtractorWorker.PROGRESS_KEY, -1)
                        if (progress != -1) {
                            if (progress < 100) {
                                showCancelState(progress = progress, indeterminate = false)
                            } else {
                                resetExtractButton()
                            }
                        } else if (!info.state.isFinished) {
                            showCancelState(indeterminate = true)
                        }
                    }
                }
        }

        // ── Partition popup ───────────────────────────────────────────────────

        fun showPartitionPopup(partitions: List<PartitionInfo>) {
            val bottomSheet = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
            val sheetView = layoutInflater.inflate(R.layout.dialog_partition_list, null)
            val rv = sheetView.findViewById<RecyclerView>(R.id.rvPartitions)

            // Header
            sheetView.findViewById<TextView>(R.id.tvTitle).text = "Select Partition"
            sheetView.findViewById<TextView>(R.id.tvCount).text = partitions.size.toString()

            // Search with mutable display list
            var displayedPartitions = partitions.toMutableList()
            val etSearch = sheetView.findViewById<android.widget.EditText>(R.id.etSearch)

            rv.layoutManager = LinearLayoutManager(requireContext())
            rv.setHasFixedSize(false)
            rv.setItemViewCacheSize(20)

            val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                override fun getItemCount() = displayedPartitions.size
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                    val v = layoutInflater.inflate(R.layout.item_partition_popup, parent, false)
                    return object : RecyclerView.ViewHolder(v) {}
                }
                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    val item = displayedPartitions[position]
                    val v = holder.itemView

                    val tvName        = v.findViewById<TextView>(R.id.tvPartitionName)
                    val tvMetadata    = v.findViewById<TextView>(R.id.tvPartitionMetadata)
                    val btnExtractRow = v.findViewById<View>(R.id.btnExtractRow)
                    val card          = v.findViewById<View>(R.id.cardPartition)
                    val tvSizeTag     = v.findViewById<TextView>(R.id.tvSizeTag)
                    val viewAccent    = v.findViewById<View>(R.id.viewAccent)

                    // Text
                    tvName.text = item.name
                    tvMetadata.text = "${item.formattedSize} • ${item.opCount} ops"

                    // Size tag chip
                    tvSizeTag.text = FormatUtils.getSizeCategory(item.sizeBytes)
                    tvSizeTag.backgroundTintList = ColorStateList.valueOf(FormatUtils.getSizeColor(item.sizeBytes))
                    tvSizeTag.visibility = View.VISIBLE

                    // Left accent bar
                    viewAccent.backgroundTintList = ColorStateList.valueOf(FormatUtils.getSizeColor(item.sizeBytes))

                    // Extract button click
                    btnExtractRow.setOnClickListener {
                        if (checkAndRequestPermissions()) {
                            selectedPartition = item
                            tvSelectedPartitionName.text = item.name
                            tvSelectedPartitionSize.text = item.formattedSize
                            btnExtractSelected.isEnabled = true
                            activeExtractionWorkId = viewModel.extractPartition(
                                activePartitionSource(),
                                activePartitionVersion(),
                                item.name,
                                regionName
                            )
                            workInfoJob?.cancel()
                            workInfoJob = viewLifecycleOwner.lifecycleScope.launch {
                                observePartitionProgress(item, activeExtractionWorkId ?: return@launch)
                            }
                            bottomSheet.dismiss()
                        }
                    }

                    // Card click: select only
                    card.setOnClickListener {
                        selectedPartition = item
                        tvSelectedPartitionName.text = item.name
                        tvSelectedPartitionSize.text = item.formattedSize
                        btnExtractSelected.isEnabled = true
                        bottomSheet.dismiss()
                        workInfoJob?.cancel()
                        activeExtractionWorkId?.let { workId ->
                            workInfoJob = viewLifecycleOwner.lifecycleScope.launch {
                                observePartitionProgress(item, workId)
                            }
                        }
                    }
                }
            }

            rv.adapter = adapter

            // Wire search
            etSearch.doOnTextChanged { text, _, _, _ ->
                val query = text?.toString()?.trim().orEmpty()
                displayedPartitions = if (query.isEmpty()) {
                    partitions.toMutableList()
                } else {
                    partitions.filter { it.name.contains(query, ignoreCase = true) }.toMutableList()
                }
                adapter.notifyDataSetChanged()
            }

            bottomSheet.setContentView(sheetView)
            bottomSheet.behavior.apply {
                state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
                skipCollapsed = true
                peekHeight = (resources.displayMetrics.heightPixels * 0.70).toInt()
            }
            bottomSheet.show()
        }

        val dialog = MaterialAlertDialogBuilder(requireContext()).setView(dialogView).show()
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

        layoutPartitionSelector.setHapticClickListener {
            val currentState = viewModel.uiState.value
            if (currentState.showPartitionSelectDialog != null) {
                showPartitionPopup(currentState.showPartitionSelectDialog.partitions.sortedBy { it.name })
            } else {
                popupAlreadyShownForData = false
                viewModel.fetchExtractablePartitions(ota)
            }
        }

        btnExtractSelected.setHapticClickListener {
            val partition = selectedPartition ?: return@setHapticClickListener
            if (isShowingCancelState()) {
                activeExtractionWorkId?.let { viewModel.cancelPartitionExtraction(it, partition.name) }
            } else if (checkAndRequestPermissions()) {
                activeExtractionWorkId = viewModel.extractPartition(
                    activePartitionSource(),
                    activePartitionVersion(),
                    partition.name,
                    regionName
                )
                workInfoJob?.cancel()
                workInfoJob = viewLifecycleOwner.lifecycleScope.launch {
                    observePartitionProgress(partition, activeExtractionWorkId ?: return@launch)
                }
            }
        }

        val stateJob = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                selectorProgress.isVisible = state.isFetchingPartitions
                ivExpand.isVisible = !state.isFetchingPartitions
                state.showPartitionSelectDialog?.let { partitionData ->
                    if (!popupAlreadyShownForData && !state.isFetchingPartitions) {
                        popupAlreadyShownForData = true
                        showPartitionPopup(partitionData.partitions.sortedBy { it.name })
                    }
                }
                btnExtractProgress.isVisible = state.isStartingExtraction
                if (state.isStartingExtraction) {
                    btnExtractSelected.text = ""
                    btnExtractSelected.icon = null
                    btnExtractSelected.isEnabled = false
                } else if (btnExtractSelected.text.isEmpty()) {
                    resetExtractButton()
                }
            }
        }

        dialog.setOnDismissListener {
            stateJob.cancel()
            workInfoJob?.cancel()
            activeExtractionWorkId = null
            viewModel.clearPartitionSelectDialog()
        }

        btnDownloadOta.setHapticClickListener {
            viewModel.startDownload(ota, deviceName, regionName)
            dialog.dismiss()
        }
        btnCopyLink.setHapticClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("OTA Link", ota.url))
            Toast.makeText(requireContext(), "Link copied", Toast.LENGTH_SHORT).show()
        }
        btnChangelog.setHapticClickListener {
            ota.panelUrl?.let { url ->
                    try { openInAppBrowser(url, getString(R.string.ota_details_title)) } catch (_: Exception) {}
            }
        }
        btnShare.setHapticClickListener {
            val shareText = """
                🚀 𝗢𝗧𝗔 𝗣𝘂𝗹𝘀𝗲 | 𝗨𝗽𝗱𝗮𝘁𝗲 𝗔𝗹𝗲𝗿𝘁

                • 𝗩𝗲𝗿: ${ota.versionName ?: "Unknown"}
                • 𝗥𝗲𝗴𝗶𝗼𝗻: $regionName
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
            startActivity(Intent.createChooser(shareIntent, "Share OTA Update for $deviceName"))
        }

        btnViewJsonTop.setHapticClickListener {
            if (ota.rawJson.isNullOrBlank()) {
                Toast.makeText(requireContext(), getString(R.string.json_output_unavailable), Toast.LENGTH_SHORT).show()
            } else {
                startActivity(JsonOutputActivity.createIntent(requireContext(), ota, regionName))
            }
        }
    }

    private fun checkAndRequestPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!android.os.Environment.isExternalStorageManager()) {
                requestManageAllFilesAccess()
                return false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissionHelper.hasNotificationPermission()) {
                requestPermissionsLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
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

    private fun formatErrorMessage(msg: String?): String {
        return when {
            msg == null -> "Unknown Error"
            msg.contains("2004") -> "No update found."
            msg.contains("resolve") -> "Network error."
            else -> msg
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
