package com.abhinav.otapulse.feature.devices.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.abhinav.otapulse.R
import com.abhinav.otapulse.databinding.FragmentDevicesBinding
import com.abhinav.otapulse.core.common.FormatUtils
import com.abhinav.otapulse.core.model.Device
import com.abhinav.otapulse.core.model.OtaUpdate
import com.abhinav.otapulse.core.model.RegionVariant
import com.abhinav.otapulse.ota.payload.PartitionInfo
import com.abhinav.otapulse.core.common.PermissionHelper
import com.abhinav.otapulse.core.common.openInAppBrowser
import com.abhinav.otapulse.core.common.toPredefined
import com.abhinav.otapulse.core.common.toFullRegionName
import com.abhinav.otapulse.core.common.performHapticFeedback
import com.abhinav.otapulse.core.common.setHapticClickListener
import com.abhinav.otapulse.core.ui.applyBackgroundBlur
import com.abhinav.otapulse.feature.otatools.ui.JsonOutputActivity
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DevicesFragment : Fragment(R.layout.fragment_devices) {

    private var _binding: FragmentDevicesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DevicesViewModel by viewModels()
    private lateinit var deviceAdapter: DeviceAdapter

    @Inject
    lateinit var permissionHelper: PermissionHelper

    private var pendingDownload: (() -> Unit)? = null

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            proceedWithPendingDownloadIfPermissionsGranted()
        } else {
            showPermissionDeniedSnackbar(getString(R.string.permission_denied_download_error))
            pendingDownload = null
        }
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            proceedWithPendingDownloadIfPermissionsGranted()
        } else {
            showPermissionDeniedSnackbar(getString(R.string.permission_denied_download_error))
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
                showPermissionDeniedSnackbar(getString(R.string.permission_denied_download_error))
                pendingDownload = null
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDevicesBinding.bind(view)

        setupRecyclerView()
        setupSearch()
        setupBrandTabs()
        setupClickListeners()
        observeViewModel()
        runEnterAnimation()
    }

    private fun runEnterAnimation() {
        val viewsToAnimate = listOfNotNull(
            binding.searchCardView.takeIf { it.isVisible },
            binding.tabCardView.takeIf { it.isVisible },
            binding.devicesRecyclerView
        )
        com.abhinav.otapulse.core.common.AnimationUtils.animateEntrance(viewsToAnimate)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDevices()
    }

    private fun setupRecyclerView() {
        deviceAdapter = DeviceAdapter(
            onFetchDetails = { device, variant -> viewModel.fetchOtaDetails(device, variant) },
            onDownload = { otaUpdate, device, variant -> handleDownload(otaUpdate, device, variant) },
            onToggleFavorite = { deviceName -> viewModel.toggleFavorite(deviceName) },
            onCopyLink = { url -> copyLinkToClipboard(url) },
            onViewChangelog = { url -> viewChangelog(url) },
            onDeleteCustomDevice = { device -> showDeleteConfirmationDialog(device) },
            onEditCustomDevice = { device ->
                val fragment = AddDeviceFragment().apply {
                    arguments = Bundle().apply {
                        putParcelable("device", device.toPredefined())
                    }
                }
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.nav_enter,
                        R.anim.nav_exit,
                        R.anim.nav_pop_enter,
                        R.anim.nav_pop_exit
                    )
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        )
        binding.devicesRecyclerView.apply {
            adapter = deviceAdapter
            layoutManager = LinearLayoutManager(context)
            itemAnimator = null
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    deviceAdapter.submitList(state.devices)
                    deviceAdapter.updateOtaDetails(state.otaDetails)

                    state.errorMessage?.let {
                        Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                        viewModel.clearErrorMessage()
                    }

                    state.userMessage?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                        viewModel.clearUserMessage()
                    }

                    if (state.pendingDownload != null) {
                        showOverwriteConfirmationDialog(state.pendingDownload)
                    }

                    if (state.showOtaDetailsDialog != null) {
                        showOtaDetailsDialog(state.showOtaDetailsDialog)
                        viewModel.clearOtaDetailsDialog()
                    }
                }
            }
        }
    }

    private fun showOtaDetailsDialog(data: OtaDetailsDialogData) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_ota_details, null)

        val tvComponentName = dialogView.findViewById<TextView>(R.id.tvComponentName)
        val tvVersionName = dialogView.findViewById<TextView>(R.id.tvVersionName)
        val tvAndroidVersion = dialogView.findViewById<TextView>(R.id.tvAndroidVersion)
        val tvSecurityPatch = dialogView.findViewById<TextView>(R.id.tvSecurityPatch)
        val tvSize = dialogView.findViewById<TextView>(R.id.tvSize)
        val tvArbStatus = dialogView.findViewById<TextView>(R.id.tvArbStatus)
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

        val fullRegionName = data.variant.displayName.toFullRegionName()
        tvComponentName.text = "${getString(R.string.ota_details_title)}: $fullRegionName"
        tvVersionName.text = data.otaUpdate.versionName
        tvAndroidVersion.text = data.otaUpdate.realAndroidVersion
        tvSecurityPatch.text = data.otaUpdate.securityPatch
        val arbStatusText = data.otaUpdate.arbStatus ?: "N/A"
        tvArbStatus.text = arbStatusText
        if (arbStatusText.equals("Safe", ignoreCase = true)) {
            tvArbStatus.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.arb_safe))
        } else if (arbStatusText.contains("Protected", ignoreCase = true)) {
            tvArbStatus.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.arb_protected))
        }

        tvMd5.text = data.otaUpdate.md5.ifBlank { "N/A" }
        tvMd5.setTextColor(tvAndroidVersion.currentTextColor)

        tvSize.text = data.otaUpdate.size

        var selectedPartition: PartitionInfo? = null
        var popupAlreadyShownForData = false
        var workInfoJob: kotlinx.coroutines.Job? = null
        var activeExtractionWorkId: java.util.UUID? = null

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

        fun showPartitionPopup(partitions: List<PartitionInfo>) {
            val bottomSheet = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
            val sheetView = layoutInflater.inflate(R.layout.dialog_partition_list, null)
            val rv = sheetView.findViewById<RecyclerView>(R.id.rvPartitions)

            // ── Header: title text + count badge ──────────────────────────────
            sheetView.findViewById<TextView>(R.id.tvTitle).text = "Select Partition"
            sheetView.findViewById<TextView>(R.id.tvCount).text = partitions.size.toString()

            // ── Search: mutable display list updated on query change ──────────
            var displayedPartitions = partitions.toMutableList()

            val etSearch = sheetView.findViewById<android.widget.EditText>(R.id.etSearch)

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
                    val btnExtractRow = v.findViewById<View>(R.id.btnExtractRow)
                    val card       = v.findViewById<View>(R.id.cardPartition)
                    val tvSizeTag  = v.findViewById<TextView>(R.id.tvSizeTag)
                    val viewAccent = v.findViewById<View>(R.id.viewAccent)

                    // ── Text ──────────────────────────────────────────────────
                    tvName.text = item.name
                    tvMetadata.text = "${item.formattedSize} • ${item.opCount} ops"

                    // ── Size tag chip ─────────────────────────────────────────
                    tvSizeTag.text = FormatUtils.getSizeCategory(item.sizeBytes)
                    tvSizeTag.backgroundTintList = ColorStateList.valueOf(FormatUtils.getSizeColor(item.sizeBytes))
                    tvSizeTag.visibility = View.VISIBLE

                    // ── Left accent bar ───────────────────────────────────────
                    viewAccent.backgroundTintList = ColorStateList.valueOf(FormatUtils.getSizeColor(item.sizeBytes))

                    // ── Extract button click ──────────────────────────────────
                    btnExtractRow.setOnClickListener {
                        if (checkAndRequestPermissions()) {
                            val partitionData = viewModel.uiState.value.showPartitionSelectDialog
                            if (partitionData != null) {
                                selectedPartition = item
                                tvSelectedPartitionName.text = item.name
                                tvSelectedPartitionSize.text = item.formattedSize
                                btnExtractSelected.isEnabled = true
                                activeExtractionWorkId = viewModel.extractPartition(
                                    url = partitionData.url,
                                    versionName = partitionData.versionName,
                                    partitionName = item.name,
                                    regionName = data.variant.displayName
                                )
                                workInfoJob?.cancel()
                                workInfoJob = viewLifecycleOwner.lifecycleScope.launch {
                                    observePartitionProgress(item, activeExtractionWorkId ?: return@launch)
                                }
                                bottomSheet.dismiss()
                            }
                        }
                    }

                    // ── Card click: select only ───────────────────────────────
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

            // ── Wire search ───────────────────────────────────────────────────
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
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        layoutPartitionSelector.setHapticClickListener {
            val currentState = viewModel.uiState.value
            if (currentState.showPartitionSelectDialog != null) {
                showPartitionPopup(currentState.showPartitionSelectDialog.partitions.sortedBy { it.name })
            } else {
                popupAlreadyShownForData = false
                viewModel.fetchExtractablePartitions(data.otaUpdate)
            }
        }

        btnExtractSelected.setHapticClickListener {
            val partition = selectedPartition ?: return@setHapticClickListener
            val currentState = viewModel.uiState.value
            val partitionData = currentState.showPartitionSelectDialog
            if (isShowingCancelState()) {
                activeExtractionWorkId?.let { viewModel.cancelPartitionExtraction(it, partition.name) }
            } else if (partitionData != null && checkAndRequestPermissions()) {
                activeExtractionWorkId = viewModel.extractPartition(
                    url = partitionData.url,
                    versionName = partitionData.versionName,
                    partitionName = partition.name,
                    regionName = data.variant.displayName
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
            handleDownload(data.otaUpdate, data.device, data.variant)
            dialog.dismiss()
        }
        btnCopyLink.setHapticClickListener { copyLinkToClipboard(data.otaUpdate.url) }
        btnChangelog.setHapticClickListener { viewChangelog(data.otaUpdate.panelUrl ?: "") }
        btnViewJsonTop.setHapticClickListener {
            if (data.otaUpdate.rawJson.isNullOrBlank()) {
                Toast.makeText(requireContext(), getString(R.string.json_output_unavailable), Toast.LENGTH_SHORT).show()
            } else {
                startActivity(JsonOutputActivity.createIntent(requireContext(), data.otaUpdate, data.variant.displayName))
            }
        }
        btnShare.setHapticClickListener {
            val shareText = """
                🚀 𝗢𝗧𝗔 𝗣𝘂𝗹𝘀𝗲 | 𝗨𝗽𝗱𝗮𝘁𝗲 𝗔𝗹𝗲𝗿𝘁

                • 𝗩𝗲𝗿: ${data.otaUpdate.versionName}
                • 𝗥𝗲𝗴𝗶𝗼𝗻: ${data.variant.displayName.toFullRegionName()}
                • 𝗔𝗻𝗱𝗿𝗼𝗶𝗱: ${data.otaUpdate.realAndroidVersion}
                • 𝗦𝗲𝗰𝘂𝗿𝗶𝘁𝘆 𝗣𝗮𝘁𝗰𝗵: ${data.otaUpdate.securityPatch}
                • 𝗦𝗶𝘇𝗲: ${data.otaUpdate.size}
                • 𝗔𝗥𝗕 𝗦𝘁𝗮𝘁𝘂𝘀: ${data.otaUpdate.arbStatus ?: "N/A"}

                ━━━━━━━━━━━━━━━━━
                • 𝗖𝗵𝗮𝗻𝗴𝗲𝗹𝗼𝗴: ${data.otaUpdate.panelUrl ?: "Not available"}

                ━━━━━━━━━━━━━━━━━
                • 𝗗𝗼𝘄𝗻𝗹𝗼𝗮𝗱: ${data.otaUpdate.url}

                ━━━━━━━━━━━━━━━━━
                • @abhinav_v1
            """.trimIndent()
            val sendIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            startActivity(android.content.Intent.createChooser(sendIntent, "Share OTA Update for ${data.device.name}"))
        }
    }


    private fun showOverwriteConfirmationDialog(pending: PendingDownload) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_overwrite_download, null)
        dialogView.findViewById<TextView>(R.id.textFileName).text = pending.targetFile.name

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setOnCancelListener {
                viewModel.cancelPendingDownload()
            }
            .show()

        dialog.applyBackgroundBlur()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94f).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialogView.findViewById<View>(R.id.buttonCancel).setHapticClickListener {
            viewModel.cancelPendingDownload()
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.buttonConfirm).setHapticClickListener {
            viewModel.confirmOverwriteDownload()
            dialog.dismiss()
        }
    }

    private fun setupSearch() {
        binding.searchEditText.doOnTextChanged { text, _, _, _ ->
            viewModel.onSearchQueryChanged(text.toString())
        }
    }

    private fun setupBrandTabs() {
        val tabTitles = listOf("All", "OnePlus", "Realme", "OPPO")
        val layoutInflater = LayoutInflater.from(requireContext())
        binding.brandTabLayout.apply {
            tabMode = TabLayout.MODE_FIXED
            tabGravity = TabLayout.GRAVITY_FILL

            tabTitles.forEach { title ->
                val tab = newTab()
                val customTabView = layoutInflater.inflate(R.layout.custom_tab_item, this, false)
                customTabView.findViewById<TextView>(R.id.tab_title).text = title
                tab.customView = customTabView
                addTab(tab)
            }
        }

        binding.brandTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.customView?.performHapticFeedback()
                val customTabView = tab?.customView
                val text = customTabView?.findViewById<TextView>(R.id.tab_title)?.text.toString()
                viewModel.onBrandSelected(text)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupClickListeners() {
        binding.searchIcon.setHapticClickListener {
            binding.searchCardView.isVisible = true
            binding.tabCardView.isVisible = false
            binding.searchEditText.requestFocus()
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.searchEditText, InputMethodManager.SHOW_IMPLICIT)
        }

        binding.closeSearchIcon.setHapticClickListener {
            binding.searchCardView.isVisible = false
            binding.tabCardView.isVisible = true
            binding.searchEditText.setText("")
            viewModel.onSearchQueryChanged("")
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
        }

        binding.addDeviceFab.setHapticClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.nav_enter,
                    R.anim.nav_exit,
                    R.anim.nav_pop_enter,
                    R.anim.nav_pop_exit
                )
                .replace(R.id.fragment_container, AddDeviceFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun handleDownload(otaUpdate: OtaUpdate, device: Device, variant: RegionVariant) {
        pendingDownload = {
            viewModel.startDownload(otaUpdate, device, variant)
            Toast.makeText(requireContext(), getString(R.string.starting_download_for, otaUpdate.versionName), Toast.LENGTH_SHORT).show()
        }

        if (checkAndRequestPermissions()) {
            pendingDownload?.invoke()
            pendingDownload = null
        }
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

        val permissionsToRequest = mutableListOf<String>()
        if (!permissionHelper.hasStoragePermission()) {
            permissionsToRequest.addAll(permissionHelper.getRequiredStoragePermissions())
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
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

        showNotificationPermissionSettingsDialog()
    }

    private fun requestManageAllFilesAccess() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.permission_needed))
            .setMessage(getString(R.string.broad_file_access_prompt))
            .setPositiveButton(getString(R.string.settings)) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:${requireContext().packageName}")
                    manageStoragePermissionLauncher.launch(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    manageStoragePermissionLauncher.launch(intent)
                }
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                pendingDownload = null
            }
            .show()
    }

    private fun proceedWithPendingDownloadIfPermissionsGranted() {
        if (pendingDownload != null) {
            if (permissionHelper.hasNotificationPermission() && permissionHelper.hasStoragePermission()) {
                pendingDownload?.invoke()
            } else {
                showPermissionDeniedSnackbar("Permissions missing.")
            }
            pendingDownload = null
        }
    }

    private fun showNotificationPermissionSettingsDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.notification_permission_title)
            .setMessage(R.string.notification_permission_settings_message)
            .setPositiveButton(R.string.settings) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.fromParts("package", requireContext().packageName, null)
                    startActivity(intent)
                } catch (_: Exception) {
                    Toast.makeText(requireContext(), getString(R.string.cannot_open_app_settings), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun copyLinkToClipboard(url: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Download Link", url))
        Snackbar.make(binding.root, getString(R.string.link_copied), Snackbar.LENGTH_SHORT).show()
    }

    private fun viewChangelog(url: String) {
        if (url.isNotEmpty()) {
            try {
                openInAppBrowser(url, getString(R.string.ota_details_title))
            } catch (_: Exception) {
                Toast.makeText(requireContext(), getString(R.string.changelog_error), Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), getString(R.string.changelog_not_available), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPermissionDeniedSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction(getString(R.string.settings)) {
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.fromParts("package", requireContext().packageName, null)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), getString(R.string.cannot_open_app_settings), Toast.LENGTH_SHORT).show()
                }
            }.show()
    }

    private fun showDeleteConfirmationDialog(device: Device) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_custom_device_title))
            .setMessage(getString(R.string.delete_custom_device_message, device.name))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteCustomDevice(device.name)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        pendingDownload = null
    }
}
