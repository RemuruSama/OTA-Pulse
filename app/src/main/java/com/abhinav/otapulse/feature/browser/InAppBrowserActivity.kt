package com.abhinav.otapulse.feature.browser

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.abhinav.otapulse.R
import com.abhinav.otapulse.core.common.setHapticClickListener
import com.abhinav.otapulse.databinding.ActivityInAppBrowserBinding
import com.abhinav.otapulse.feature.settings.SettingsFragment
import com.google.android.material.color.MaterialColors

class InAppBrowserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInAppBrowserBinding
    private var manualDesktopMode = false
    private var wideWindowDesktopMode = false
    private var effectiveDesktopMode = false
    private val defaultUserAgent by lazy {
        WebSettings.getDefaultUserAgent(this)
    }

    private val browserPrefs by lazy {
        getSharedPreferences(SettingsFragment.APP_SETTINGS_PREFS, Context.MODE_PRIVATE)
    }

    private val initialUrl: String
        get() = intent.getStringExtra(EXTRA_URL).orEmpty()

    private val initialTitle: String?
        get() = intent.getStringExtra(EXTRA_TITLE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Apply AMOLED overlay if enabled and currently in dark mode
        val themePrefs = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val isAmoled = themePrefs.getBoolean(SettingsFragment.PREF_AMOLED_MODE, false)
        if (isAmoled) {
            val uiMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
            if (uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                theme.applyStyle(R.style.ThemeOverlay_OTAPulse_Amoled, true)
            }
        }

        binding = ActivityInAppBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupEdgeToEdge()
        setupBackHandling()
        setupChrome()
        setupWebView()
        updateWideWindowDesktopMode(forceReload = false)

        if (savedInstanceState == null) {
            binding.webView.loadUrl(initialUrl)
            updateBrowserChrome(initialUrl, isLoading = true)
        } else {
            binding.webView.restoreState(savedInstanceState)
            updateBrowserChrome(binding.webView.url ?: initialUrl, isLoading = false)
        }
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.topBar.updatePadding(top = systemBars.top)
            binding.browserControls.updateLayoutParams<android.widget.FrameLayout.LayoutParams> {
                bottomMargin = 12 + systemBars.bottom
            }
            insets
        }
    }

    private fun setupBackHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.webView.canGoBack()) {
                    binding.webView.goBack()
                } else {
                    finish()
                }
            }
        })
    }

    private fun setupChrome() {
        binding.navBack.setHapticClickListener { finish() }
        binding.actionBack.setHapticClickListener { if (binding.webView.canGoBack()) binding.webView.goBack() }
        binding.actionForward.setHapticClickListener { if (binding.webView.canGoForward()) binding.webView.goForward() }
        binding.actionReload.setHapticClickListener { binding.webView.reload() }
        binding.actionDesktopMode.setHapticClickListener {
            manualDesktopMode = !manualDesktopMode
            browserPrefs.edit()
                .putBoolean(SettingsFragment.PREF_BROWSER_DESKTOP_MODE, manualDesktopMode)
                .apply()
            applyDesktopMode(forceReload = true)
        }
        binding.actionCopyLink.setHapticClickListener {
            val url = binding.webView.url ?: initialUrl
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.button_copy_link), url))
            Toast.makeText(this, R.string.link_copied, Toast.LENGTH_SHORT).show()
        }
        binding.actionShare.setHapticClickListener {
            val url = binding.webView.url ?: initialUrl
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, url)
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.browser_share)))
        }
        binding.actionOpenExternal.setHapticClickListener {
            openExternally(binding.webView.url ?: initialUrl)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        manualDesktopMode = browserPrefs.getBoolean(SettingsFragment.PREF_BROWSER_DESKTOP_MODE, false)
        val showControls = browserPrefs.getBoolean(SettingsFragment.PREF_BROWSER_SHOW_CONTROLS, true)

        binding.browserControls.isVisible = showControls
        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }
        applyDesktopMode(forceReload = false)
        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                binding.progressIndicator.progress = newProgress
                binding.progressIndicator.isVisible = newProgress in 0..99
                binding.loadingIndicator.isVisible = newProgress in 0..99
            }
        }
        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val target = request?.url?.toString().orEmpty()
                if (target.startsWith("http://") || target.startsWith("https://")) return false
                openExternally(target)
                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                updateBrowserChrome(url, isLoading = true)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                if (effectiveDesktopMode) {
                    binding.webView.evaluateJavascript(DESKTOP_VIEWPORT_SCRIPT, null)
                }
                updateBrowserChrome(url, isLoading = false)
            }
        }
    }

    private fun updateWideWindowDesktopMode(forceReload: Boolean) {
        val shouldUseWideWindowDesktop = resources.configuration.screenWidthDp >= DESKTOP_MODE_BREAKPOINT_DP
        if (wideWindowDesktopMode == shouldUseWideWindowDesktop) {
            return
        }
        wideWindowDesktopMode = shouldUseWideWindowDesktop
        applyDesktopMode(forceReload)
    }

    private fun applyDesktopMode(forceReload: Boolean) {
        val shouldUseDesktopMode = manualDesktopMode || wideWindowDesktopMode
        val desktopModeChanged = effectiveDesktopMode != shouldUseDesktopMode
        effectiveDesktopMode = shouldUseDesktopMode

        binding.webView.settings.apply {
            userAgentString = if (effectiveDesktopMode) {
                buildDesktopUserAgent(defaultUserAgent)
            } else {
                defaultUserAgent
            }
            loadWithOverviewMode = effectiveDesktopMode || wideWindowDesktopMode
            useWideViewPort = effectiveDesktopMode || wideWindowDesktopMode
        }

        updateDesktopModeButton()

        if ((desktopModeChanged || forceReload) && !binding.webView.url.isNullOrBlank()) {
            binding.webView.reload()
        }
    }

    private fun updateDesktopModeButton() {
        val tint = if (effectiveDesktopMode) {
            MaterialColors.getColor(binding.actionDesktopMode, androidx.appcompat.R.attr.colorPrimary)
        } else {
            MaterialColors.getColor(binding.actionDesktopMode, com.google.android.material.R.attr.colorOnSurface)
        }
        binding.actionDesktopMode.imageTintList = ColorStateList.valueOf(tint)
        binding.actionDesktopMode.alpha = if (effectiveDesktopMode) 1f else 0.72f
        binding.actionDesktopMode.contentDescription = getString(
            if (effectiveDesktopMode) R.string.browser_disable_desktop_mode
            else R.string.browser_enable_desktop_mode
        )
    }

    private fun updateBrowserChrome(url: String?, isLoading: Boolean) {
        val resolvedUrl = url?.takeIf { it.isNotBlank() } ?: initialUrl
        val compactAddress = resolvedUrl.toUri().host?.removePrefix("www.") ?: resolvedUrl

        binding.addressText.text = initialTitle?.takeIf { it.isNotBlank() } ?: compactAddress
        binding.addressIcon.alpha = if (isLoading) 0.6f else 1f
        binding.actionBack.isEnabled = binding.webView.canGoBack()
        binding.actionForward.isEnabled = binding.webView.canGoForward()
        binding.actionBack.alpha = if (binding.actionBack.isEnabled) 1f else 0.4f
        binding.actionForward.alpha = if (binding.actionForward.isEnabled) 1f else 0.4f
        binding.loadingIndicator.isVisible = isLoading
    }

    private fun openExternally(url: String) {
        if (url.isBlank()) return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(this, R.string.could_not_open_link, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        binding.webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            updateWideWindowDesktopMode(forceReload = true)
        }
    }

    override fun onDestroy() {
        binding.webView.apply {
            stopLoading()
            loadUrl("about:blank")
            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()
            destroy()
        }
        super.onDestroy()
    }

    private fun buildDesktopUserAgent(baseUserAgent: String): String {
        return baseUserAgent
            .replace(Regex("\\s?wv"), "")
            .replace(Regex("\\)\\s+Version/[^ ]+"), ")")
            .replace(
                Regex("\\(Linux; Android [^\\)]*\\)"),
                "(X11; Linux x86_64)"
            )
            .replace(" Mobile ", " ")
            .replace(" Mobile", "")
            .replace("; wv", "")
            .trim()
    }

    companion object {
        private const val EXTRA_URL = "url"
        private const val EXTRA_TITLE = "title"
        private const val DESKTOP_MODE_BREAKPOINT_DP = 840
        private const val DESKTOP_VIEWPORT_SCRIPT = """
            (function() {
                var meta = document.querySelector('meta[name="viewport"]');
                if (!meta) {
                    meta = document.createElement('meta');
                    meta.name = 'viewport';
                    document.head.appendChild(meta);
                }
                meta.setAttribute('content', 'width=1280, initial-scale=0.25, viewport-fit=cover');
            })();
        """

        fun createIntent(context: Context, url: String, title: String? = null): Intent {
            return Intent(context, InAppBrowserActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_TITLE, title)
            }
        }
    }
}
