package com.abhinav.otapulse.feature.otatools.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.abhinav.otapulse.databinding.FragmentLinkResolverBinding
import com.abhinav.otapulse.core.common.setHapticClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LinkResolverFragment : Fragment() {

    private var _binding: FragmentLinkResolverBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OtaToolsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLinkResolverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupActions()
        observeState()
        runEnterAnimation()
    }

    private fun setupActions() {
        binding.btnResolveLink.setHapticClickListener {
            viewModel.resolveLink(binding.inputResolverUrl.text?.toString().orEmpty())
        }

        binding.inputResolverUrl.doOnTextChanged { text, _, _, _ ->
            if (text.isNullOrBlank()) {
                binding.layoutResolverInput.endIconDrawable = ContextCompat.getDrawable(requireContext(), com.abhinav.otapulse.R.drawable.ic_paste_stroke)
                binding.layoutResolverInput.setEndIconOnClickListener {
                    pasteFromClipboard()
                }
            } else {
                binding.layoutResolverInput.endIconDrawable = ContextCompat.getDrawable(requireContext(), com.abhinav.otapulse.R.drawable.ic_close)
                binding.layoutResolverInput.setEndIconOnClickListener {
                    binding.inputResolverUrl.text?.clear()
                }
            }
        }

        // Initial setup
        binding.layoutResolverInput.setEndIconOnClickListener {
            pasteFromClipboard()
        }

        binding.btnCopyResolvedUrl.setHapticClickListener {
            val resolvedUrl = viewModel.uiState.value.resolverResult?.resolvedUrl ?: return@setHapticClickListener
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Resolved OTA Link", resolvedUrl))
            Toast.makeText(requireContext(), "Resolved link copied", Toast.LENGTH_SHORT).show()
        }

        binding.btnShareResolvedUrl.setHapticClickListener {
            val resolvedUrl = viewModel.uiState.value.resolverResult?.resolvedUrl ?: return@setHapticClickListener
            try {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "OTA-Pulse Resolved URL: $resolvedUrl")
                }
                startActivity(Intent.createChooser(shareIntent, "Share URL"))
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "Could not share link", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun pasteFromClipboard() {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val item = clipboard.primaryClip?.getItemAt(0)
        val pasteData = item?.text?.toString()
        if (!pasteData.isNullOrBlank()) {
            binding.inputResolverUrl.setText(pasteData)
            Toast.makeText(requireContext(), "Link pasted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Clipboard is empty", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                if (state.isLoading) {
                    binding.progressResolveLink.indicatorColor = binding.btnResolveLink.currentTextColor
                    binding.progressResolveLink.isVisible = true
                    binding.btnResolveLink.isEnabled = false
                    binding.btnResolveLink.text = ""
                } else {
                    binding.progressResolveLink.isVisible = false
                    binding.btnResolveLink.isEnabled = true
                    binding.btnResolveLink.text = "Resolve Link"
                }

                binding.cardResolverResult.isVisible = state.resolverResult != null
                state.resolverResult?.let { resolved ->
                    binding.tvResolverFilename.text = resolved.fileName ?: "Resolved OTA link"
                    binding.tvResolvedUrl.text = resolved.resolvedUrl
                }
                state.userMessage?.let { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    viewModel.clearUserMessage()
                }
            }
        }
    }

    private fun runEnterAnimation() {
        val views = listOf(
            binding.tvHeaderBadge,
            binding.tvHeaderTitle,
            binding.tvHeaderSubtitle,
            binding.cardHeroIcon,
            binding.btnResolveLink
        )
        com.abhinav.otapulse.core.common.AnimationUtils.animateEntrance(views)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
