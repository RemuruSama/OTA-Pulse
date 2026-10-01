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
import androidx.compose.animation.slideOutVertically
import com.abhinav.otapulse.core.ui.theme.OtaPulseMotion

/**
 * Shared animation definitions for navigation transitions in OTA Pulse.
 *
 * Bottom Nav tabs use **Fade Through** (Material Motion pattern):
 *   outgoing screen fades out + scales down → incoming screen fades in + scales up.
 *   This creates a peer-to-peer feel without directional sliding.
 *
 * Push/Pop navigation uses **Shared Axis Z** with depth cues:
 *   push adds a subtle scale-down to the exiting screen for depth perception.
 *
 * Sheet transitions use slide-up + fade for modal bottom sheets.
 */
object NavigationAnimations {

    val topLevelRoutes = setOf(
        Screen.HomeUpdate.route,
        Screen.DeviceCatalog.route,
        Screen.OtaTools.route,
        Screen.About.route,
        Screen.Settings.route
    )

    // ── Push Navigation (Secondary Screens) ───────────────────────────────

    fun defaultEnterTransition(): EnterTransition {
        return fadeIn(animationSpec = OtaPulseMotion.NavFadeInSpec) +
                slideInHorizontally(animationSpec = OtaPulseMotion.NavSlideSpring) { fullWidth -> fullWidth }
    }

    fun defaultExitTransition(): ExitTransition {
        return fadeOut(animationSpec = OtaPulseMotion.NavFadeOutSpec) +
                slideOutHorizontally(animationSpec = OtaPulseMotion.NavSlideSpring) { fullWidth -> -fullWidth / 4 } +
                scaleOut(targetScale = 0.96f, animationSpec = OtaPulseMotion.NavScaleSpring)
    }

    fun defaultPopEnterTransition(): EnterTransition {
        return fadeIn(animationSpec = OtaPulseMotion.NavFadeInSpec) +
                slideInHorizontally(animationSpec = OtaPulseMotion.NavSlideSpring) { fullWidth -> -fullWidth / 4 } +
                scaleIn(initialScale = 0.96f, animationSpec = OtaPulseMotion.NavScaleSpring)
    }

    fun defaultPopExitTransition(): ExitTransition {
        return fadeOut(animationSpec = OtaPulseMotion.NavFadeOutSpec) +
                slideOutHorizontally(animationSpec = OtaPulseMotion.NavSlideSpring) { fullWidth -> fullWidth }
    }

    // ── Bottom Nav (Fade Through) ─────────────────────────────────────────

    /**
     * Fade Through enter: incoming screen fades in from alpha=0 and scales
     * up from 0.96f — creating a fluid forward transition without latency.
     */
    fun bottomNavEnterTransition(): EnterTransition {
        return fadeIn(
            animationSpec = OtaPulseMotion.NavFadeInSpec
        ) + scaleIn(
            initialScale = 0.96f,
            animationSpec = OtaPulseMotion.NavScaleSpring
        )
    }

    /**
     * Fade Through exit: outgoing screen fades out and scales down to 0.96f
     * — creating a fluid step-back transition without delay.
     */
    fun bottomNavExitTransition(): ExitTransition {
        return fadeOut(
            animationSpec = OtaPulseMotion.NavFadeOutSpec
        ) + scaleOut(
            targetScale = 0.96f,
            animationSpec = OtaPulseMotion.NavScaleSpring
        )
    }

    // ── Context-Aware Top-Level Transitions ───────────────────────────────

    fun topLevelEnterTransition(initialRoute: String?): EnterTransition =
        if (initialRoute in topLevelRoutes) bottomNavEnterTransition() else defaultEnterTransition()

    fun topLevelExitTransition(targetRoute: String?): ExitTransition =
        if (targetRoute in topLevelRoutes) bottomNavExitTransition() else defaultExitTransition()

    fun topLevelPopEnterTransition(initialRoute: String?): EnterTransition =
        if (initialRoute in topLevelRoutes) bottomNavEnterTransition() else defaultPopEnterTransition()

    fun topLevelPopExitTransition(targetRoute: String?): ExitTransition =
        if (targetRoute in topLevelRoutes) bottomNavExitTransition() else defaultPopExitTransition()

    // ── Sheet Transitions ─────────────────────────────────────────────────

    fun sheetEnterTransition(): EnterTransition {
        return fadeIn(
            animationSpec = OtaPulseMotion.NavFadeInSpec
        ) + slideInVertically(
            animationSpec = OtaPulseMotion.NavSlideSpring
        ) { it / 3 }
    }

    fun sheetExitTransition(): ExitTransition {
        return fadeOut(
            animationSpec = OtaPulseMotion.NavFadeOutSpec
        ) + slideOutVertically(
            animationSpec = OtaPulseMotion.NavSlideSpring
        ) { it / 3 }
    }
}

