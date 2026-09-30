/*
 * Copyright 2026 OTA Pulse
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.abhinav.otapulse.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.abhinav.otapulse.core.ui.theme.LocalReduceMotion
import com.abhinav.otapulse.core.ui.theme.OtaPulseMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Global session tracker to determine whether an item belongs to the initial
 * screen entrance cascade or is being scrolled into view.
 *
 * Scrolled items (items composed after the initial screen reveal window or beyond
 * the initial viewport threshold) immediately render with progress = 1f (alpha = 1f),
 * ensuring fast scrolling is 100% solid with zero blank frames or delayed pop-in.
 */
internal object StackAnimationSessionTracker {
    private val sessionTimestamps = java.util.concurrent.ConcurrentHashMap<String, Long>()

    fun isInitialScreenBatch(sessionKey: String, index: Int): Boolean {
        // Items beyond top viewport capacity (index > 7) are scrolled items
        if (index > 7) return false

        val now = android.os.SystemClock.uptimeMillis()
        val sessionStart = sessionTimestamps.compute(sessionKey) { _, existing ->
            // Cache session timestamp for 2 seconds; beyond that, treat as a fresh reveal
            if (existing != null && (now - existing) < 2000L) existing else now
        } ?: now

        // Items composed within the first 250ms of session creation belong to the initial screen waterfall
        return (now - sessionStart) <= 250L
    }
}

/**
 * Visual styling presets for stack item entrance animations.
 */
enum class StackAnimationPreset {
    /** Natural organic spring motion with subtle depth settling. */
    Natural,

    /** Emphasized card deck feel with 3D perspective pitch and tactile elevation drop. */
    Deck3D,

    /** High-stiffness spring for rapid, dense list interactions without perceptible overshoot. */
    Snappy,

    /** Playful spring with extra elastic rebound. */
    Bouncy,

    /** Softer, low-stiffness easing for relaxed modal sections. */
    Gentle
}

/**
 * Applies a premium spring motion stack appearance to list and grid items.
 *
 * Features:
 * - Fluid spring-damped translation, scale, and optional 3D perspective tilt across the WHOLE list.
 * - Diminishing-returns stagger delay for the initial viewport batch.
 * - Safe baseline opacity (0.75f) for scrolled items so fast-scrolling pages are never blank.
 * - Respects [LocalReduceMotion].
 *
 * @param index The position index of the item within the list/grid.
 * @param sessionKey An optional key (e.g. search query, tab name, filter ID) to re-trigger the cascade.
 * @param preset The [StackAnimationPreset] defining spring physics and 3D depth character.
 */
fun Modifier.stackItemAppearance(
    index: Int,
    sessionKey: String = "",
    preset: StackAnimationPreset = StackAnimationPreset.Natural
): Modifier = composed {
    if (LocalReduceMotion.current) {
        return@composed this
    }

    val isInitialBatch = remember(sessionKey) {
        StackAnimationSessionTracker.isInitialScreenBatch(sessionKey, index)
    }

    val initialSlide = when (preset) {
        StackAnimationPreset.Deck3D -> 24f
        StackAnimationPreset.Bouncy -> 22f
        StackAnimationPreset.Snappy -> 14f
        StackAnimationPreset.Gentle -> 12f
        else -> 18f
    }
    val initialScale = when (preset) {
        StackAnimationPreset.Deck3D -> 0.93f
        StackAnimationPreset.Bouncy -> 0.94f
        StackAnimationPreset.Snappy -> 0.96f
        StackAnimationPreset.Gentle -> 0.97f
        else -> 0.95f
    }
    val initialTilt = when (preset) {
        StackAnimationPreset.Deck3D -> 3.5f
        StackAnimationPreset.Natural -> 2.0f
        else -> 0f
    }

    val progress = remember(sessionKey) { Animatable(0f) }
    val density = LocalDensity.current
    val initialSlidePx = remember(density, initialSlide) { with(density) { initialSlide.dp.toPx() } }

    LaunchedEffect(sessionKey) {
        // Stagger delay only applies to initial screen reveal items (0..7).
        // Scrolled items enter with 0ms delay so their spring glide begins the moment they appear.
        if (isInitialBatch && index > 0) {
            val staggerMs = OtaPulseMotion.staggerDelayForIndex(index)
            if (staggerMs > 0L) {
                delay(staggerMs)
            }
        }

        val springSpec = when (preset) {
            StackAnimationPreset.Snappy -> spring<Float>(dampingRatio = 0.74f, stiffness = 650f)
            StackAnimationPreset.Bouncy -> spring<Float>(dampingRatio = 0.58f, stiffness = 460f)
            StackAnimationPreset.Gentle -> spring<Float>(dampingRatio = 0.86f, stiffness = 340f)
            StackAnimationPreset.Deck3D -> spring<Float>(dampingRatio = 0.74f, stiffness = 500f)
            StackAnimationPreset.Natural -> OtaPulseMotion.StackSlideSpring
        }

        progress.animateTo(1f, springSpec)
    }

    this.graphicsLayer {
        val p = progress.value
        if (p >= 1f) {
            // Identity state: complete bypass of transformations, GPU hardware fast-path
            alpha = 1f
            translationY = 0f
            scaleX = 1f
            scaleY = 1f
            rotationX = 0f
        } else {
            // Initial screen batch fades in from 0f; scrolled items start at 0.75f opacity
            // so fast-scrolling pages are 100% solid and never blank while still displaying
            // the full physical spring slide and scale pop on the whole list!
            alpha = if (isInitialBatch) {
                (p / 0.85f).coerceIn(0f, 1f)
            } else {
                (0.75f + (0.25f * p)).coerceIn(0f, 1f)
            }
            translationY = (1f - p) * initialSlidePx
            val s = initialScale + (1f - initialScale) * p
            scaleX = s
            scaleY = s
            if (initialTilt != 0f) {
                rotationX = (1f - p) * initialTilt
                cameraDistance = 12f * density.density
            }
        }
    }
}

/**
 * Adds an interactive tactile press response with spring-driven depression
 * and lively elastic rebound on touch release.
 *
 * @param interactionSource The source tracking touch interactions.
 * @param pressScale The target scale when compressed under touch.
 */
fun Modifier.pressInteraction(
    interactionSource: MutableInteractionSource,
    pressScale: Float = 0.96f
): Modifier = composed {
    if (LocalReduceMotion.current) {
        return@composed this
    }

    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressScale else 1f,
        animationSpec = if (isPressed) {
            OtaPulseMotion.StackPressSpring
        } else {
            OtaPulseMotion.StackReleaseSpring
        },
        label = "press_scale"
    )

    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 0.dp,
        animationSpec = if (isPressed) {
            OtaPulseMotion.StackPressDp
        } else {
            OtaPulseMotion.StackReleaseDp
        },
        label = "press_elevation"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
        shadowElevation = elevation.toPx()
    }
}

data class PressInteractionState(
    val interactionSource: MutableInteractionSource,
    val modifier: Modifier
)

@Composable
fun rememberPressInteraction(pressScale: Float = 0.96f): PressInteractionState {
    val interactionSource = remember { MutableInteractionSource() }
    val modifier = Modifier.pressInteraction(interactionSource, pressScale)
    return remember(interactionSource, modifier) {
        PressInteractionState(interactionSource, modifier)
    }
}

/**
 * Extension for [LazyListScope] that automatically wraps items with [stackItemAppearance]
 * and reorder placement spring physics.
 */
inline fun <T> LazyListScope.stackAnimatedItems(
    items: List<T>,
    sessionKey: String = "",
    preset: StackAnimationPreset = StackAnimationPreset.Natural,
    noinline key: ((item: T) -> Any)? = null,
    noinline contentType: (item: T) -> Any? = { null },
    crossinline itemContent: @Composable LazyItemScope.(item: T) -> Unit
) {
    items(
        count = items.size,
        key = if (key != null) { index: Int -> key(items[index]) } else null,
        contentType = { index: Int -> contentType(items[index]) }
    ) { index ->
        val item = items[index]
        Box(
            modifier = Modifier
                .stackItemAppearance(index, sessionKey, preset)
                .animateItem(
                    fadeInSpec = null,
                    fadeOutSpec = null,
                    placementSpec = OtaPulseMotion.StackReorderSpec
                )
        ) {
            itemContent(item)
        }
    }
}
