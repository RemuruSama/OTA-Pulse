package com.abhinav.otapulse.feature.browser

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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

class InAppBrowserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInAppBrowserBinding
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

        binding = ActivityInAppBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupEdgeToEdge()
        setupBackHandling()
        setupChrome()
        setupWebView()

        if (savedInstanceState == null) {
            binding.webView.loadUrl(initialUrl)
            updateBrowserChrome(initialUrl, isLoading = true)
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
        binding.navBack.setHapticClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.actionBack.setHapticClickListener { if (binding.webView.canGoBack()) binding.webView.goBack() }
        binding.actionForward.setHapticClickListener { if (binding.webView.canGoForward()) binding.webView.goForward() }
        binding.actionReload.setHapticClickListener { binding.webView.reload() }
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
        val isDesktopMode = browserPrefs.getBoolean(SettingsFragment.PREF_BROWSER_DESKTOP_MODE, false)
        val showControls = browserPrefs.getBoolean(SettingsFragment.PREF_BROWSER_SHOW_CONTROLS, true)

        binding.browserControls.isVisible = showControls
        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            builtInZoomControls = true
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            userAgentString = if (isDesktopMode) {
                userAgentString
                    .replace("Android", "X11; Linux x86_64")
                    .replace("Mobile", "")
            } else {
                WebSettings.getDefaultUserAgent(this@InAppBrowserActivity)
            }
        }
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
                updateBrowserChrome(url, isLoading = false)
            }
        }
    }

    private fun updateBrowserChrome(url: String?, isLoading: Boolean) {
        val resolvedUrl = url?.takeIf { it.isNotBlank() } ?: initialUrl
        val compactAddress = resolvedUrl.toUri().host?.removePrefix("www.") ?: resolvedUrl

        binding.addressText.text = compactAddress
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

    companion object {
        private const val EXTRA_URL = "url"
        private const val EXTRA_TITLE = "title"

        fun createIntent(context: Context, url: String, title: String? = null): Intent {
            return Intent(context, InAppBrowserActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_TITLE, title)
            }
        }
    }
}
