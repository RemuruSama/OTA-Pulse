package com.abhinav.otapulse.ui.devices

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.abhinav.otapulse.R
import com.abhinav.otapulse.databinding.FragmentDevicesBinding
import com.abhinav.otapulse.domain.model.Device
import com.abhinav.otapulse.domain.model.OtaUpdate
import com.abhinav.otapulse.domain.model.RegionVariant
import com.abhinav.otapulse.util.PermissionHelper
import com.abhinav.otapulse.util.toPredefined
import com.abhinav.otapulse.util.performHapticFeedback
import com.abhinav.otapulse.util.setHapticClickListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
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

    private val manageStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (android.os.Environment.isExternalStorageManager()) {
                proceedWithPendingDownloadIfPermissionsGranted()
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
        com.abhinav.otapulse.util.AnimationUtils.animateEntrance(viewsToAnimate)
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
                }
            }
        }
    }

    private fun showOverwriteConfirmationDialog(pending: PendingDownload) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.file_already_exists))
            .setMessage(getString(R.string.file_exists_message, pending.targetFile.name))
            .setPositiveButton(getString(R.string.delete_and_download)) { _, _ ->
                viewModel.confirmOverwriteDownload()
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                viewModel.cancelPendingDownload()
            }
            .setOnCancelListener {
                viewModel.cancelPendingDownload()
            }
            .show()
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

        binding.addDeviceFab?.setHapticClickListener {
            parentFragmentManager.beginTransaction()
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
        // Android 11+ (API 30+) - All Files Access
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!android.os.Environment.isExternalStorageManager()) {
                requestManageAllFilesAccess()
                return false
            }
            // Notification permission still required for Tiramisu+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissionHelper.hasNotificationPermission()) {
                requestPermissionsLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                return false
            }
            return true
        }

        // Legacy (API < 30)
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

    private fun copyLinkToClipboard(url: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Download Link", url))
        Snackbar.make(binding.root, getString(R.string.link_copied), Snackbar.LENGTH_SHORT).show()
    }

    private fun viewChangelog(url: String) {
        if (url.isNotEmpty()) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
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