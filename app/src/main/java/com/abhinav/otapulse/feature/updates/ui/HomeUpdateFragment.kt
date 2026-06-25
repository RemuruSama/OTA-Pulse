package com.abhinav.otapulse.feature.updates.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.abhinav.otapulse.R
import com.abhinav.otapulse.catalog.model.RegionData
import com.abhinav.otapulse.core.common.DeviceUtils
import com.abhinav.otapulse.core.common.FormatUtils
import com.abhinav.otapulse.core.common.PermissionHelper
import com.abhinav.otapulse.core.common.openInAppBrowser
import com.abhinav.otapulse.core.common.setHapticClickListener
import com.abhinav.otapulse.core.common.OtaCardData
import com.abhinav.otapulse.core.common.OtaShareHelper
import com.abhinav.otapulse.core.model.OtaUpdate
import com.abhinav.otapulse.core.ui.applyBackgroundBlur
import com.abhinav.otapulse.databinding.FragmentHomeUpdateBinding
import com.abhinav.otapulse.feature.otatools.ui.JsonOutputActivity
import com.abhinav.otapulse.feature.otatools.ui.OtaToolsViewModel
import com.abhinav.otapulse.ota.payload.PartitionInfo
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
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
        binding.tvPanelName.text = name.ifBlank { model.ifBlank { getString(R.string.unknown) } }
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
            val baseOtaVersion = getBaseOtaString(modelInput)

            if (modelInput.isBlank() || apiModelParam.isBlank() || region.isBlank()) {
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

            val letters = listOf("A", "C", "F", "H", "J")

            viewModel.sendRequestAcrossVersionsAndServers(
                model = apiModelParam,
                displayDeviceName = nameInput.ifBlank { modelInput },
                baseOtaVersion = baseOtaVersion,
                ruiVersion = 4,
                region = region,
                servers = customSearchOrder,
                letters = letters,
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
                        binding.layoutMultiUpdatesContainer.isVisible = false
                    } else {
                        binding.progressBar.isVisible = false
                        binding.buttonSubmit.isEnabled = true
                        binding.buttonSubmit.text = getString(R.string.btn_check_for_update)
                    }

                    if (state.multiResults != null && state.multiResults.isNotEmpty()) {
                        binding.errorCard.isVisible = false
                        binding.dividerUpdate.isVisible = true
                        binding.tvOtaFoundLabel.isVisible = true
                        binding.layoutMultiUpdatesContainer.isVisible = true
                        binding.layoutMultiUpdatesContainer.removeAllViews()

                        for (ota in state.multiResults) {
                            val itemView = layoutInflater.inflate(R.layout.item_home_update_result, binding.layoutMultiUpdatesContainer, false)
                            val tvUpdateVersionValue = itemView.findViewById<TextView>(R.id.tv_update_version_value)
                            
                            tvUpdateVersionValue.text = ota.versionName ?: getString(R.string.unknown_version)
                            itemView.setHapticClickListener {
                                showResultDialog(ota)
                            }
                            binding.layoutMultiUpdatesContainer.addView(itemView)
                        }
                    } else if (state.result?.isFailure == true) {
                        val error = state.result.exceptionOrNull()
                        binding.errorCard.isVisible = true
                        binding.errorTextView.text = formatErrorMessage(error?.message)
                        binding.dividerUpdate.isVisible = false
                        binding.tvOtaFoundLabel.isVisible = false
                        binding.layoutMultiUpdatesContainer.isVisible = false
                    } else {
                        binding.errorCard.isVisible = false
                        binding.dividerUpdate.isVisible = false
                        binding.tvOtaFoundLabel.isVisible = false
                        binding.layoutMultiUpdatesContainer.isVisible = false
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

        val layoutPartitionSelector = dialogView.findViewById<View>(R.id.layoutPartitionSelector)
        val tvSelectedPartitionName = dialogView.findViewById<TextView>(R.id.tvSelectedPartitionName)
        val tvSelectedPartitionSize = dialogView.findViewById<TextView>(R.id.tvSelectedPartitionSize)
        val ivExpand = dialogView.findViewById<View>(R.id.ivExpand)
        val selectorProgress = dialogView.findViewById<View>(R.id.selectorProgress)
        val btnExtractSelected = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnExtractSelected)
        val btnExtractProgress = dialogView.findViewById<com.abhinav.otapulse.core.ui.WavyCircularProgressIndicator>(R.id.btnExtractProgress)
        val extractionProgressBar = dialogView.findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(R.id.extractionProgressBar)

        val deviceLabel = binding.inputProductName.text?.toString()?.trim().orEmpty()
            .ifBlank { binding.inputProductModel.text?.toString()?.trim().orEmpty() }
            .ifBlank { getString(R.string.unknown) }
        val regionName = inferRegionFromNvId(binding.inputNvid.text?.toString().orEmpty())

        tvComponentName.text = getString(R.string.ota_details_device, deviceLabel)
        tvVersionName.text = ota.versionName ?: getString(R.string.unknown_version)
        tvAndroidVersion.text = ota.realAndroidVersion?.removePrefix("Android ")?.trim() ?: getString(R.string.unknown)
        tvSecurityPatch.text = ota.securityPatch ?: getString(R.string.unknown)

        val arbStatusText = ota.arbStatus ?: "N/A"
        tvArbStatus.text = arbStatusText
        if (arbStatusText.equals("Safe", ignoreCase = true)) {
            tvArbStatus.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.arb_safe))
        } else if (arbStatusText.contains("Protected", ignoreCase = true)) {
            tvArbStatus.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.arb_protected))
        }

        tvMd5.text = ota.md5.ifBlank { "N/A" }
        tvSize.text = ota.size

        var selectedPartitions: MutableSet<PartitionInfo> = mutableSetOf()
        var popupAlreadyShownForData = false
        var workInfoJob: kotlinx.coroutines.Job? = null
        var activeExtractionWorkId: java.util.UUID? = null

        fun resetExtractButton() {
            btnExtractSelected.isEnabled = selectedPartitions.isNotEmpty()
            btnExtractSelected.text = getString(R.string.extract)
            btnExtractSelected.icon = null
            extractionProgressBar.isVisible = false
        }

        fun updateSelectedPartitionUI() {
            if (selectedPartitions.isEmpty()) {
                tvSelectedPartitionName.text = getString(R.string.partition_extraction_select_partition)
                tvSelectedPartitionSize.text = ""
                resetExtractButton()
            } else {
                if (selectedPartitions.size == 1) {
                    tvSelectedPartitionName.text = selectedPartitions.first().name
                } else {
                    tvSelectedPartitionName.text = "${selectedPartitions.size} partitions selected"
                }
                val totalSize = selectedPartitions.sumOf { it.sizeBytes }
                tvSelectedPartitionSize.text = FormatUtils.formatSize(totalSize)
                btnExtractSelected.isEnabled = true
            }
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

        suspend fun observePartitionProgress(items: List<PartitionInfo>, workId: java.util.UUID) {
            val ctx = context ?: return
            androidx.work.WorkManager.getInstance(ctx)
                .getWorkInfoByIdFlow(workId)
                .collect { info ->
                    val innerCtx = context ?: return@collect
                    if (info != null) {
                        viewModel.clearStartingExtraction()

                        if (info.state == androidx.work.WorkInfo.State.SUCCEEDED) {
                            activeExtractionWorkId = null
                            resetExtractButton()
                            val msg = if (items.size == 1) "${items.first().name}.img extracted successfully" else "${items.size} partitions extracted"
                            Toast.makeText(innerCtx, msg, Toast.LENGTH_SHORT).show()
                            workInfoJob?.cancel()
                            return@collect
                        } else if (info.state == androidx.work.WorkInfo.State.CANCELLED) {
                            activeExtractionWorkId = null
                            resetExtractButton()
                            Toast.makeText(innerCtx, "Extraction cancelled", Toast.LENGTH_SHORT).show()
                            workInfoJob?.cancel()
                            return@collect
                        } else if (info.state == androidx.work.WorkInfo.State.FAILED) {
                            activeExtractionWorkId = null
                            resetExtractButton()
                            Toast.makeText(innerCtx, "Extraction failed", Toast.LENGTH_SHORT).show()
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

        fun showPartitionPopup(partitions: List<PartitionInfo>) {
            val bottomSheet = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
            val sheetView = layoutInflater.inflate(R.layout.dialog_partition_list, null)
            val rv = sheetView.findViewById<RecyclerView>(R.id.rvPartitions)

            sheetView.findViewById<TextView>(R.id.tvTitle).text = getString(R.string.partition_extraction_select_partition)
            sheetView.findViewById<TextView>(R.id.tvCount).text = partitions.size.toString()

            var displayedPartitions = partitions.toMutableList()
            val dialogSelected = mutableSetOf<PartitionInfo>().apply { addAll(selectedPartitions) }

            val etSearch = sheetView.findViewById<android.widget.EditText>(R.id.etSearch)
            val btnConfirm = sheetView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnConfirmSelection)

            val updateConfirmButton = {
                btnConfirm.text = getString(R.string.partition_extraction_confirm_selection, dialogSelected.size)
                btnConfirm.isEnabled = dialogSelected.isNotEmpty()
                val btnSelectAll = sheetView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectAll)
                if (dialogSelected.size == partitions.size) {
                    btnSelectAll.text = "Deselect All"
                    btnSelectAll.backgroundTintList = android.content.res.ColorStateList.valueOf(com.google.android.material.color.MaterialColors.getColor(btnSelectAll, com.google.android.material.R.attr.colorErrorContainer))
                    btnSelectAll.setTextColor(com.google.android.material.color.MaterialColors.getColor(btnSelectAll, com.google.android.material.R.attr.colorOnErrorContainer))
                } else {
                    btnSelectAll.text = "Select All"
                    btnSelectAll.backgroundTintList = android.content.res.ColorStateList.valueOf(com.google.android.material.color.MaterialColors.getColor(btnSelectAll, com.google.android.material.R.attr.colorSecondaryContainer))
                    btnSelectAll.setTextColor(com.google.android.material.color.MaterialColors.getColor(btnSelectAll, com.google.android.material.R.attr.colorOnSecondaryContainer))
                }
            }
            updateConfirmButton()

            btnConfirm.setOnClickListener {
                selectedPartitions.clear()
                selectedPartitions.addAll(dialogSelected)
                updateSelectedPartitionUI()
                bottomSheet.dismiss()
            }

            rv.layoutManager = LinearLayoutManager(requireContext())
            rv.setHasFixedSize(false)
            rv.setItemViewCacheSize(20)

            val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                override fun getItemCount() = displayedPartitions.size
                override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                    val v = layoutInflater.inflate(R.layout.item_partition_popup, parent, false)
                    return object : RecyclerView.ViewHolder(v) {}
                }
                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    val item = displayedPartitions[position]
                    val v = holder.itemView

                    val tvName     = v.findViewById<TextView>(R.id.tvPartitionName)
                    val tvMetadata = v.findViewById<TextView>(R.id.tvPartitionMetadata)
                    val card       = v.findViewById<View>(R.id.cardPartition)
                    val tvSizeTag  = v.findViewById<TextView>(R.id.tvSizeTag)
                    val viewAccent = v.findViewById<View>(R.id.viewAccent)

                    tvName.text = item.name
                    tvMetadata.text = "${item.formattedSize} • ${item.opCount} ops"

                    tvSizeTag.text = FormatUtils.getSizeCategory(item.sizeBytes)
                    tvSizeTag.backgroundTintList = ColorStateList.valueOf(FormatUtils.getSizeColor(item.sizeBytes))
                    tvSizeTag.visibility = View.VISIBLE

                    viewAccent.backgroundTintList = ColorStateList.valueOf(FormatUtils.getSizeColor(item.sizeBytes))

                    val cbSelect = v.findViewById<com.google.android.material.checkbox.MaterialCheckBox>(R.id.cbSelect)
                    cbSelect.isChecked = dialogSelected.contains(item)

                    val toggleSelection = {
                        if (dialogSelected.contains(item)) {
                            dialogSelected.remove(item)
                            cbSelect.isChecked = false
                        } else {
                            dialogSelected.add(item)
                            cbSelect.isChecked = true
                        }
                        updateConfirmButton()
                    }

                    card.setOnClickListener { toggleSelection() }
                }
            }

            rv.adapter = adapter
            
            val btnSelectAll = sheetView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectAll)
            btnSelectAll.setOnClickListener {
                if (dialogSelected.size == partitions.size) {
                    dialogSelected.clear()
                } else {
                    dialogSelected.clear()
                    dialogSelected.addAll(partitions)
                }
                updateConfirmButton()
                adapter.notifyDataSetChanged()
            }

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
            if (selectedPartitions.isEmpty()) return@setHapticClickListener
            val currentState = viewModel.uiState.value
            val partitionData = currentState.showPartitionSelectDialog
            if (isShowingCancelState()) {
                activeExtractionWorkId?.let { viewModel.cancelPartitionExtraction(it, selectedPartitions.joinToString("_") { p -> p.name }) }
            } else if (partitionData != null && checkAndRequestPermissions()) {
                activeExtractionWorkId = viewModel.extractPartitions(
                    source = partitionData.source,
                    versionName = partitionData.versionName,
                    partitionNames = selectedPartitions.map { it.name },
                    regionName = regionName
                )
                workInfoJob?.cancel()
                workInfoJob = viewLifecycleOwner.lifecycleScope.launch {
                    observePartitionProgress(selectedPartitions.toList(), activeExtractionWorkId ?: return@launch)
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
            pendingDownload = {
                viewModel.startDownload(ota, deviceLabel, regionName, isFromHomeUpdate = true)
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
            OtaShareHelper.shareOtaCard(
                requireContext(),
                OtaCardData(
                    deviceName = deviceLabel,
                    versionName = ota.versionName,
                    regionName = regionName,
                    androidVersion = ota.realAndroidVersion?.removePrefix("Android ")?.trim(),
                    securityPatch = ota.securityPatch,
                    size = ota.size,
                    arbStatus = ota.arbStatus,
                    md5 = ota.md5,
                    downloadUrl = ota.url,
                    changelogUrl = ota.panelUrl
                )
            )
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

    private fun getBaseOtaString(rawId: String): String {
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
        return "${cleanBase}_11"
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
