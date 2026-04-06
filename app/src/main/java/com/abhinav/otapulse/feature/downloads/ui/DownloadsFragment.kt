package com.abhinav.otapulse.feature.downloads.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
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
        val urlErrorText = dialogView.findViewById<TextView>(R.id.urlErrorText)
        val urlEditText = dialogView.findViewById<TextInputEditText>(R.id.url_edit_text)
        val pasteUrlButton = dialogView.findViewById<ImageButton>(R.id.pasteUrlButton)
        val clearUrlButton = dialogView.findViewById<ImageButton>(R.id.clearUrlButton)
        val cancelButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.cancelButton)
        val addButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.addButton)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        cancelButton.setHapticClickListener {
            dialog.dismiss()
        }

        pasteUrlButton.setHapticClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipText = clipboard.primaryClip
                ?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)
                ?.coerceToText(requireContext())
                ?.toString()
                ?.trim()
                .orEmpty()

            if (clipText.isNotBlank()) {
                urlEditText.setText(clipText)
                urlEditText.setSelection(clipText.length)
                urlErrorText.visibility = View.GONE
            } else {
                Toast.makeText(requireContext(), R.string.no_url_in_clipboard, Toast.LENGTH_SHORT).show()
            }
        }

        clearUrlButton.setHapticClickListener {
            urlEditText.text?.clear()
            urlErrorText.visibility = View.GONE
        }

        addButton.setHapticClickListener {
            val url = urlEditText.text?.toString()?.trim().orEmpty()
            if (url.isBlank()) {
                urlErrorText.visibility = View.VISIBLE
                return@setHapticClickListener
            }
            urlErrorText.visibility = View.GONE
            viewModel.startDownloadWithUrl(url)
            dialog.dismiss()
        }

        urlEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) urlErrorText.visibility = View.GONE
        }

        val updateClearButtonState = {
            val hasText = !urlEditText.text.isNullOrBlank()
            clearUrlButton.isEnabled = hasText
            clearUrlButton.visibility = if (hasText) View.VISIBLE else View.GONE
            pasteUrlButton.visibility = if (hasText) View.GONE else View.VISIBLE
        }

        urlEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                updateClearButtonState()
            }
        })
        updateClearButtonState()

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
