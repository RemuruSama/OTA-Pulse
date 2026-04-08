package com.abhinav.otapulse.feature.devices.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.BundleCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.abhinav.otapulse.R
import com.abhinav.otapulse.databinding.FragmentAddDeviceBinding
import com.abhinav.otapulse.catalog.model.RegionData
import com.abhinav.otapulse.core.common.performHapticFeedback
import com.abhinav.otapulse.core.common.setHapticClickListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddDeviceFragment : Fragment() {

    private var _binding: FragmentAddDeviceBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddDeviceViewModel by viewModels()
    private lateinit var groupAdapter: FirmwareGroupAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddDeviceBinding.inflate(inflater, container, false)
        
        // Handle Edit Mode
        arguments?.let { args ->
            BundleCompat.getParcelable(args, "device", com.abhinav.otapulse.catalog.model.PredefinedDevice::class.java)?.let { device ->
                viewModel.setEditDevice(device)
            }
        }
        
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        groupAdapter = FirmwareGroupAdapter(
            onAddVariantClicked = { androidVersion ->
                showAddVariantDialog(androidVersion)
            },
            onRemoveVariantClicked = { androidVersion, variant ->
                viewModel.removeVariantFromGroup(androidVersion, variant)
            },
            onEditGroupClicked = { androidVersion ->
                showEditGroupDialog(androidVersion)
            },
            onDeleteGroupClicked = { androidVersion ->
                showDeleteGroupConfirmationDialog(androidVersion)
            }
        )
        binding.groupsRecyclerView.apply {
            adapter = groupAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            it.performHapticFeedback()
            parentFragmentManager.popBackStack()
        }

        binding.buttonAddGroup.setHapticClickListener {
            showAddGroupDialog()
        }

        binding.buttonSaveDevice.setHapticClickListener {
            viewModel.saveDevice(
                name = binding.inputDeviceName.text.toString(),
                ruiVersion = viewModel.uiState.value.ruiVersion
            )
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    groupAdapter.submitList(state.firmwareGroups.toList())
                    binding.groupsRecyclerView.isVisible = state.firmwareGroups.isNotEmpty()
                    binding.emptyView.isVisible = state.firmwareGroups.isEmpty()

                    if (state.isEditMode) {
                        binding.toolbarTitle.text = "Edit Custom Device"
                        binding.buttonSaveDevice.text = "Update Device"
                        if (binding.inputDeviceName.text.isNullOrBlank()) {
                            binding.inputDeviceName.setText(state.deviceName)
                        }
                    } else {
                        binding.toolbarTitle.text = "Add Custom Device"
                        binding.buttonSaveDevice.text = "Save Device"
                    }

                    if (state.isSaveSuccess) {
                        val message = if (state.isEditMode) "Device updated successfully!" else "Device saved successfully!"
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                        viewModel.resetSaveSuccess()
                    }

                    state.errorMessage?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    private fun showAddGroupDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_group, null)
        val input = dialogView.findViewById<TextInputEditText>(R.id.inputAndroidVersion)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Firmware Group")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Add") { _, _ ->
                viewModel.addFirmwareGroup(input.text.toString())
            }
            .create()
        
        dialog.show()
    }

    private fun showEditGroupDialog(oldName: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_group, null)
        val input = dialogView.findViewById<TextInputEditText>(R.id.inputAndroidVersion).apply { setText(oldName) }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Firmware Group Name")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                viewModel.editFirmwareGroup(oldName, input.text.toString())
            }
            .create()

        dialog.show()
    }

    private fun showDeleteGroupConfirmationDialog(androidVersion: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Firmware Group?")
            .setMessage("Are you sure you want to delete the '$androidVersion' group and all its variants? This action cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteFirmwareGroup(androidVersion)
            }
            .show()
    }

    private fun showAddVariantDialog(androidVersion: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_variant, null)
        val productModelInput = dialogView.findViewById<TextInputEditText>(R.id.inputProductModel)
        val productNameInput = dialogView.findViewById<TextInputEditText>(R.id.inputProductName)
        val regionSpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.spinnerRegion)
        val versionLetterSpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.spinnerVersionLetter)
        val ruiVersionSpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.spinnerRuiVersion)
        val reqModeSpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.spinnerReqMode)
        val graySpinner = dialogView.findViewById<AutoCompleteTextView>(R.id.spinnerGray)

        val ruiVersions = (2..7).map { it.toString() }.toTypedArray()
        val ruiAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, ruiVersions)
        ruiVersionSpinner.setAdapter(ruiAdapter)

        val regionNames = RegionData.regions.map { it.displayName }
        val regionAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, regionNames)
        regionSpinner.setAdapter(regionAdapter)

        val versionLetters = arrayOf("A", "C", "F", "H")
        val versionLetterAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, versionLetters)
        versionLetterSpinner.setAdapter(versionLetterAdapter)

        val reqModes = arrayOf("manual", "server_auto", "client_auto", "taste")
        val reqModeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, reqModes)
        reqModeSpinner.setAdapter(reqModeAdapter)
        reqModeSpinner.setText("manual", false)

        val grayValues = arrayOf("0", "1")
        val grayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, grayValues)
        graySpinner.setAdapter(grayAdapter)
        graySpinner.setText("0", false)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Variant to $androidVersion")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Add") { _, _ ->
                val selectedRegionName = regionSpinner.text.toString()
                val selectedRegion = RegionData.regions.find { it.displayName == selectedRegionName }
                val selectedRuiVersion = ruiVersionSpinner.text.toString().toIntOrNull() ?: viewModel.uiState.value.ruiVersion
                val selectedReqMode = reqModeSpinner.text.toString().trim().takeIf { it.isNotBlank() } ?: "manual"
                val selectedGray = graySpinner.text.toString().trim().toIntOrNull() ?: 0

                viewModel.addVariantToGroup(
                    androidVersion = androidVersion,
                    productModel = productModelInput.text.toString(),
                    productName = productNameInput.text.toString(),
                    selectedRegion = selectedRegion,
                    versionLetter = versionLetterSpinner.text.toString(),
                    ruiVersion = selectedRuiVersion,
                    reqMode = selectedReqMode,
                    gray = selectedGray
                )
            }
            .create()
            
        dialog.show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
