package com.abhinav.otapulse.ui.downloads

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.abhinav.otapulse.R
import com.abhinav.otapulse.databinding.FragmentDownloadsBinding
import com.abhinav.otapulse.util.setHapticClickListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DownloadsFragment : Fragment(R.layout.fragment_downloads) {

    private var _binding: FragmentDownloadsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DownloadsViewModel by viewModels()
    private lateinit var downloadAdapter: DownloadAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDownloadsBinding.bind(view)

        setupRecyclerView()
        observeViewModel()

        binding.addDownloadFab.setHapticClickListener {
            showAddDownloadDialog()
        }
    }

    private fun showAddDownloadDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_download, null)
        val urlEditText = dialogView.findViewById<TextInputEditText>(R.id.url_edit_text)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Download")
            .setView(dialogView)
            .setPositiveButton("Add") { dialog, _ ->
                val url = urlEditText.text.toString()
                if (url.isNotBlank()) {
                    viewModel.startDownloadWithUrl(url)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    private fun setupRecyclerView() {
        downloadAdapter = DownloadAdapter(
            onPause = { viewModel.pauseDownload(it) },
            onResume = { viewModel.resumeDownload(it) },
            onCancel = { viewModel.cancelDownload(it) },
            onRetry = { viewModel.retryDownload(it) },
            onDelete = { viewModel.deleteDownload(it) }
        )
        binding.downloadsRecyclerView.apply {
            adapter = downloadAdapter
            layoutManager = LinearLayoutManager(context)
            itemAnimator = null
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allDownloads.collect { downloads ->
                    binding.emptyView.isVisible = downloads.isEmpty()
                    binding.downloadsRecyclerView.isVisible = downloads.isNotEmpty()
                    downloadAdapter.submitList(downloads)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
