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

package com.abhinav.otapulse.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import com.abhinav.otapulse.core.ui.theme.OtaPulseMotion

/**
 * Shared animation definitions for navigation transitions in OTA Pulse.
 * Uses spring physics for snappy, fluid screen changes.
 */
object NavigationAnimations {

    fun defaultEnterTransition(): EnterTransition {
        return fadeIn(animationSpec = OtaPulseMotion.FadeInSpec) +
                slideInHorizontally(animationSpec = OtaPulseMotion.SpringMediumOffset) { fullWidth -> fullWidth }
    }

    fun defaultExitTransition(): ExitTransition {
        return fadeOut(animationSpec = OtaPulseMotion.FadeOutSpec) +
                slideOutHorizontally(animationSpec = OtaPulseMotion.SpringMediumOffset) { fullWidth -> -fullWidth }
    }

    fun defaultPopEnterTransition(): EnterTransition {
        return fadeIn(animationSpec = OtaPulseMotion.FadeInSpec) +
                slideInHorizontally(animationSpec = OtaPulseMotion.SpringMediumOffset) { fullWidth -> -fullWidth }
    }

    fun defaultPopExitTransition(): ExitTransition {
        return fadeOut(animationSpec = OtaPulseMotion.FadeOutSpec) +
                slideOutHorizontally(animationSpec = OtaPulseMotion.SpringMediumOffset) { fullWidth -> fullWidth }
    }

    fun bottomNavEnterTransition(): EnterTransition {
        return fadeIn(animationSpec = tween(300)) +
                scaleIn(initialScale = 0.98f, animationSpec = OtaPulseMotion.SpringMedium) +
                slideInVertically(animationSpec = OtaPulseMotion.SpringMediumOffset) { it / 40 }
    }

    fun bottomNavExitTransition(): ExitTransition {
        return fadeOut(animationSpec = tween(200)) +
                scaleOut(targetScale = 0.98f, animationSpec = OtaPulseMotion.SpringMedium)
    }
}
