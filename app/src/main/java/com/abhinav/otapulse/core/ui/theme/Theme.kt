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
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

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
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val (colorScheme, extendedColors, holoConfig) = when (themeMode) {
        ThemeMode.MATERIAL_YOU -> {
            val baseScheme = when {
                dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                }
                darkTheme -> otaPulseDarkColorScheme()
                else -> otaPulseLightColorScheme()
            }

            val finalScheme = if (darkTheme && amoledDark) {
                baseScheme.copy(
                    background = Color.Black,
                    surface = Color.Black
                )
            } else {
                baseScheme
            }

            val ext = if (darkTheme) {
                if (amoledDark) DarkExtendedColors.copy(glassPanel = Color.Black.copy(alpha = 0.8f)) else DarkExtendedColors
            } else {
                LightExtendedColors
            }

            Triple(finalScheme, ext, HolographicConfig(isEnabled = false))
        }
        ThemeMode.HOLOGRAPHIC -> {
            val baseScheme = holographicColorScheme()
            val finalScheme = if (amoledDark) {
                baseScheme.copy(
                    background = Color.Black,
                    surface = Color.Black
                )
            } else {
                baseScheme
            }

            val ext = if (amoledDark) {
                HoloExtendedColors.copy(glassPanel = Color.Black.copy(alpha = 0.75f))
            } else {
                HoloExtendedColors
            }

            Triple(
                finalScheme,
                ext,
                HolographicConfig(isEnabled = true)
            )
        }
    }

    CompositionLocalProvider(
        LocalOtaPulseExtendedColors provides extendedColors,
        LocalHolographicConfig provides holoConfig
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = OtaPulseTypography,
            shapes = OtaPulseShapes,
            content = content
        )
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
