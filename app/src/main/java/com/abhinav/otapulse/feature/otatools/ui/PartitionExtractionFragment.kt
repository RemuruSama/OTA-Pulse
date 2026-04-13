package com.abhinav.otapulse.feature.otatools.ui

import android.Manifest
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
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.abhinav.otapulse.R
import com.abhinav.otapulse.databinding.FragmentPartitionExtractionBinding
import com.abhinav.otapulse.ota.payload.PartitionInfo
import com.abhinav.otapulse.core.common.FormatUtils
import com.abhinav.otapulse.core.common.PermissionHelper
import com.abhinav.otapulse.core.common.setHapticClickListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PartitionExtractionFragment : Fragment() {

    private var _binding: FragmentPartitionExtractionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OtaToolsViewModel by viewModels()

    @Inject
    lateinit var permissionHelper: PermissionHelper

    private var selectedLocalZipUri: Uri? = null
    private var selectedLocalZipName: String = ""
    private var activeExtractionWorkId: java.util.UUID? = null
    private var workInfoJob: Job? = null
    private var selectedPartition: PartitionInfo? = null
    private var wasFetchingPartitions = false
    private var suppressSourceWatcher = false

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(requireContext(), getString(R.string.notification_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    private val manageStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ -> }

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
        if (!binding.inputPartitionUrl.text.isNullOrBlank()) {
            suppressSourceWatcher = true
            binding.inputPartitionUrl.text?.clear()
            suppressSourceWatcher = false
        }
        updateSelectedLocalZipSummary(uri)
        updateSourceUiState()
        startPartitionLoad()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPartitionExtractionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupActions()
        observeState()
        runEnterAnimation()
    }

    private fun setupActions() {
        binding.btnPickLocalZip.setHapticClickListener {
            pickLocalZipLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
        }

        binding.btnRemoveLocalZip.setHapticClickListener {
            selectedLocalZipUri = null
            selectedLocalZipName = ""
            updateSelectedLocalZipSummary(null)
            updateSourceUiState()
            
            // Re-evaluate if there's a URL to load or reset state
            val urlSource = binding.inputPartitionUrl.text?.toString()?.trim().orEmpty()
            if (urlSource.isNotBlank()) {
                startPartitionLoad()
            } else {
                viewModel.clearPartitionSelectDialog()
                selectedPartition = null
                resetExtractButton()
                binding.tvSelectedPartitionName.text = "Select Partition"
                binding.tvSelectedPartitionSize.text = ""
            }
        }

        binding.layoutPartitionUrl.setEndIconOnClickListener {
            pasteFromClipboard()
        }

        binding.inputPartitionUrl.doOnTextChanged { text, _, _, _ ->
            if (!suppressSourceWatcher && !text.isNullOrBlank() && selectedLocalZipUri != null) {
                selectedLocalZipUri = null
                selectedLocalZipName = ""
                updateSelectedLocalZipSummary(null)
            }

            if (text.isNullOrBlank()) {
                binding.layoutPartitionUrl.endIconDrawable = androidx.core.content.ContextCompat.getDrawable(requireContext(), com.abhinav.otapulse.R.drawable.ic_paste_stroke)
                binding.layoutPartitionUrl.setEndIconOnClickListener {
                    pasteFromClipboard()
                }
            } else {
                binding.layoutPartitionUrl.endIconDrawable = androidx.core.content.ContextCompat.getDrawable(requireContext(), com.abhinav.otapulse.R.drawable.ic_close)
                binding.layoutPartitionUrl.setEndIconOnClickListener {
                    binding.inputPartitionUrl.text?.clear()
                }
            }

            updateSourceUiState()

            val partitionData = viewModel.uiState.value.showPartitionSelectDialog
            if (partitionData != null) {
                val currentText = text?.toString()?.trim().orEmpty()
                val isLocalZipLoading = selectedLocalZipUri != null && partitionData.source == selectedLocalZipUri.toString()
                
                if (!isLocalZipLoading && partitionData.source != currentText) {
                    viewModel.clearPartitionSelectDialog()
                    selectedPartition = null
                    resetExtractButton()
                    binding.tvSelectedPartitionName.text = "Select Partition"
                    binding.tvSelectedPartitionSize.text = ""
                }
            }
        }

        binding.inputPartitionUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO ||
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                binding.inputPartitionUrl.clearFocus()
                val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(binding.inputPartitionUrl.windowToken, 0)
                startPartitionLoad()
                true
            } else {
                false
            }
        }

        binding.layoutPartitionSelector.setHapticClickListener {
            val partitionData = viewModel.uiState.value.showPartitionSelectDialog
            if (partitionData != null) {
                showPartitionPopup(
                    partitions = partitionData.partitions.sortedBy { it.name },
                    source = partitionData.source,
                    versionName = partitionData.versionName
                )
            } else {
                startPartitionLoad()
            }
        }

        binding.btnExtractSelected.setHapticClickListener {
            handleExtractAction()
        }

        updateSourceUiState()
    }

    private fun pasteFromClipboard() {
        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val item = clipboard.primaryClip?.getItemAt(0)
        val pasteData = item?.text?.toString()
        if (!pasteData.isNullOrBlank()) {
            binding.inputPartitionUrl.setText(pasteData)
            Toast.makeText(requireContext(), "Link pasted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Clipboard is empty", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startPartitionLoad() {
        val urlSource = binding.inputPartitionUrl.text?.toString()?.trim().orEmpty()
        val localZip = selectedLocalZipUri

        when {
            urlSource.isNotBlank() -> {
                setLoadingState(true)
                viewModel.fetchExtractablePartitions(
                    source = urlSource,
                    versionName = guessVersionName(urlSource),
                    sourceLabel = "Full OTA URL"
                )
            }
            localZip != null -> {
                setLoadingState(true)
                viewModel.fetchExtractablePartitions(
                    source = localZip.toString(),
                    versionName = selectedLocalZipName.ifBlank { "Local OTA" },
                    sourceLabel = selectedLocalZipName.ifBlank { "Local OTA ZIP" }
                )
            }
            else -> {
                Toast.makeText(requireContext(), "Paste a full OTA URL or choose a local ZIP first", Toast.LENGTH_SHORT).show()
                return
            }
        }
    }

    private fun handleExtractAction() {
        val partition = selectedPartition ?: return
        val partitionData = viewModel.uiState.value.showPartitionSelectDialog ?: return
        
        if (isShowingCancelState()) {
            activeExtractionWorkId?.let { viewModel.cancelPartitionExtraction(it, partition.name) }
        } else if (checkAndRequestPermissions()) {
            activeExtractionWorkId = viewModel.extractPartition(
                partitionData.source,
                partitionData.versionName,
                partition.name
            )
            showCancelState(indeterminate = true)
            workInfoJob?.cancel()
            workInfoJob = viewLifecycleOwner.lifecycleScope.launch {
                observePartitionProgress(partition, activeExtractionWorkId ?: return@launch)
            }
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                val justFinishedFetching = wasFetchingPartitions && !state.isFetchingPartitions
                wasFetchingPartitions = state.isFetchingPartitions

                setLoadingState(state.isFetchingPartitions)

                if (justFinishedFetching && state.showPartitionSelectDialog != null) {
                    val data = state.showPartitionSelectDialog
                    showPartitionPopup(
                        partitions = data.partitions.sortedBy { it.name },
                        source = data.source,
                        versionName = data.versionName
                    )
                }

                binding.btnExtractProgress.isVisible = state.isStartingExtraction
                if (state.isStartingExtraction) {
                    binding.btnExtractSelected.text = ""
                    binding.btnExtractSelected.icon = null
                    binding.btnExtractSelected.isEnabled = false
                } else if (binding.btnExtractSelected.text.isEmpty()) {
                    resetExtractButton()
                }

                state.userMessage?.let { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    viewModel.clearUserMessage()
                }
            }
        }
    }

    private fun showPartitionPopup(
        partitions: List<PartitionInfo>,
        source: String,
        versionName: String
    ) {
        val bottomSheet = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.dialog_partition_list, null)
        val recyclerView = sheetView.findViewById<RecyclerView>(R.id.rvPartitions)
        val search = sheetView.findViewById<EditText>(R.id.etSearch)

        sheetView.findViewById<TextView>(R.id.tvTitle).text = "Select Partition"
        sheetView.findViewById<TextView>(R.id.tvCount).text = partitions.size.toString()

        var displayedPartitions = partitions.toMutableList()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun getItemCount() = displayedPartitions.size

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val itemView = layoutInflater.inflate(R.layout.item_partition_popup, parent, false)
                return object : RecyclerView.ViewHolder(itemView) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val partition = displayedPartitions[position]
                val itemView = holder.itemView
                itemView.findViewById<TextView>(R.id.tvPartitionName).text = partition.name
                itemView.findViewById<TextView>(R.id.tvPartitionMetadata).text =
                    "${partition.formattedSize} • ${partition.opCount} ops"

                val color = FormatUtils.getSizeColor(partition.sizeBytes)
                itemView.findViewById<TextView>(R.id.tvSizeTag).apply {
                    text = FormatUtils.getSizeCategory(partition.sizeBytes)
                    backgroundTintList = ColorStateList.valueOf(color)
                    isVisible = true
                }
                itemView.findViewById<View>(R.id.viewAccent).backgroundTintList = ColorStateList.valueOf(color)

                val selectPartitionAction = fun(startExtraction: Boolean) {
                    selectedPartition = partition
                    binding.tvSelectedPartitionName.text = partition.name
                    binding.tvSelectedPartitionSize.text = partition.formattedSize
                    binding.btnExtractSelected.isEnabled = true
                    bottomSheet.dismiss()
                    
                    workInfoJob?.cancel()
                    activeExtractionWorkId?.let { workId ->
                        workInfoJob = viewLifecycleOwner.lifecycleScope.launch {
                            observePartitionProgress(partition, workId)
                        }
                    }

                    if (startExtraction) {
                        handleExtractAction()
                    }
                }

                itemView.findViewById<View>(R.id.cardPartition).setOnClickListener { selectPartitionAction(false) }
                itemView.findViewById<View>(R.id.btnExtractRow).setOnClickListener { selectPartitionAction(true) }
            }
        }

        recyclerView.adapter = adapter
        search.doOnTextChanged { text, _, _, _ ->
            val query = text?.toString()?.trim().orEmpty()
            displayedPartitions = if (query.isBlank()) {
                partitions.toMutableList()
            } else {
                partitions.filter { it.name.contains(query, ignoreCase = true) }.toMutableList()
            }
            adapter.notifyDataSetChanged()
        }

        bottomSheet.setContentView(sheetView)
        bottomSheet.behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
            peekHeight = (resources.displayMetrics.heightPixels * 0.70).toInt()
        }
        bottomSheet.show()
    }    private fun isShowingCancelState(): Boolean = activeExtractionWorkId != null &&
        (binding.extractionProgressBar.isVisible || viewModel.uiState.value.isStartingExtraction)

    private fun showCancelState(progress: Int? = null, indeterminate: Boolean = false) {
        binding.btnExtractSelected.isEnabled = true
        binding.btnExtractSelected.text = if (progress != null && progress in 0..99) {
            "$progress%"
        } else {
            ""
        }
        binding.btnExtractSelected.setIconResource(R.drawable.ic_cancel_circle)
        binding.extractionProgressBar.isVisible = true
        binding.extractionProgressBar.isIndeterminate = indeterminate
        if (progress != null) {
            binding.extractionProgressBar.progress = progress
        }
    }

    private fun resetExtractButton() {
        binding.btnExtractSelected.isEnabled = selectedPartition != null
        binding.btnExtractSelected.text = "Extract"
        binding.btnExtractSelected.icon = null
        binding.extractionProgressBar.isVisible = false
    }

    private suspend fun observePartitionProgress(item: PartitionInfo, workId: java.util.UUID) {
        androidx.work.WorkManager.getInstance(requireContext())
            .getWorkInfoByIdFlow(workId)
            .collect { info ->
                if (info == null) return@collect
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

                val progress = info.progress.getInt(
                    com.abhinav.otapulse.arb.worker.PartitionExtractorWorker.PROGRESS_KEY,
                    -1
                )

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

    private fun setLoadingState(isLoading: Boolean) {
        binding.loadingIndicator.isVisible = isLoading
        binding.ivExpand.isVisible = !isLoading
        binding.layoutPartitionSelector.isEnabled = !isLoading
        
        if (isLoading) {
            binding.tvSelectedPartitionName.text = "Loading partitions..."
            binding.tvSelectedPartitionSize.text = ""
        } else {
            val partitionData = viewModel.uiState.value.showPartitionSelectDialog
            if (partitionData == null) {
                binding.tvSelectedPartitionName.text = "Select Partition"
            } else if (selectedPartition == null) {
                binding.tvSelectedPartitionName.text = "Select Partition"
            }
        }
    }

    private fun updateSourceUiState() {
        val hasUrl = !binding.inputPartitionUrl.text.isNullOrBlank()
        val hasLocalZip = selectedLocalZipUri != null

        val urlActive = hasUrl || !hasLocalZip
        val localActive = hasLocalZip || !hasUrl

        binding.layoutPartitionUrl.alpha = if (urlActive) 1f else 0.45f
        binding.cardLocalZip.alpha = if (localActive) 1f else 0.45f
        binding.tvLocalZipSummary.alpha = if (localActive) 1f else 0.7f
        binding.tvLocalZipSize.alpha = if (localActive) 1f else 0.7f

        binding.btnPickLocalZip.isEnabled = !hasUrl
        binding.btnRemoveLocalZip.isEnabled = !hasUrl
    }


    private fun checkAndRequestPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!android.os.Environment.isExternalStorageManager()) {
                requestManageAllFilesAccess()
                return false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !permissionHelper.hasNotificationPermission()
            ) {
                requestNotificationPermission()
                return false
            }
            return true
        }

        val permissions = mutableListOf<String>()
        if (!permissionHelper.hasStoragePermission()) {
            permissions.addAll(permissionHelper.getRequiredStoragePermissions())
        }
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

    private fun updateSelectedLocalZipSummary(uri: Uri?) {
        if (uri == null) {
            binding.tvLocalZipSummary.text = "No ZIP selected yet"
            binding.tvLocalZipSize.isVisible = false
            binding.btnRemoveLocalZip.isVisible = false
            return
        }
        val name = selectedLocalZipName.ifBlank { "Selected OTA ZIP" }
        binding.tvLocalZipSummary.text = name
        val sizeText = resolveFileSize(uri)?.let { FormatUtils.formatSize(it) }
        if (sizeText != null) {
            binding.tvLocalZipSize.text = sizeText
            binding.tvLocalZipSize.isVisible = true
        } else {
            binding.tvLocalZipSize.isVisible = false
        }
        binding.btnRemoveLocalZip.isVisible = true
    }

    private fun resolveDisplayName(uri: Uri): String {
        requireContext().contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
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
        requireContext().contentResolver.query(
            uri,
            arrayOf(OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
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

    private fun runEnterAnimation() {
        val views = listOf(
            binding.tvHeaderBadge,
            binding.tvHeaderTitle,
            binding.tvHeaderSubtitle,
            binding.cardHeroIcon,
            binding.layoutPartitionUrl,
            binding.partitionSelectorRow
        )
        com.abhinav.otapulse.core.common.AnimationUtils.animateEntrance(views)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        workInfoJob?.cancel()
        _binding = null
    }
}
