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

import android.provider.Settings
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset

/**
 * Whether the user has disabled animations via Accessibility > Remove Animations
 * or Developer Options > Animator duration scale = 0.
 *
 * When `true`, all motion specs should resolve to instant/snap animations.
 */
val LocalReduceMotion = compositionLocalOf { false }

/**
 * Provides [LocalReduceMotion] to the composition tree by reading
 * `Settings.Global.ANIMATOR_DURATION_SCALE`. Wrap your root composable
 * with this to enable Reduce Motion awareness throughout the app.
 */
@Composable
fun MotionProvider(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val reduceMotion = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }
    CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
        content()
    }
}

/**
 * Resolves this [AnimationSpec] to [androidx.compose.animation.core.snap] if [LocalReduceMotion] is enabled.
 */
@Composable
fun <T> androidx.compose.animation.core.AnimationSpec<T>.orSnap(): androidx.compose.animation.core.AnimationSpec<T> =
    if (LocalReduceMotion.current) androidx.compose.animation.core.snap() else this

/**
 * Resolves this [FiniteAnimationSpec] to [androidx.compose.animation.core.snap] if [LocalReduceMotion] is enabled.
 */
@Composable
fun <T> androidx.compose.animation.core.FiniteAnimationSpec<T>.orSnap(): androidx.compose.animation.core.FiniteAnimationSpec<T> =
    if (LocalReduceMotion.current) androidx.compose.animation.core.snap() else this

/**
 * Spring-based motion specifications for OTA Pulse.
 *
 * Adheres to 2026 trending design direction by favoring bouncy, natural spring physics
 * over linear or basic easing curves.
 */
object OtaPulseMotion {

    // ── Spring Specs (Float) ──────────────────────────────────────────────

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

    /** Snappy micro-interaction spring — fast with slight overshoot. */
    val SpringSnappy = spring<Float>(
        dampingRatio = 0.65f,
        stiffness = 1000f
    )

    /** Silky smooth spring for sheet drags and large gestures. */
    val SpringSilky = spring<Float>(
        dampingRatio = 0.9f,
        stiffness = 150f
    )

    // ── Spring Specs (Typed Overloads) ────────────────────────────────────

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

    // ── Shared Element / Predictive Back ──────────────────────────────────

    val SharedElementSpec = spring<Float>(
        dampingRatio = 0.85f,
        stiffness = 350f
    )

    val PredictiveBackSpec = spring<Float>(
        dampingRatio = 0.9f,
        stiffness = 600f
    )

    // ── Navigation Transitions ────────────────────────────────────────────

    /**
     * Fast, responsive spring for navigation push/pop slide transitions.
     * High stiffness (1000f) eliminates initial input lag/delay, while high damping (0.90f)
     * settles cleanly in ~220ms without prolonged oscillation or sluggish tails.
     */
    val NavSlideSpring = spring<IntOffset>(
        dampingRatio = 0.90f,
        stiffness = 1000f
    )

    /**
     * Scale spring for navigation depth transitions (0.96f <-> 1.0f).
     */
    val NavScaleSpring = spring<Float>(
        dampingRatio = 0.90f,
        stiffness = 1000f
    )

    /** Synchronized fade transitions for navigation push/pop. */
    val NavFadeInSpec = tween<Float>(durationMillis = 200, easing = FastOutSlowInEasing)
    val NavFadeOutSpec = tween<Float>(durationMillis = 180, easing = FastOutSlowInEasing)

    // ── Content Transitions (Tween) ──────────────────────────────────────

    val FadeInSpec = tween<Float>(durationMillis = 200, easing = EaseOut)
    val FadeOutSpec = tween<Float>(durationMillis = 150, easing = EaseIn)

    // ── Standardized Duration Tokens ─────────────────────────────────────

    /** Quick micro-interaction (press feedback, icon swap). */
    const val DurationShort = 150

    /** Standard transition (content swap, navigation). */
    const val DurationMedium = 300

    /** Elaborate transition (sheet reveal, complex entrance). */
    const val DurationLong = 450

    // ── Stack List Animation Specs ────────────────────────────────────────

    /**
     * Slide spring for stack items entering view.
     * High-FPS optimized: stiffness 500f and damping 0.74f provide swift,
     * fluid overshoot that settles cleanly within 200ms at 120Hz/144Hz.
     */
    val StackSlideSpring = spring<Float>(
        dampingRatio = 0.74f,
        stiffness = 500f
    )

    /**
     * Scale spring for stack items popping and settling into their bounds.
     */
    val StackScaleSpring = spring<Float>(
        dampingRatio = 0.72f,
        stiffness = 520f
    )

    /**
     * Rapid alpha spring for clean opacity transition without ghosting.
     */
    val StackAlphaSpring = spring<Float>(
        dampingRatio = 0.90f,
        stiffness = 700f
    )

    /**
     * 3D tilt spring for subtle spatial rotation settling.
     */
    val StackTiltSpring = spring<Float>(
        dampingRatio = 0.76f,
        stiffness = 520f
    )

    /**
     * Responsive touch compression spring when user presses an item.
     * High stiffness (850f) provides immediate tactile feedback.
     */
    val StackPressSpring = spring<Float>(
        dampingRatio = 0.68f,
        stiffness = 850f
    )

    /**
     * Bouncy rebound spring when user releases a pressed item.
     * Lower damping ratio (0.58f) produces a lively, playful snap back.
     */
    val StackReleaseSpring = spring<Float>(
        dampingRatio = 0.58f,
        stiffness = 520f
    )

    /**
     * Spring for list/grid items reshuffling or shifting after deletion/reordering.
     */
    val StackReorderSpec = spring<IntOffset>(
        dampingRatio = 0.78f,
        stiffness = 550f
    )

    /**
     * Bouncy tactile snap spring for swipe gestures (e.g. swipe-to-delete).
     */
    val StackSwipeSpring = spring<Float>(
        dampingRatio = 0.65f,
        stiffness = 580f
    )

    // ── Stack Typed Overloads ─────────────────────────────────────────────

    val StackSlideDp = spring<Dp>(
        dampingRatio = 0.74f,
        stiffness = 500f
    )

    val StackSlideOffset = spring<IntOffset>(
        dampingRatio = 0.74f,
        stiffness = 500f
    )

    val SpringGentleOffset = spring<IntOffset>(
        dampingRatio = 0.8f,
        stiffness = 200f
    )

    val StackPressDp = spring<Dp>(
        dampingRatio = 0.68f,
        stiffness = 850f
    )

    val StackReleaseDp = spring<Dp>(
        dampingRatio = 0.58f,
        stiffness = 520f
    )

    val StackSizeSpring = spring<androidx.compose.ui.unit.IntSize>(
        dampingRatio = 0.75f,
        stiffness = 450f
    )

    /** Enter duration for list items (legacy fallback). */
    const val StackEnterDuration = 220

    /** Exit duration for list items. */
    const val StackExitDuration = 180

    /** Press animation duration feel. */
    const val StackPressDuration = 120

    /** Stagger delay between list items (ms), synchronized with 120Hz frame pacing. */
    const val StaggerDelayMs = 16

    /** Max items to stagger (items beyond this appear without extra delay). */
    const val StaggerMaxItems = 12

    /** Standard entrance spring for list items. */
    val StackEnterSpringSpec = spring<Float>(
        dampingRatio = 0.74f,
        stiffness = 500f
    )

    /** Standard entrance easing for list items (tween fallback). */
    val StackEnterSpec = tween<Float>(
        durationMillis = StackEnterDuration,
        easing = FastOutSlowInEasing
    )

    /**
     * Computes a natural diminishing stagger delay (in milliseconds) for item at [index].
     * Uses a decaying interval curve so initial items pop quickly and later items do not
     * create a prolonged wait.
     */
    fun staggerDelayForIndex(
        index: Int,
        maxItems: Int = StaggerMaxItems,
        baseDelayMs: Long = StaggerDelayMs.toLong()
    ): Long {
        if (index <= 0) return 0L
        val clampedIndex = index.coerceAtMost(maxItems)
        var total = 0L
        for (i in 0 until clampedIndex) {
            val decayFactor = (1.0 - (i * 0.08)).coerceAtLeast(0.45)
            total += (baseDelayMs * decayFactor).toLong()
        }
        return total
    }
}

