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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import com.abhinav.otapulse.core.ui.theme.LocalReduceMotion
import com.abhinav.otapulse.core.ui.theme.OtaPulseMotion

/**
 * A wrapper that provides a staggered entry animation for its content.
 * Ideal for lists, bento grids, and section-based layouts.
 *
 * Animates with fade + slide up + scale-in for a premium stacking effect.
 */
@Composable
fun StaggeredItem(
    visible: Boolean,
    index: Int,
    content: @Composable () -> Unit
) {
    if (LocalReduceMotion.current) {
        if (visible) {
            content()
        }
        return
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = OtaPulseMotion.StackEnterDuration,
                delayMillis = OtaPulseMotion.staggerDelayForIndex(index).toInt()
            )
        ) + slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = OtaPulseMotion.StackSlideOffset
        ) + scaleIn(
            initialScale = 0.95f,
            animationSpec = OtaPulseMotion.StackScaleSpring
        ),
        exit = fadeOut(
            animationSpec = OtaPulseMotion.FadeOutSpec
        ) + androidx.compose.animation.scaleOut(
            targetScale = 0.96f,
            animationSpec = OtaPulseMotion.SpringSnappy
        )
    ) {
        content()
    }
}

/**
 * Variant of [StaggeredItem] designed for screen-level section entrance.
 * Uses slightly longer stagger for a more deliberate reveal.
 */
@Composable
fun StaggeredSection(
    visible: Boolean,
    index: Int,
    staggerDelayMs: Int = 40,
    content: @Composable () -> Unit
) {
    if (LocalReduceMotion.current) {
        if (visible) {
            content()
        }
        return
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = OtaPulseMotion.DurationMedium,
                delayMillis = index * staggerDelayMs
            )
        ) + slideInVertically(
            initialOffsetY = { it / 5 },
            animationSpec = OtaPulseMotion.SpringGentleOffset
        ) + scaleIn(
            initialScale = 0.97f,
            animationSpec = OtaPulseMotion.SpringGentle
        ),
        exit = fadeOut(
            animationSpec = OtaPulseMotion.FadeOutSpec
        )
    ) {
        content()
    }
}

