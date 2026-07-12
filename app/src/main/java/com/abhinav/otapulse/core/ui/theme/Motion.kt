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

import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset

/**
 * Spring-based motion specifications for OTA Pulse.
 *
 * Adheres to 2026 trending design direction by favoring bouncy, natural spring physics
 * over linear or basic easing curves.
 */
object OtaPulseMotion {
    val SpringStiff = spring<Float>(
        dampingRatio = 0.7f,
        stiffness = 800f
    )
    
    val SpringMedium = spring<Float>(
        dampingRatio = 0.75f,
        stiffness = 400f
    )
    
    val SpringGentle = spring<Float>(
        dampingRatio = 0.8f,
        stiffness = 200f
    )
    
    val SpringBouncy = spring<Float>(
        dampingRatio = 0.5f,
        stiffness = 500f
    )

    // Overloads for different types
    val SpringMediumDp = spring<Dp>(
        dampingRatio = 0.75f,
        stiffness = 400f
    )

    val SpringMediumOffset = spring<IntOffset>(
        dampingRatio = 0.75f,
        stiffness = 400f
    )

    val SpringMediumSize = spring<androidx.compose.ui.unit.IntSize>(
        dampingRatio = 0.75f,
        stiffness = 400f
    )

    // For shared element transitions
    val SharedElementSpec = spring<Float>(
        dampingRatio = 0.85f,
        stiffness = 350f
    )

    // Predictive back gesture animation spec
    val PredictiveBackSpec = spring<Float>(
        dampingRatio = 0.9f,
        stiffness = 600f
    )

    // Content transitions
    val FadeInSpec = tween<Float>(durationMillis = 200, easing = EaseOut)
    val FadeOutSpec = tween<Float>(durationMillis = 150, easing = EaseIn)

    // Stagger delay for bento grid and lists
    const val StaggerDelayMs = 40
}
