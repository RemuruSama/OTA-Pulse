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

package com.abhinav.otapulse.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abhinav.otapulse.core.ui.theme.OtaPulseTheme
import com.abhinav.otapulse.core.ui.theme.holographicEdge

/**
 * Frosted glass surface composable for Holographic mode.
 *
 * Simulates frosted glass by applying a semi-transparent surface tint over the background
 * and drawing an iridescent chromatic border.
 * Note: Real backdrop blur requires RenderEffect API 31+ or custom RenderNode shaders;
 * this component uses layered alpha tinting for broad backward compatibility down to API 29.
 */
@Composable
fun HolographicSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    tonalElevation: Dp = 0.dp,
    color: Color = OtaPulseTheme.extendedColors.glassPanel,
    content: @Composable () -> Unit
) {
    val config = OtaPulseTheme.holographicConfig

    Surface(
        modifier = modifier
            .holographicEdge(shape = shape, config = config),
        shape = shape,
        color = color,
        tonalElevation = tonalElevation,
        shadowElevation = 0.dp,
        content = content
    )
}
