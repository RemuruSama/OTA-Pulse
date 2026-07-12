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

package com.abhinav.otapulse.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle

/**
 * Master theme composable for OTA Pulse.
 *
 * Supports switching between Material You (with dynamic color support and AMOLED dark mode)
 * and the Holographic glassmorphism theme.
 */
@Composable
fun OtaPulseTheme(
    themeMode: ThemeMode = ThemeMode.MATERIAL_YOU,
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoledDark: Boolean = false,
    dynamicColor: Boolean = true,
    seedColor: Color = Color(0xFFBA1A1A),
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val (extendedColors, holoConfig) = when (themeMode) {
        ThemeMode.MATERIAL_YOU -> {
            val ext = if (darkTheme) {
                if (amoledDark) DarkExtendedColors.copy(glassPanel = Color.Black.copy(alpha = 0.8f)) else DarkExtendedColors
            } else {
                LightExtendedColors
            }
            Pair(ext, HolographicConfig(isEnabled = false))
        }
        ThemeMode.HOLOGRAPHIC -> {
            val ext = if (amoledDark) {
                HoloExtendedColors.copy(glassPanel = Color.Black.copy(alpha = 0.75f))
            } else {
                HoloExtendedColors
            }
            Pair(ext, HolographicConfig(isEnabled = true))
        }
    }

    val finalSeedColor = remember(seedColor, dynamicColor) {
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Color(context.getColor(android.R.color.system_accent1_500))
        } else {
            seedColor
        }
    }

    CompositionLocalProvider(
        LocalOtaPulseExtendedColors provides extendedColors,
        LocalHolographicConfig provides holoConfig
    ) {
        if (themeMode == ThemeMode.HOLOGRAPHIC) {
            val baseScheme = holographicColorScheme()
            val finalScheme = if (amoledDark) {
                baseScheme.copy(
                    background = Color.Black,
                    surface = Color.Black
                )
            } else {
                baseScheme
            }
            MaterialTheme(
                colorScheme = finalScheme,
                typography = OtaPulseTypography,
                shapes = OtaPulseShapes,
                content = content
            )
        } else {
            DynamicMaterialTheme(
                seedColor = finalSeedColor,
                isDark = darkTheme,
                isAmoled = amoledDark,
                style = paletteStyle,
                typography = OtaPulseTypography,
                shapes = OtaPulseShapes,
                animate = true,
                content = content
            )
        }
    }
}

/**
 * Convenience accessor for custom theme properties.
 */
object OtaPulseTheme {
    val extendedColors: OtaPulseExtendedColors
        @Composable
        get() = LocalOtaPulseExtendedColors.current

    val holographicConfig: HolographicConfig
        @Composable
        get() = LocalHolographicConfig.current
}
