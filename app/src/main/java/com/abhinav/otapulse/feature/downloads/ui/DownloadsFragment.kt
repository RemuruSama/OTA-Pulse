package com.abhinav.otapulse.feature.downloads.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.abhinav.otapulse.R
import com.abhinav.otapulse.databinding.FragmentDownloadsBinding
import com.abhinav.otapulse.core.common.setHapticClickListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
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
        val urlInputLayout = dialogView.findViewById<TextInputLayout>(R.id.url_text_input_layout)
        val urlEditText = dialogView.findViewById<TextInputEditText>(R.id.url_edit_text)
        val cancelButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.cancelButton)
        val addButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.addButton)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        cancelButton.setHapticClickListener {
            dialog.dismiss()
        }

        addButton.setHapticClickListener {
            val url = urlEditText.text?.toString()?.trim().orEmpty()
            if (url.isBlank()) {
                urlInputLayout.error = "Enter a download URL"
                return@setHapticClickListener
            }
            urlInputLayout.error = null
            viewModel.startDownloadWithUrl(url)
            dialog.dismiss()
        }

        urlEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) urlInputLayout.error = null
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
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
                    binding.addDownloadFab.isVisible = downloads.isEmpty()
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
