/*
 * Copyright (C) 2026 OTA Pulse
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.abhinav.otapulse.feature.browser

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.abhinav.otapulse.R
import com.abhinav.otapulse.core.preferences.ThemePreferences
import com.abhinav.otapulse.core.ui.theme.MotionProvider
import com.abhinav.otapulse.core.ui.theme.OtaPulseTheme
import com.abhinav.otapulse.feature.browser.ui.InAppBrowserScreen
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class InAppBrowserActivity : AppCompatActivity() {

    @Inject
    lateinit var themePreferences: ThemePreferences

    private var activeWebView: WebView? = null

    private val initialUrl: String
        get() = intent.getStringExtra(EXTRA_URL).orEmpty()

    private val initialTitle: String?
        get() = intent.getStringExtra(EXTRA_TITLE)

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        val themeSettings = themePreferences.getThemeSettings()
        AppCompatDelegate.setDefaultNightMode(themeSettings.nightMode)
        if (themeSettings.amoledDark && isNightModeActive()) {
            theme.applyStyle(R.style.ThemeOverlay_OTAPulse_Amoled, true)
        }

        setContent {
            val currentSettings by themePreferences.themeSettingsFlow.collectAsState(
                initial = themePreferences.getThemeSettings()
            )

            val isDark = when (currentSettings.nightMode) {
                AppCompatDelegate.MODE_NIGHT_NO -> false
                AppCompatDelegate.MODE_NIGHT_YES -> true
                else -> isSystemInDarkTheme()
            }

            OtaPulseTheme(
                themeMode = currentSettings.themeMode,
                darkTheme = isDark,
                amoledDark = currentSettings.amoledDark,
                dynamicColor = currentSettings.dynamicColor,
                seedColor = Color(currentSettings.seedColor),
                paletteStyle = currentSettings.paletteStyle
            ) {
                MotionProvider {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        InAppBrowserScreen(
                            initialUrl = initialUrl,
                            initialTitle = initialTitle,
                            isDark = isDark,
                            savedInstanceState = savedInstanceState,
                            onWebViewCreated = { webView ->
                                activeWebView = webView
                            },
                            onFinish = { finish() }
                        )
                    }
                }
            }
        }
    }

    private fun isNightModeActive(): Boolean {
        val uiMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return uiMode == Configuration.UI_MODE_NIGHT_YES
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
