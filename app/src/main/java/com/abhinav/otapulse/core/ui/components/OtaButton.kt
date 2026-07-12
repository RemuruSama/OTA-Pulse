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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abhinav.otapulse.core.ui.theme.HoloGradientStops
import com.abhinav.otapulse.core.ui.theme.OtaPulseMotion
import com.abhinav.otapulse.core.ui.theme.OtaPulseTheme
import com.abhinav.otapulse.core.ui.theme.holographicEdge

@Composable
private fun rememberPressScale(interactionSource: MutableInteractionSource): Float {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = OtaPulseMotion.SpringStiff,
        label = "press_scale"
    )
    return scale
}

@Composable
private fun ButtonContent(
    text: String,
    icon: ImageVector?,
    compact: Boolean,
    isLoading: Boolean = false
) {
    if (isLoading) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WavyProgressIndicator(
                modifier = Modifier.size(if (compact) 16.dp else 18.dp),
                color = LocalContentColor.current,
                strokeWidth = 2.2.dp,
                size = if (compact) 16.dp else 18.dp
            )
            Spacer(modifier = Modifier.width(if (compact) 8.dp else 10.dp))
            Text(
                text = text,
                style = (if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleSmall).copy(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    } else if (icon != null) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(if (compact) 16.dp else 18.dp)
            )
            Spacer(modifier = Modifier.width(if (compact) 6.dp else 8.dp))
            Text(
                text = text,
                style = (if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleSmall).copy(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    } else {
        Text(
            text = text,
            style = (if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleSmall).copy(
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Material 3 Expressive Primary Button with bouncy spring touch physics.
 * In Holographic mode, displays an iridescent gradient background.
 */
@Composable
fun OtaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    compact: Boolean = false,
    isLoading: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource)
    val isHolo = OtaPulseTheme.holographicConfig.isEnabled
    val minHeight = if (compact) 42.dp else 52.dp
    val hPad = if (compact) 16.dp else 24.dp
    val vPad = if (compact) 10.dp else 14.dp

    if (isHolo && enabled) {
        val gradientBrush = remember { Brush.horizontalGradient(HoloGradientStops) }
        Box(
            modifier = modifier
                .scale(scale)
                .heightIn(min = minHeight)
                .clip(MaterialTheme.shapes.medium)
                .background(gradientBrush)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    enabled = enabled && !isLoading,
                    onClick = onClick
                )
                .padding(horizontal = hPad, vertical = vPad),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides Color.Black) {
                ButtonContent(text = text, icon = icon, compact = compact, isLoading = isLoading)
            }
        }
    } else {
        Button(
            onClick = { if (!isLoading) onClick() },
            modifier = modifier.scale(scale).heightIn(min = minHeight),
            enabled = enabled,
            interactionSource = interactionSource,
            shape = MaterialTheme.shapes.medium,
            contentPadding = PaddingValues(horizontal = hPad, vertical = vPad)
        ) {
            ButtonContent(text = text, icon = icon, compact = compact, isLoading = isLoading)
        }
    }
}

/**
 * Outlined Button variant with spring touch physics.
 */
@Composable
fun OtaOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    compact: Boolean = false,
    isLoading: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource)
    val isHolo = OtaPulseTheme.holographicConfig.isEnabled
    val minHeight = if (compact) 42.dp else 52.dp
    val hPad = if (compact) 16.dp else 24.dp
    val vPad = if (compact) 10.dp else 14.dp

    if (isHolo && enabled) {
        OutlinedButton(
            onClick = { if (!isLoading) onClick() },
            modifier = modifier
                .scale(scale)
                .heightIn(min = minHeight)
                .holographicEdge(shape = MaterialTheme.shapes.medium, config = OtaPulseTheme.holographicConfig),
            enabled = enabled,
            interactionSource = interactionSource,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = OtaPulseTheme.extendedColors.glassPanel.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            contentPadding = PaddingValues(horizontal = hPad, vertical = vPad)
        ) {
            ButtonContent(text = text, icon = icon, compact = compact, isLoading = isLoading)
        }
    } else {
        OutlinedButton(
            onClick = { if (!isLoading) onClick() },
            modifier = modifier.scale(scale).heightIn(min = minHeight),
            enabled = enabled,
            interactionSource = interactionSource,
            shape = MaterialTheme.shapes.medium,
            contentPadding = PaddingValues(horizontal = hPad, vertical = vPad)
        ) {
            ButtonContent(text = text, icon = icon, compact = compact, isLoading = isLoading)
        }
    }
}

/**
 * Filled Tonal Button variant with spring touch physics.
 */
@Composable
fun OtaTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    compact: Boolean = false,
    isLoading: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource)
    val isHolo = OtaPulseTheme.holographicConfig.isEnabled
    val minHeight = if (compact) 42.dp else 52.dp
    val hPad = if (compact) 16.dp else 24.dp
    val vPad = if (compact) 10.dp else 14.dp

    if (isHolo && enabled) {
        FilledTonalButton(
            onClick = { if (!isLoading) onClick() },
            modifier = modifier
                .scale(scale)
                .heightIn(min = minHeight)
                .holographicEdge(shape = MaterialTheme.shapes.medium, config = OtaPulseTheme.holographicConfig),
            enabled = enabled,
            interactionSource = interactionSource,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = OtaPulseTheme.extendedColors.glassPanel.copy(alpha = 0.7f),
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            contentPadding = PaddingValues(horizontal = hPad, vertical = vPad)
        ) {
            ButtonContent(text = text, icon = icon, compact = compact, isLoading = isLoading)
        }
    } else {
        FilledTonalButton(
            onClick = { if (!isLoading) onClick() },
            modifier = modifier.scale(scale).heightIn(min = minHeight),
            enabled = enabled,
            interactionSource = interactionSource,
            shape = MaterialTheme.shapes.medium,
            contentPadding = PaddingValues(horizontal = hPad, vertical = vPad)
        ) {
            ButtonContent(text = text, icon = icon, compact = compact, isLoading = isLoading)
        }
    }
}
