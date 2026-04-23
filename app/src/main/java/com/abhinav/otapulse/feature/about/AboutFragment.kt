package com.abhinav.otapulse.feature.about

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.abhinav.otapulse.R
import com.abhinav.otapulse.databinding.FragmentAboutBinding
import com.abhinav.otapulse.core.network.GitHubUpdater
import com.abhinav.otapulse.core.network.UpdateInfo
import com.abhinav.otapulse.core.common.openExternalBrowser
import com.abhinav.otapulse.core.common.openInAppBrowser
import com.abhinav.otapulse.core.common.setHapticClickListener
import com.abhinav.otapulse.core.common.AnimationUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.noties.markwon.Markwon
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AboutFragment : Fragment() {

    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!

    // Store the update info if found, so clicking the button again doesn't re-fetch
    private var pendingUpdateInfo: UpdateInfo? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupVersionInfo()
        setupClickListeners()

        // 1. Initial State: Manual Check
        setupManualCheckButton()

        // 2. Auto-Check: Run silently in background to see if we need to upgrade the button
        performSilentUpdateCheck()

        runEnterAnimation()

        // Fix: Ensure the view is fully inflated and laid out before starting animation
        // and using Animatable interface for wider compatibility.
        binding.creatorAvatar.post {
            val avd = binding.creatorAvatar.drawable as? Animatable
            avd?.start()
        }
    }

    private fun setupVersionInfo() {
        try {
            val packageInfo = requireActivity().packageManager.getPackageInfo(requireActivity().packageName, 0)
            val versionText = "v${packageInfo.versionName}"
            binding.versionBadge.text = versionText
        } catch (e: Exception) {
            e.printStackTrace()
            binding.versionBadge.text = "v1.0"
        }
    }

    private fun performSilentUpdateCheck() {
        // Don't show progress bar for silent check
        val currentVersion = binding.versionBadge.text.toString().removePrefix("v")

        GitHubUpdater.checkForUpdate(currentVersion) { updateInfo ->
            activity?.runOnUiThread {
                if (_binding == null) return@runOnUiThread

                if (updateInfo != null) {
                    // UPDATE FOUND! Change UI to "Update Available"
                    pendingUpdateInfo = updateInfo
                    applyUpdateAvailableUi(updateInfo)
                }
            }
        }
    }

    private fun applyUpdateAvailableUi(info: UpdateInfo) {
        binding.btnCheckUpdate.let { btn ->
            btn.text = "Update Available"
            btn.setIconResource(R.drawable.ic_download) // Ensure you have this icon or remove this line

            // Change color to indicate importance (Primary color)
            val primaryColor = ContextCompat.getColor(requireContext(), com.google.android.material.R.color.material_dynamic_primary50) // Or R.color.your_primary
            btn.setBackgroundColor(primaryColor)
            btn.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            btn.iconTint = ContextCompat.getColorStateList(requireContext(), android.R.color.white)

            // Update Click Listener to open dialog immediately instead of checking
            btn.setHapticClickListener {
                showUpdateDialog(info)
            }
        }
    }

    private fun setupManualCheckButton() {
        // Default state: "Check for Updates"
        binding.btnCheckUpdate.setHapticClickListener {
            if (pendingUpdateInfo != null) {
                showUpdateDialog(pendingUpdateInfo!!)
                return@setHapticClickListener
            }

            val startTime = System.currentTimeMillis()
            val minAnimationTime = 3000L // 3 second minimum

            // Manual Check Logic (with Loading UI)
            binding.btnCheckUpdate.isEnabled = false
            binding.btnCheckUpdate.text = ""
            // Fade in progress indicator — withLayer() composites alpha cheaply from GPU texture
            binding.progressUpdate.apply {
                indicatorColor = binding.btnCheckUpdate.currentTextColor
                alpha = 0f
                visibility = View.VISIBLE
                animate()
                    .alpha(1f)
                    .setDuration(300)
                    .withLayer()
                    .start()
            }

            val currentVersion = binding.versionBadge.text.toString().removePrefix("v")

            GitHubUpdater.checkForUpdate(currentVersion) { updateInfo ->
                activity?.runOnUiThread {
                    if (_binding == null) return@runOnUiThread

                    val elapsedTime = System.currentTimeMillis() - startTime
                    val delay = (minAnimationTime - elapsedTime).coerceAtLeast(0)

                    Handler(Looper.getMainLooper()).postDelayed({
                        if (_binding == null) return@postDelayed // Re-check binding in case fragment is destroyed during delay

                        // Fade out progress indicator — withLayer() composites alpha cheaply from GPU texture
                        binding.progressUpdate.animate()
                            .alpha(0f)
                            .setDuration(300)
                            .withLayer()
                            .withEndAction {
                                binding.progressUpdate.visibility = View.GONE
                                binding.btnCheckUpdate.isEnabled = true

                                if (updateInfo != null) {
                                    pendingUpdateInfo = updateInfo
                                    applyUpdateAvailableUi(updateInfo)
                                    showUpdateDialog(updateInfo)
                                } else {
                                    binding.btnCheckUpdate.text = "Check for Updates" // Reset text
                                    Toast.makeText(requireContext(), "You are using the latest version", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .start()
                    }, delay)
                }
            }
        }
    }

    private fun showUpdateDialog(info: UpdateInfo) {
        val markwon = Markwon.create(requireContext())
        val markdownText = markwon.toMarkdown(info.changelog)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("New Version Available: ${info.version}")
            .setMessage(markdownText)
            .setPositiveButton("Download") { _, _ ->
                try {
                    openExternalBrowser(info.downloadUrl)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Could not open download link", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun setupClickListeners() {
        binding.githubButton.setHapticClickListener { openExternal("https://github.com/RemuruSama/OTA-Pulse") }
        binding.telegramButton.setHapticClickListener { openUrl("https://t.me/abhinav_v1") }
        binding.buyMeACoffeeButton.setHapticClickListener { openExternal("https://paypal.me/Abhinavftp?country.x=IN&locale.x=en_GB") }
        binding.creatorCard.setHapticClickListener { openUrl("https://t.me/CodeSenseiX") }
        binding.upiIdCopyCard.setHapticClickListener { copyUpiToClipboard() }
    }

    private fun openUrl(url: String) {
        try {
            openInAppBrowser(url)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Could not open link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openExternal(url: String) {
        try {
            openExternalBrowser(url)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Could not open link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyUpiToClipboard() {
        val upiId = getString(R.string.upi_id_value)
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("UPI ID", upiId)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), getString(R.string.upi_id_copied_toast), Toast.LENGTH_SHORT).show()
    }

    private fun runEnterAnimation() {
        val elements = listOfNotNull(
            binding.profileSection,
            binding.mainSheet,
            binding.creatorCard,
            binding.footerText
        )
        // Delegate to shared utility — picks up the correct interpolator,
        // density-independent offset, and hardware-layer optimisation.
        AnimationUtils.animateEntrance(elements)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
