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

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Material You Static Fallback Palette (Seed #8B1A1A) ─────────────────────

private val LightPrimary = Color(0xFFBA1A1A)
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightPrimaryContainer = Color(0xFFFFDAD6)
private val LightOnPrimaryContainer = Color(0xFF410002)
private val LightSecondary = Color(0xFF775652)
private val LightOnSecondary = Color(0xFFFFFFFF)
private val LightSecondaryContainer = Color(0xFFFFDAD6)
private val LightOnSecondaryContainer = Color(0xFF2C1512)
private val LightTertiary = Color(0xFF755B2E)
private val LightOnTertiary = Color(0xFFFFFFFF)
private val LightTertiaryContainer = Color(0xFFFFDEA6)
private val LightOnTertiaryContainer = Color(0xFF281900)
private val LightError = Color(0xFFBA1A1A)
private val LightOnError = Color(0xFFFFFFFF)
private val LightErrorContainer = Color(0xFFFFDAD6)
private val LightOnErrorContainer = Color(0xFF410002)
private val LightBackground = Color(0xFFFFFBFF)
private val LightOnBackground = Color(0xFF201A19)
private val LightSurface = Color(0xFFFFFBFF)
private val LightOnSurface = Color(0xFF201A19)
private val LightSurfaceVariant = Color(0xFFF5DDDA)
private val LightOnSurfaceVariant = Color(0xFF534341)
private val LightOutline = Color(0xFF857370)
private val LightOutlineVariant = Color(0xFFD8C2BF)

private val DarkPrimary = Color(0xFFFFB4AB)
private val DarkOnPrimary = Color(0xFF690005)
private val DarkPrimaryContainer = Color(0xFF93000A)
private val DarkOnPrimaryContainer = Color(0xFFFFDAD6)
private val DarkSecondary = Color(0xFFE7BDB8)
private val DarkOnSecondary = Color(0xFF442926)
private val DarkSecondaryContainer = Color(0xFF5D3F3B)
private val DarkOnSecondaryContainer = Color(0xFFFFDAD6)
private val DarkTertiary = Color(0xFFE5C18D)
private val DarkOnTertiary = Color(0xFF3F2E04)
private val DarkTertiaryContainer = Color(0xFF584419)
private val DarkOnTertiaryContainer = Color(0xFFFFDEA6)
private val DarkError = Color(0xFFFFB4AB)
private val DarkOnError = Color(0xFF690005)
private val DarkErrorContainer = Color(0xFF93000A)
private val DarkOnErrorContainer = Color(0xFFFFDAD6)
private val DarkBackground = Color(0xFF201A19)
private val DarkOnBackground = Color(0xFFEDE0DE)
private val DarkSurface = Color(0xFF201A19)
private val DarkOnSurface = Color(0xFFEDE0DE)
private val DarkSurfaceVariant = Color(0xFF534341)
private val DarkOnSurfaceVariant = Color(0xFFD8C2BF)
private val DarkOutline = Color(0xFFA08C8A)
private val DarkOutlineVariant = Color(0xFF534341)

// ── Holographic Glassmorphism Palette ───────────────────────────────────────

val HoloPrimary = Color(0xFF7B61FF)       // Vivid Purple
val HoloSecondary = Color(0xFF00E5FF)     // Electric Cyan
val HoloTertiary = Color(0xFFFF6FD8)      // Hot Pink
val HoloAccent = Color(0xFF39FF14)        // Neon Green (sparingly)
val HoloSurface = Color(0xFF0D0D1A)       // Deep space dark
val HoloSurfaceVariant = Color(0xFF1A1A2E)// Slightly lighter space
val HoloOnSurface = Color(0xFFE8E6F0)     // Soft white
val HoloOnSurfaceVariant = Color(0xFFA0A0B8) // Muted lavender
val HoloOutline = Color(0xFF3E3E5E)
val HoloOutlineVariant = Color(0xFF2A2A40)

val HoloGradientStops = listOf(HoloPrimary, HoloSecondary, HoloTertiary)
val HoloFrostedOverlay = Color.White.copy(alpha = 0.06f)

// ── Color Schemes ───────────────────────────────────────────────────────────

fun otaPulseLightColorScheme(): ColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant
)

fun otaPulseDarkColorScheme(): ColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant
)

fun holographicColorScheme(): ColorScheme = darkColorScheme(
    primary = HoloPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3E2D8F),
    onPrimaryContainer = Color(0xFFE5DEFF),
    secondary = HoloSecondary,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF005863),
    onSecondaryContainer = Color(0xFF9FF0FF),
    tertiary = HoloTertiary,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF7D1B64),
    onTertiaryContainer = Color(0xFFFFD8ED),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = HoloSurface,
    onBackground = HoloOnSurface,
    surface = HoloSurface,
    onSurface = HoloOnSurface,
    surfaceVariant = HoloSurfaceVariant,
    onSurfaceVariant = HoloOnSurfaceVariant,
    outline = HoloOutline,
    outlineVariant = HoloOutlineVariant
)

// ── Extended Semantic Colors ────────────────────────────────────────────────

@Immutable
data class OtaPulseExtendedColors(
    val successContainer: Color,
    val onSuccessContainer: Color,
    val arbSafe: Color,
    val arbWarning: Color,
    val arbDanger: Color,
    val glassPanel: Color,
    val isHolographic: Boolean
)

val LightExtendedColors = OtaPulseExtendedColors(
    successContainer = Color(0xFFD7F5DD),
    onSuccessContainer = Color(0xFF003816),
    arbSafe = Color(0xFF2E7D32),
    arbWarning = Color(0xFFED6C02),
    arbDanger = Color(0xFFD32F2F),
    glassPanel = Color(0xFFFFFFFF).copy(alpha = 0.7f),
    isHolographic = false
)

val DarkExtendedColors = OtaPulseExtendedColors(
    successContainer = Color(0xFF005223),
    onSuccessContainer = Color(0xFF8DF7A6),
    arbSafe = Color(0xFF4CAF50),
    arbWarning = Color(0xFFFF9800),
    arbDanger = Color(0xFFF44336),
    glassPanel = Color(0xFF201A19).copy(alpha = 0.7f),
    isHolographic = false
)

val HoloExtendedColors = OtaPulseExtendedColors(
    successContainer = Color(0xFF004D34),
    onSuccessContainer = Color(0xFF6EFFC8),
    arbSafe = HoloAccent,
    arbWarning = Color(0xFFFFAB00),
    arbDanger = Color(0xFFFF3D00),
    glassPanel = HoloSurfaceVariant.copy(alpha = 0.65f),
    isHolographic = true
)

val LocalOtaPulseExtendedColors = staticCompositionLocalOf { LightExtendedColors }
