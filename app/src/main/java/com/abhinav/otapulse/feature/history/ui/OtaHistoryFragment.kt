package com.abhinav.otapulse.feature.history.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Menu
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.MenuCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.abhinav.otapulse.R
import com.abhinav.otapulse.feature.devices.ui.DevicesViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.abhinav.otapulse.core.model.OtaHistoryEntry

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OtaHistoryFragment : Fragment() {

    private val historyViewModel: OtaHistoryViewModel by activityViewModels()
    private val devicesViewModel: DevicesViewModel by activityViewModels()
    private lateinit var adapter: OtaHistoryAdapter
    private lateinit var layoutEmptyState: View
    private lateinit var tvHistoryCount: com.google.android.material.chip.Chip

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try {
                requireContext().contentResolver.openInputStream(it)?.use { stream ->
                    val json = stream.bufferedReader().use { reader -> reader.readText() }
                    val type = object : TypeToken<List<OtaHistoryEntry>>() {}.type
                    val list: List<OtaHistoryEntry> = Gson().fromJson(json, type) ?: emptyList()
                    if (list.isNotEmpty()) {
                        historyViewModel.importHistory(list)
                        Toast.makeText(requireContext(), "Imported ${list.size} records!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "No records found in file.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Failed to import history.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            try {
                val historyList = historyViewModel.historyFlow.value
                val json = Gson().toJson(historyList)
                requireContext().contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(json.toByteArray())
                }
                Toast.makeText(requireContext(), "History exported successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Failed to export history.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val TAG = "OtaHistoryFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_ota_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)
        tvHistoryCount = view.findViewById(R.id.tvHistoryCount)
        
        val rvHistory: RecyclerView = view.findViewById(R.id.rvHistory)
        adapter = OtaHistoryAdapter { entry ->
            // When a history entry is clicked, show its OTA details
            devicesViewModel.showOtaDetailsFromHistory(entry)
        }
        rvHistory.adapter = adapter

        view.findViewById<View>(R.id.btnMoreOptions).setOnClickListener { v ->
            val popup = PopupMenu(requireContext(), v)
            popup.menu.add(1, 1, Menu.NONE, "Import History")
            popup.menu.add(2, 2, Menu.NONE, "Export History")
            MenuCompat.setGroupDividerEnabled(popup.menu, true)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        importLauncher.launch(arrayOf("application/json"))
                        true
                    }
                    2 -> {
                        val historyList = historyViewModel.historyFlow.value
                        if (historyList.isEmpty()) {
                            Toast.makeText(requireContext(), "No history to export.", Toast.LENGTH_SHORT).show()
                        } else {
                            exportLauncher.launch("ota_pulse_history.json")
                        }
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        view.findViewById<View>(R.id.btnClearHistory).setOnClickListener {
            val title = "Clear OTA History"
            val message = "Are you sure you want to clear ALL OTA history?"
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Clear") { _, _ ->
                    historyViewModel.clearHistory(null)
                    adapter.resetAnimations()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        historyViewModel.setDeviceName(null) // Global history for now

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                historyViewModel.historyFlow.collectLatest { history ->
                    adapter.submitList(history)
                    if (history.isEmpty()) {
                        layoutEmptyState.visibility = View.VISIBLE
                        rvHistory.visibility = View.GONE
                        tvHistoryCount.visibility = View.GONE
                    } else {
                        layoutEmptyState.visibility = View.GONE
                        rvHistory.visibility = View.VISIBLE
                        tvHistoryCount.visibility = View.VISIBLE
                        tvHistoryCount.text = "${history.size} record${if (history.size != 1) "s" else ""}"
                    }
                }
            }
        }
    }
}
