package com.abhinav.otapulse.feature.browser.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.abhinav.otapulse.R
import com.abhinav.otapulse.core.common.performHapticFeedback
import com.abhinav.otapulse.core.preferences.AppSettingsPreferences

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

@Composable
fun InAppBrowserScreen(
    initialUrl: String,
    initialTitle: String?,
    savedInstanceState: Bundle?,
    onWebViewCreated: (WebView) -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    val browserPrefs = remember {
        context.getSharedPreferences(AppSettingsPreferences.PREFS_NAME, Context.MODE_PRIVATE)
    }
    val showControls = remember(browserPrefs) {
        browserPrefs.getBoolean(AppSettingsPreferences.PREF_BROWSER_SHOW_CONTROLS, true)
    }
    val defaultUserAgent = remember {
        try {
            WebSettings.getDefaultUserAgent(context)
        } catch (_: Exception) {
            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
        }
    }

    var manualDesktopMode by remember {
        mutableStateOf(browserPrefs.getBoolean(AppSettingsPreferences.PREF_BROWSER_DESKTOP_MODE, false))
    }
    var wideWindowDesktopMode by remember {
        mutableStateOf(screenWidthDp >= 840)
    }
    var effectiveDesktopMode by remember {
        mutableStateOf(manualDesktopMode || wideWindowDesktopMode)
    }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(screenWidthDp) {
        val shouldUseWide = screenWidthDp >= 840
        if (wideWindowDesktopMode != shouldUseWide) {
            wideWindowDesktopMode = shouldUseWide
            val newEffective = manualDesktopMode || wideWindowDesktopMode
            if (effectiveDesktopMode != newEffective) {
                effectiveDesktopMode = newEffective
                webViewInstance?.apply {
                    settings.apply {
                        userAgentString = if (effectiveDesktopMode) buildDesktopUserAgent(defaultUserAgent) else defaultUserAgent
                        loadWithOverviewMode = effectiveDesktopMode || wideWindowDesktopMode
                        useWideViewPort = effectiveDesktopMode || wideWindowDesktopMode
                    }
                    if (!url.isNullOrBlank()) reload()
                }
            }
        }
    }

    val toggleDesktopMode = {
        manualDesktopMode = !manualDesktopMode
        browserPrefs.edit()
            .putBoolean(AppSettingsPreferences.PREF_BROWSER_DESKTOP_MODE, manualDesktopMode)
            .apply()
        val newEffective = manualDesktopMode || wideWindowDesktopMode
        effectiveDesktopMode = newEffective
        webViewInstance?.apply {
            settings.apply {
                userAgentString = if (effectiveDesktopMode) buildDesktopUserAgent(defaultUserAgent) else defaultUserAgent
                loadWithOverviewMode = effectiveDesktopMode || wideWindowDesktopMode
                useWideViewPort = effectiveDesktopMode || wideWindowDesktopMode
            }
            if (!url.isNullOrBlank()) reload()
        }
        context.performHapticFeedback()
    }

    BackHandler(enabled = true) {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onFinish()
        }
    }

    DisposableEffect(webViewInstance) {
        onDispose {
            webViewInstance?.stopLoading()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    context.performHapticFeedback()
                    onFinish()
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = stringResource(id = R.string.browser_back),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .height(44.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_language),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )

                        val displayTitle = remember(currentUrl, initialTitle) {
                            if (!initialTitle.isNullOrBlank()) {
                                initialTitle
                            } else {
                                try {
                                    currentUrl.toUri().host?.removePrefix("www.") ?: currentUrl
                                } catch (_: Exception) {
                                    currentUrl
                                }
                            }
                        }

                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 10.dp)
                        )

                        if (isLoading && progress in 0..99) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                IconButton(onClick = {
                    context.performHapticFeedback()
                    openExternally(context, currentUrl)
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_open_in_new),
                        contentDescription = stringResource(id = R.string.browser_open_external),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    ) { innerPadding ->
        // WebView + Bottom Controls Container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AndroidView(
                factory = { ctx ->
                    manualDesktopMode = browserPrefs.getBoolean(AppSettingsPreferences.PREF_BROWSER_DESKTOP_MODE, false)
                    wideWindowDesktopMode = ctx.resources.configuration.screenWidthDp >= 840
                    effectiveDesktopMode = manualDesktopMode || wideWindowDesktopMode

                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.apply {
                            @SuppressLint("SetJavaScriptEnabled")
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            setSupportZoom(true)
                            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            userAgentString = if (effectiveDesktopMode) buildDesktopUserAgent(defaultUserAgent) else defaultUserAgent
                            loadWithOverviewMode = effectiveDesktopMode || wideWindowDesktopMode
                            useWideViewPort = effectiveDesktopMode || wideWindowDesktopMode
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progress = newProgress
                                isLoading = newProgress in 0..99
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val target = request?.url?.toString().orEmpty()
                                if (target.startsWith("http://") || target.startsWith("https://")) return false
                                openExternally(ctx, target)
                                return true
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                url?.let { currentUrl = it }
                                isLoading = true
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                if (effectiveDesktopMode) {
                                    evaluateJavascript(DESKTOP_VIEWPORT_SCRIPT, null)
                                }
                                url?.let { currentUrl = it }
                                isLoading = false
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                            }

                            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                super.doUpdateVisitedHistory(view, url, isReload)
                                url?.let { currentUrl = it }
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                            }
                        }

                        if (savedInstanceState == null) {
                            post {
                                if (url == null || url == "about:blank" || url?.isEmpty() == true) {
                                    loadUrl(initialUrl)
                                }
                            }
                        } else {
                            restoreState(savedInstanceState)
                        }
                        webViewInstance = this
                        onWebViewCreated(this)
                    }
                },
                update = { _ -> },
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            )

                // Progress Bar overlay
                if (isLoading && progress in 0..99) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                if (showControls) {
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 8.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(
                                onClick = {
                                    context.performHapticFeedback()
                                    webViewInstance?.goBack()
                                    webViewInstance?.let {
                                        canGoBack = it.canGoBack()
                                        canGoForward = it.canGoForward()
                                    }
                                },
                                enabled = canGoBack
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_arrow_back),
                                    contentDescription = stringResource(id = R.string.browser_back),
                                    tint = if (canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }

                            IconButton(
                                onClick = {
                                    context.performHapticFeedback()
                                    webViewInstance?.goForward()
                                    webViewInstance?.let {
                                        canGoBack = it.canGoBack()
                                        canGoForward = it.canGoForward()
                                    }
                                },
                                enabled = canGoForward
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_arrow_back),
                                    contentDescription = stringResource(id = R.string.browser_forward),
                                    modifier = Modifier.rotate(180f),
                                    tint = if (canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }

                            IconButton(
                                onClick = {
                                    context.performHapticFeedback()
                                    webViewInstance?.reload()
                                }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_retry),
                                    contentDescription = stringResource(id = R.string.browser_reload),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(
                                onClick = { toggleDesktopMode() }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_desktop_windows),
                                    contentDescription = stringResource(
                                        id = if (effectiveDesktopMode) R.string.browser_disable_desktop_mode else R.string.browser_enable_desktop_mode
                                    ),
                                    tint = if (effectiveDesktopMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                                )
                            }

                            IconButton(
                                onClick = {
                                    context.performHapticFeedback()
                                    val url = webViewInstance?.url ?: currentUrl
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.button_copy_link), url))
                                    Toast.makeText(context, R.string.link_copied, Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_copy_stroke),
                                    contentDescription = stringResource(id = R.string.browser_copy_link),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(
                                onClick = {
                                    context.performHapticFeedback()
                                    val url = webViewInstance?.url ?: currentUrl
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, url)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.browser_share)))
                                }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_share_stroke),
                                    contentDescription = stringResource(id = R.string.browser_share),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
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

private fun openExternally(context: Context, url: String) {
    if (url.isBlank()) return
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: Exception) {
        Toast.makeText(context, R.string.could_not_open_link, Toast.LENGTH_SHORT).show()
    }
}
