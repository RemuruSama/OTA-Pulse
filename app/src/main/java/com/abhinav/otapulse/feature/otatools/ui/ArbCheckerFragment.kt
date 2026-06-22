package com.abhinav.otapulse.feature.otatools.ui

import android.content.res.ColorStateList
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.color.MaterialColors
import com.abhinav.otapulse.R
import com.abhinav.otapulse.core.common.setHapticClickListener
import com.abhinav.otapulse.databinding.FragmentArbCheckerBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ArbCheckerFragment : Fragment() {

    private var _binding: FragmentArbCheckerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OtaToolsViewModel by viewModels()

    private var selectedLocalZipUri: Uri? = null
    private var selectedLocalZipName: String = ""
    private var suppressSourceWatcher = false

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
        if (!binding.inputArbUrl.text.isNullOrBlank()) {
            suppressSourceWatcher = true
            binding.inputArbUrl.text?.clear()
            suppressSourceWatcher = false
        }
        viewModel.clearArbCheckResult()
        updateSelectedLocalZipSummary(uri)
        updateSourceUiState()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArbCheckerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupActions()
        observeState()
        runEnterAnimation()
    }

    private fun setupActions() {
        binding.btnPickLocalArbZip.setHapticClickListener {
            pickLocalZipLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
        }

        binding.btnRemoveLocalArbZip.setHapticClickListener {
            selectedLocalZipUri = null
            selectedLocalZipName = ""
            updateSelectedLocalZipSummary(null)
            viewModel.clearArbCheckResult()
            updateSourceUiState()
        }

        binding.btnCheckArb.setHapticClickListener {
            startArbCheck()
        }

        binding.inputArbUrl.doOnTextChanged { text, _, _, _ ->
            if (!suppressSourceWatcher && !text.isNullOrBlank() && selectedLocalZipUri != null) {
                selectedLocalZipUri = null
                selectedLocalZipName = ""
                updateSelectedLocalZipSummary(null)
            }
            viewModel.clearArbCheckResult()

            if (text.isNullOrBlank()) {
                binding.layoutArbUrl.endIconDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_paste_stroke)
                binding.layoutArbUrl.setEndIconOnClickListener {
                    pasteFromClipboard()
                }
            } else {
                binding.layoutArbUrl.endIconDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_close)
                binding.layoutArbUrl.setEndIconOnClickListener {
                    binding.inputArbUrl.text?.clear()
                }
            }

            updateSourceUiState()
        }

        binding.layoutArbUrl.setEndIconOnClickListener {
            pasteFromClipboard()
        }

        binding.inputArbUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO ||
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            ) {
                startArbCheck()
                true
            } else {
                false
            }
        }

        updateSourceUiState()
    }

    private fun startArbCheck() {
        val urlSource = binding.inputArbUrl.text?.toString()?.trim().orEmpty()
        val localZip = selectedLocalZipUri

        when {
            urlSource.isNotBlank() -> {
                viewModel.checkArb(
                    source = urlSource,
                    sourceLabel = "URL",
                    displayName = guessDisplayName(urlSource)
                )
            }
            localZip != null -> {
                viewModel.checkArb(
                    source = localZip.toString(),
                    sourceLabel = "Local ZIP",
                    displayName = selectedLocalZipName.ifBlank { "Local OTA ZIP" }
                )
            }
            else -> {
                Toast.makeText(requireContext(), "Paste a full OTA URL or choose a local ZIP first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                binding.progressCheckArb.isVisible = state.isCheckingArb
                binding.btnCheckArb.isEnabled = !state.isCheckingArb && hasSelectedSource()
                binding.btnCheckArb.text = if (state.isCheckingArb) "" else "Check ARB"

                binding.cardArbResult.isVisible = state.arbCheckResult != null
                state.arbCheckResult?.let { result ->
                    binding.tvArbResultStatus.text = result.arbInfo.toDisplayString()
                    val statusBackground = if (result.arbInfo.isSafe) {
                        ContextCompat.getColor(requireContext(), R.color.colorSuccessContainer)
                    } else {
                        MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorErrorContainer)
                    }
                    val statusForeground = if (result.arbInfo.isSafe) {
                        ContextCompat.getColor(requireContext(), R.color.colorOnSuccessContainer)
                    } else {
                        MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnErrorContainer)
                    }
                    binding.cardArbResultStatus.setCardBackgroundColor(statusBackground)
                    binding.tvArbResultStatus.setTextColor(statusForeground)
                    binding.tvArbResultIndex.text = result.arbInfo.arbIndex.toString()
                    binding.tvArbResultVersion.text = "${result.arbInfo.major}.${result.arbInfo.minor}"
                    binding.tvArbResultSource.text = result.sourceLabel
                }

                state.userMessage?.let { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    viewModel.clearUserMessage()
                }
            }
        }
    }

    private fun updateSourceUiState() {
        val isEnabled = hasSelectedSource() && !viewModel.uiState.value.isCheckingArb
        binding.btnCheckArb.isEnabled = isEnabled
        binding.btnCheckArb.backgroundTintList = ColorStateList.valueOf(
            if (isEnabled) {
                MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorErrorContainer)
            } else {
                MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorSurfaceContainerHighest)
            }
        )
        binding.btnCheckArb.setTextColor(
            if (isEnabled) {
                MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnErrorContainer)
            } else {
                MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnSurfaceVariant)
            }
        )
    }

    private fun hasSelectedSource(): Boolean {
        val hasUrl = !binding.inputArbUrl.text.isNullOrBlank()
        val hasLocalZip = selectedLocalZipUri != null
        return hasUrl || hasLocalZip
    }

    private fun updateSelectedLocalZipSummary(uri: Uri?) {
        binding.cardSelectedLocalArbZip.isVisible = uri != null
        binding.tvSelectedLocalArbZipName.text = if (uri != null) {
            selectedLocalZipName.ifBlank { "Local OTA ZIP selected" }
        } else {
            ""
        }
    }

    private fun pasteFromClipboard() {
        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val item = clipboard.primaryClip?.getItemAt(0)
        val pasteData = item?.text?.toString()
        if (!pasteData.isNullOrBlank()) {
            binding.inputArbUrl.setText(pasteData)
            Toast.makeText(requireContext(), "Link pasted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Clipboard is empty", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resolveDisplayName(uri: Uri): String {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        return requireContext().contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                cursor.getString(nameIndex).orEmpty()
            } else {
                uri.lastPathSegment.orEmpty()
            }
        }.orEmpty()
    }

    private fun guessDisplayName(source: String): String {
        return source.substringAfterLast('/').substringBefore('?').ifBlank { "Remote OTA package" }
    }

    private fun runEnterAnimation() {
        val views = listOf(
            binding.tvHeaderBadge,
            binding.tvCursor,
            binding.tvHeaderTitle,
            binding.viewAccentLine,
            binding.tvHeaderSubtitle,
            binding.cardHeroIcon,
            binding.layoutArbUrl,
            binding.btnCheckArb
        )
        com.abhinav.otapulse.core.common.AnimationUtils.animateEntrance(views)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
