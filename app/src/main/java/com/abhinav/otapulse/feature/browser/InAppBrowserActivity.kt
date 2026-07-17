package com.abhinav.otapulse.feature.browser

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.abhinav.otapulse.R
import com.abhinav.otapulse.core.preferences.ThemePreferences
import com.abhinav.otapulse.core.ui.theme.OtaPulseTheme
import com.abhinav.otapulse.feature.browser.ui.InAppBrowserScreen

class InAppBrowserActivity : ComponentActivity() {

    private var activeWebView: WebView? = null

    private val initialUrl: String
        get() = intent.getStringExtra(EXTRA_URL).orEmpty()

    private val initialTitle: String?
        get() = intent.getStringExtra(EXTRA_TITLE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Apply AMOLED overlay if enabled and currently in dark mode
        val themePrefs = getSharedPreferences(ThemePreferences.PREFS_NAME, Context.MODE_PRIVATE)
        val isAmoled = themePrefs.getBoolean(ThemePreferences.PREF_AMOLED_MODE, false)
        if (isAmoled) {
            val uiMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
            if (uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                theme.applyStyle(R.style.ThemeOverlay_OTAPulse_Amoled, true)
            }
        }

        setContent {
            OtaPulseTheme {
                InAppBrowserScreen(
                    initialUrl = initialUrl,
                    initialTitle = initialTitle,
                    savedInstanceState = savedInstanceState,
                    onWebViewCreated = { webView ->
                        activeWebView = webView
                    },
                    onFinish = { finish() }
                )
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        activeWebView?.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        activeWebView?.apply {
            stopLoading()
            webChromeClient = null
            webViewClient = WebViewClient()
            destroy()
        }
        activeWebView = null
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
