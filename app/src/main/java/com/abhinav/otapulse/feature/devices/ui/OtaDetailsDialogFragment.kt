package com.abhinav.otapulse.feature.devices.ui

import android.app.Dialog
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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.abhinav.otapulse.R
import com.abhinav.otapulse.core.common.DeviceUtils
import com.abhinav.otapulse.core.common.FormatUtils
import com.abhinav.otapulse.core.common.PermissionHelper
import com.abhinav.otapulse.core.common.openInAppBrowser
import com.abhinav.otapulse.core.common.setHapticClickListener
import com.abhinav.otapulse.core.common.toFullRegionName
import com.abhinav.otapulse.core.common.OtaCardData
import com.abhinav.otapulse.core.common.OtaShareHelper
import com.abhinav.otapulse.core.model.OtaUpdate
import com.abhinav.otapulse.feature.otatools.ui.JsonOutputActivity
import com.abhinav.otapulse.ota.payload.PartitionInfo
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.core.view.isVisible

class OtaDetailsDialogFragment : DialogFragment() {

    private val viewModel: DevicesViewModel by activityViewModels()
    private lateinit var permissionHelper: PermissionHelper
    private var pendingDownload: (() -> Unit)? = null
    private var data: OtaDetailsDialogData? = null

    // Permissions
    private val requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            pendingDownload?.invoke()
            pendingDownload = null
        } else {
            Toast.makeText(requireContext(), getString(R.string.permission_denied_download_error), Toast.LENGTH_SHORT).show()
        }
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (checkAndRequestPermissions()) {
                pendingDownload?.invoke()
                pendingDownload = null
            }
        } else {
            Toast.makeText(requireContext(), getString(R.string.permission_denied_download_error), Toast.LENGTH_SHORT).show()
        }
    }

    private val manageAllFilesResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && android.os.Environment.isExternalStorageManager()) {
            if (checkAndRequestPermissions()) {
                pendingDownload?.invoke()
                pendingDownload = null
            }
        } else {
            Toast.makeText(requireContext(), getString(R.string.permission_denied_download_error), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionHelper = PermissionHelper(requireContext())
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val uiState = viewModel.uiState.value
        data = uiState.showOtaDetailsDialog

        if (data == null) {
            return super.onCreateDialog(savedInstanceState)
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_ota_details, null)
        setupDialogView(dialogView, data!!)

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.Theme_App_Dialog)
            .setView(dialogView)
            .create()

        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setWindowAnimations(R.style.DialogFadeAnimation)
            val isLargeScreen = resources.configuration.smallestScreenWidthDp >= 600
            val maxDialogWidthPx = (720 * resources.displayMetrics.density).toInt()
            val targetDialogWidth = if (isLargeScreen) {
                minOf((resources.displayMetrics.widthPixels * 0.68f).toInt(), maxDialogWidthPx)
            } else {
                (resources.displayMetrics.widthPixels * 1.0f).toInt()
            }
            window.setLayout(
                targetDialogWidth,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun setupDialogView(dialogView: View, data: OtaDetailsDialogData) {
        val tvComponentName = dialogView.findViewById<TextView>(R.id.tvComponentName)
        val tvVersionName = dialogView.findViewById<TextView>(R.id.tvVersionName)
        val tvAndroidVersion = dialogView.findViewById<TextView>(R.id.tvAndroidVersion)
        val tvSecurityPatch = dialogView.findViewById<TextView>(R.id.tvSecurityPatch)
        val tvSize = dialogView.findViewById<TextView>(R.id.tvSize)
        val tvArbStatus = dialogView.findViewById<TextView>(R.id.tvArbStatus)
        val tvMd5 = dialogView.findViewById<TextView>(R.id.tvMd5)
        val tvNvId = dialogView.findViewById<TextView>(R.id.tvNvId)
        val tvSeparateSoft = dialogView.findViewById<TextView>(R.id.tvSeparateSoft)
        val tvPublishedTime = dialogView.findViewById<TextView>(R.id.tvPublishedTime)
        val tvTargetVersion = dialogView.findViewById<TextView>(R.id.tvTargetVersion)

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

        val productName = when {
            data.deviceName.equals("This Device", ignoreCase = true) || data.deviceName.equals("Custom Device", ignoreCase = true) -> {
                DeviceUtils.getDeviceName()
            }
            data.deviceName.startsWith("Custom|") -> {
                data.deviceName.removePrefix("Custom|").ifBlank { DeviceUtils.getDeviceName() }
            }
            else -> data.deviceName
        }
        tvComponentName.text = getString(R.string.ota_details_device, productName)
        tvVersionName.text = data.otaUpdate.versionName
        tvAndroidVersion.text = data.otaUpdate.realAndroidVersion?.removePrefix("Android ")?.trim()
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

        tvNvId.text = data.otaUpdate.nvId16?.ifBlank { null } ?: "N/A"
        tvSeparateSoft.text = data.otaUpdate.oplusSeparateSoft?.ifBlank { null } ?: "N/A"
        tvPublishedTime.text = FormatUtils.formatBuildDate(data.otaUpdate)
        tvTargetVersion.text = data.otaUpdate.otaTargetVersion?.ifBlank { null } ?: data.otaUpdate.realOtaVersion?.ifBlank { null } ?: "N/A"

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

            sheetView.findViewById<TextView>(R.id.tvTitle).text = "Select Partition"
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
            if (selectedPartitions.isEmpty()) return@setHapticClickListener
            val currentState = viewModel.uiState.value
            val partitionData = currentState.showPartitionSelectDialog
            if (isShowingCancelState()) {
                activeExtractionWorkId?.let { viewModel.cancelPartitionExtraction(it, selectedPartitions.joinToString("_") { p -> p.name }) }
            } else if (partitionData != null && checkAndRequestPermissions()) {
                activeExtractionWorkId = viewModel.extractPartitions(
                    url = partitionData.url,
                    versionName = partitionData.versionName,
                    partitionNames = selectedPartitions.map { it.name },
                    regionName = data.regionName
                )
                workInfoJob?.cancel()
                workInfoJob = lifecycleScope.launch {
                    observePartitionProgress(selectedPartitions.toList(), activeExtractionWorkId ?: return@launch)
                }
            }
        }

        lifecycleScope.launch {
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

        btnDownloadOta.setHapticClickListener {
            handleDownload(data.otaUpdate, productName, data.regionName)
            dismiss()
        }
        btnCopyLink.setHapticClickListener { copyLinkToClipboard(data.otaUpdate.url) }
        btnChangelog.setHapticClickListener { viewChangelog(data.otaUpdate.panelUrl ?: "") }
        btnViewJsonTop.setHapticClickListener {
            if (data.otaUpdate.rawJson.isNullOrBlank()) {
                Toast.makeText(requireContext(), getString(R.string.json_output_unavailable), Toast.LENGTH_SHORT).show()
            } else {
                startActivity(JsonOutputActivity.createIntent(requireContext(), data.otaUpdate, data.regionName))
            }
        }
        btnShare.setHapticClickListener {
            OtaShareHelper.shareOtaCard(
                requireContext(),
                OtaCardData(
                    deviceName = productName,
                    versionName = data.otaUpdate.versionName,
                    regionName = if (data.deviceName.equals("This Device", ignoreCase = true) || data.deviceName.equals("Custom Device", ignoreCase = true) || data.deviceName.startsWith("Custom|")) null else data.regionName.takeIf { it.isNotBlank() }?.toFullRegionName(),
                    androidVersion = data.otaUpdate.realAndroidVersion,
                    securityPatch = data.otaUpdate.securityPatch,
                    size = data.otaUpdate.size,
                    arbStatus = data.otaUpdate.arbStatus,
                    md5 = data.otaUpdate.md5,
                    downloadUrl = data.otaUpdate.url,
                    changelogUrl = data.otaUpdate.panelUrl,
                    nvId = data.otaUpdate.nvId16,
                    projectId = data.otaUpdate.oplusSeparateSoft,
                    buildDate = FormatUtils.formatBuildDate(data.otaUpdate),
                    targetVersion = data.otaUpdate.otaTargetVersion?.ifBlank { null } ?: data.otaUpdate.realOtaVersion?.ifBlank { null }
                )
            )
        }
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        viewModel.clearPartitionSelectDialog()
        viewModel.clearOtaDetailsDialog()
    }

    private fun handleDownload(otaUpdate: OtaUpdate, deviceName: String, regionName: String) {
        pendingDownload = {
            viewModel.startDownload(otaUpdate, deviceName, regionName)
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
        
        if (!permissionHelper.hasStoragePermission()) {
            requestStoragePermission()
            return false
        }
        return true
    }

    private fun requestManageAllFilesAccess() {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.parse("package:${requireContext().packageName}")
        }
        manageAllFilesResultLauncher.launch(intent)
    }

    private fun requestStoragePermission() {
        requestStoragePermissionLauncher.launch(arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ))
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun copyLinkToClipboard(url: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("OTA URL", url)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), R.string.link_copied, Toast.LENGTH_SHORT).show()
    }

    private fun viewChangelog(url: String) {
        if (url.isNotBlank()) {
            openInAppBrowser(url)
        } else {
            Toast.makeText(requireContext(), R.string.changelog_not_available, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val TAG = "OtaDetailsDialogFragment"
    }
}
