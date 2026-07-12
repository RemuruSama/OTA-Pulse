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

import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Configuration for the holographic glassmorphism visual system.
 */
@Immutable
data class HolographicConfig(
    val frostedBlurRadius: Dp = 24.dp,
    val chromaticEdgeWidth: Dp = 0.5.dp,
    val iridescenceIntensity: Float = 0.7f,
    val surfaceAlpha: Float = 0.08f,
    val edgeGradientStops: List<Color> = HoloGradientStops,
    val glowRadius: Dp = 8.dp,
    val isEnabled: Boolean = false
)

val LocalHolographicConfig = staticCompositionLocalOf {
    HolographicConfig()
}

/**
 * Creates a sweep gradient brush for chromatic borders in Holographic mode.
 */
fun holographicEdgeBrush(config: HolographicConfig = HolographicConfig()): Brush {
    val stops = if (config.edgeGradientStops.isNotEmpty()) {
        config.edgeGradientStops + config.edgeGradientStops.first()
    } else {
        listOf(HoloPrimary, HoloSecondary, HoloTertiary, HoloPrimary)
    }
    return Brush.sweepGradient(colors = stops)
}

/**
 * Creates a subtle diagonal gradient brush for screen backgrounds in Holographic mode.
 */
fun holographicBackgroundBrush(): Brush {
    return Brush.linearGradient(
        colors = listOf(
            HoloSurface,
            Color(0xFF131324),
            HoloSurface
        )
    )
}

/**
 * Modifier extension to apply a chromatic holographic border if enabled.
 */
fun Modifier.holographicEdge(
    shape: Shape,
    config: HolographicConfig
): Modifier = composed {
    if (config.isEnabled) {
        this
            .border(
                width = config.chromaticEdgeWidth * 2,
                brush = holographicEdgeBrush(config),
                shape = shape
            )
            .clip(shape)
    } else {
        this
    }
}
