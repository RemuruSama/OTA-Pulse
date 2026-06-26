package com.abhinav.otapulse.feature.settings.ui

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.abhinav.otapulse.R
import com.abhinav.otapulse.core.common.openExternalBrowser
import com.abhinav.otapulse.core.common.setHapticClickListener
import com.abhinav.otapulse.core.model.AppUpdateInfo
import com.abhinav.otapulse.core.network.AppUpdateDownloader
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.appbar.MaterialToolbar
import android.widget.TextView
import android.widget.ImageView
import android.widget.LinearLayout
import dagger.hilt.android.AndroidEntryPoint
import io.noties.markwon.Markwon
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Inject
import com.abhinav.otapulse.core.network.GitHubUpdater
import android.os.Handler
import android.os.Looper
import android.widget.FrameLayout

@AndroidEntryPoint
class AppUpdateFragment : Fragment(R.layout.fragment_app_update) {

    @Inject
    lateinit var okHttpClient: OkHttpClient

    private var updateInfo: AppUpdateInfo? = null

    companion object {
        private const val TAG = "AppUpdateFragment"
        private const val ARG_UPDATE_INFO = "arg_update_info"

        fun newInstance(info: AppUpdateInfo): AppUpdateFragment {
            return AppUpdateFragment().apply {
                arguments = bundleOf(ARG_UPDATE_INFO to info)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        updateInfo = arguments?.getParcelable(ARG_UPDATE_INFO)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        val tvAppVersion = view.findViewById<TextView>(R.id.tvAppVersion)
        val tvChangelog = view.findViewById<TextView>(R.id.tvChangelog)
        val btnLater = view.findViewById<MaterialButton>(R.id.btnLater)
        val btnUpdateNow = view.findViewById<MaterialButton>(R.id.btnUpdateNow)
        val btnInstallNow = view.findViewById<MaterialButton>(R.id.btnInstallNow)
        val actionButtonsContainer = view.findViewById<LinearLayout>(R.id.actionButtonsContainer)
        val downloadProgressContainer = view.findViewById<LinearLayout>(R.id.downloadProgressContainer)
        val updateProgressBar = view.findViewById<LinearProgressIndicator>(R.id.updateProgressBar)
        val tvDownloadProgress = view.findViewById<TextView>(R.id.tvDownloadProgress)

        val btnCheckUpdate = view.findViewById<MaterialButton>(R.id.btnCheckUpdate)
        val checkUpdateProgressContainer = view.findViewById<FrameLayout>(R.id.checkUpdateProgressContainer)
        val changelogSection = view.findViewById<View>(R.id.changelogSection)

        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val currentVersion = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName ?: ""
        tvAppVersion.text = "Version: $currentVersion"

        val info = updateInfo
        val contentContainer = view.findViewById<LinearLayout>(R.id.contentContainer)
        if (info != null) {
            setupUpdateAvailableState(info, tvAppVersion, tvChangelog, changelogSection, actionButtonsContainer, btnCheckUpdate, checkUpdateProgressContainer, contentContainer)
        } else {
            setupCheckForUpdateState(changelogSection, actionButtonsContainer, btnCheckUpdate, checkUpdateProgressContainer, currentVersion, tvAppVersion, tvChangelog, contentContainer)
        }

        btnLater.setHapticClickListener {
            parentFragmentManager.popBackStack()
        }

        btnUpdateNow.setHapticClickListener {
            actionButtonsContainer.visibility = View.GONE
            downloadProgressContainer.visibility = View.VISIBLE
            updateInfo?.let { startDownload(it, updateProgressBar, tvDownloadProgress, downloadProgressContainer, btnInstallNow) }
        }
    }

    private fun setupUpdateAvailableState(
        info: AppUpdateInfo,
        tvAppVersion: TextView,
        tvChangelog: TextView,
        changelogSection: View,
        actionButtonsContainer: View,
        btnCheckUpdate: View,
        checkUpdateProgressContainer: View,
        contentContainer: LinearLayout
    ) {
        contentContainer.gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
        tvAppVersion.text = "New Version: ${info.version}"
        val markwon = Markwon.create(requireContext())
        markwon.setMarkdown(tvChangelog, info.changelog)

        changelogSection.visibility = View.VISIBLE
        actionButtonsContainer.visibility = View.VISIBLE
        btnCheckUpdate.visibility = View.GONE
        checkUpdateProgressContainer.visibility = View.GONE
    }

    private fun setupCheckForUpdateState(
        changelogSection: View,
        actionButtonsContainer: View,
        btnCheckUpdate: MaterialButton,
        checkUpdateProgressContainer: View,
        currentVersion: String,
        tvAppVersion: TextView,
        tvChangelog: TextView,
        contentContainer: LinearLayout
    ) {
        contentContainer.gravity = android.view.Gravity.CENTER
        changelogSection.visibility = View.GONE
        actionButtonsContainer.visibility = View.GONE
        btnCheckUpdate.visibility = View.VISIBLE
        btnCheckUpdate.isEnabled = true
        checkUpdateProgressContainer.visibility = View.GONE

        btnCheckUpdate.setHapticClickListener {
            btnCheckUpdate.isEnabled = false
            checkUpdateProgressContainer.visibility = View.VISIBLE

            val startTime = System.currentTimeMillis()
            val minAnimationTime = 1500L

            GitHubUpdater.checkForUpdate(currentVersion.removePrefix("v"), httpClient = okHttpClient) { fetchedInfo ->
                activity?.runOnUiThread {
                    if (view == null) return@runOnUiThread

                    val elapsedTime = System.currentTimeMillis() - startTime
                    val delay = (minAnimationTime - elapsedTime).coerceAtLeast(0)

                    Handler(Looper.getMainLooper()).postDelayed({
                        if (view == null) return@postDelayed

                        if (fetchedInfo != null) {
                            val newInfo = AppUpdateInfo(fetchedInfo.version, fetchedInfo.downloadUrl, fetchedInfo.changelog)
                            updateInfo = newInfo
                            setupUpdateAvailableState(newInfo, tvAppVersion, tvChangelog, changelogSection, actionButtonsContainer, btnCheckUpdate, checkUpdateProgressContainer, contentContainer)
                        } else {
                            checkUpdateProgressContainer.visibility = View.GONE
                            btnCheckUpdate.visibility = View.GONE // Hide it or keep it? The screenshot doesn't show a button. Let's hide it.
                            
                            val tvAppTitle = requireView().findViewById<TextView>(R.id.tvAppTitle)
                            val ivAppIcon = requireView().findViewById<ImageView>(R.id.ivAppIcon)
                            
                            tvAppTitle.text = "You're on the latest version"
                            tvAppVersion.text = "Current version: $currentVersion"
                            tvAppVersion.setBackgroundResource(R.drawable.bg_version_pill)
                            
                            val onPrimaryContainerColor = com.google.android.material.color.MaterialColors.getColor(tvAppVersion, com.google.android.material.R.attr.colorOnPrimaryContainer)
                            tvAppVersion.setTextColor(onPrimaryContainerColor)
                            
                            ivAppIcon.setImageResource(R.drawable.ic_check_circle)
                            
                            val primaryColor = com.google.android.material.color.MaterialColors.getColor(ivAppIcon, androidx.appcompat.R.attr.colorPrimary)
                            ivAppIcon.imageTintList = android.content.res.ColorStateList.valueOf(primaryColor)
                        }
                    }, delay)
                }
            }
        }
    }

    private fun startDownload(
        info: AppUpdateInfo,
        progressBar: LinearProgressIndicator,
        tvProgress: TextView,
        downloadProgressContainer: View,
        btnInstallNow: MaterialButton
    ) {
        val fileName = "otapulse_update_${info.version}.apk"
        val targetFile = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)

        lifecycleScope.launch {
            AppUpdateDownloader.downloadApk(info.downloadUrl, targetFile, okHttpClient)
                .collect { state ->
                    when (state) {
                        is AppUpdateDownloader.DownloadState.Downloading -> {
                            progressBar.progress = state.progress
                            tvProgress.text = "${state.progress}%"
                        }
                        is AppUpdateDownloader.DownloadState.Success -> {
                            downloadProgressContainer.visibility = View.GONE
                            btnInstallNow.visibility = View.VISIBLE
                            btnInstallNow.setHapticClickListener {
                                installApk(state.file)
                            }
                            installApk(state.file)
                        }
                        is AppUpdateDownloader.DownloadState.Error -> {
                            Toast.makeText(requireContext(), "Download failed: ${state.exception.message}", Toast.LENGTH_LONG).show()
                            requireContext().openExternalBrowser(info.downloadUrl)
                            parentFragmentManager.popBackStack()
                        }
                        else -> {}
                    }
                }
        }
    }

    private fun installApk(apkFile: File) {
        try {
            val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", apkFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start install intent", e)
            Toast.makeText(requireContext(), "Failed to install update", Toast.LENGTH_SHORT).show()
        }
    }
}
