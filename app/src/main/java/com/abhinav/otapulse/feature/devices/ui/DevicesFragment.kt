package com.abhinav.otapulse.feature.devices.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.abhinav.otapulse.R
import com.abhinav.otapulse.databinding.FragmentDevicesBinding
import com.abhinav.otapulse.core.model.Device
import com.abhinav.otapulse.core.common.performHapticFeedback
import com.abhinav.otapulse.core.common.setHapticClickListener
import com.abhinav.otapulse.core.common.toPredefined
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.abhinav.otapulse.core.ui.applyBackgroundBlur
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DevicesFragment : Fragment(R.layout.fragment_devices) {

    private var _binding: FragmentDevicesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DevicesViewModel by activityViewModels()
    private lateinit var deviceAdapter: DeviceAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDevicesBinding.bind(view)

        setupRecyclerView()
        setupSwipeRefresh()
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
            binding.swipeRefreshLayout
        )
        com.abhinav.otapulse.core.common.AnimationUtils.animateEntrance(viewsToAnimate)
    }

    private fun setupSwipeRefresh() {
        val colorPrimary = com.google.android.material.color.MaterialColors.getColor(binding.root, androidx.appcompat.R.attr.colorPrimary)
        val colorSurface = com.google.android.material.color.MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorSurface)
        
        binding.swipeRefreshLayout.setProgressBackgroundColorSchemeColor(colorSurface)
        binding.swipeRefreshLayout.setColorSchemeColors(colorPrimary)

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.forceSyncCatalog()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDevices()
    }

    private fun setupRecyclerView() {
        deviceAdapter = DeviceAdapter(
            onFetchDetails = { device, variant -> viewModel.fetchOtaDetails(device, variant) },
            onToggleFavorite = { deviceName -> viewModel.toggleFavorite(deviceName) },
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
                    binding.swipeRefreshLayout.isRefreshing = state.isSyncingCatalog

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
    }
}
