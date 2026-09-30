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

import com.abhinav.otapulse.core.ui.components.StackAnimationPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying OtaPulseMotion spring motion specifications and physics parameters.
 */
class OtaPulseMotionTest {

    @Test
    fun `staggerDelayForIndex returns 0 for index zero or negative`() {
        assertEquals(0L, OtaPulseMotion.staggerDelayForIndex(0))
        assertEquals(0L, OtaPulseMotion.staggerDelayForIndex(-1))
    }

    @Test
    fun `staggerDelayForIndex returns base delay for index 1`() {
        assertEquals(16L, OtaPulseMotion.staggerDelayForIndex(1))
    }

    @Test
    fun `staggerDelayForIndex implements diminishing returns curve`() {
        val delay1 = OtaPulseMotion.staggerDelayForIndex(1)
        val delay2 = OtaPulseMotion.staggerDelayForIndex(2)
        val delay3 = OtaPulseMotion.staggerDelayForIndex(3)

        val delta1 = delay2 - delay1
        val delta2 = delay3 - delay2

        // Second step delay increment should be smaller than first step
        assertTrue("delta1 should be <= 16L", delta1 <= 16L)
        assertTrue("delta2 should be <= delta1 (diminishing curve)", delta2 <= delta1)
    }

    @Test
    fun `staggerDelayForIndex respects maxItems clamp`() {
        val delayAtMax = OtaPulseMotion.staggerDelayForIndex(OtaPulseMotion.StaggerMaxItems)
        val delayBeyondMax = OtaPulseMotion.staggerDelayForIndex(OtaPulseMotion.StaggerMaxItems + 5)
        assertEquals(delayAtMax, delayBeyondMax)
    }

    @Test
    fun `stack press spring has higher stiffness than release spring for tactile response`() {
        // Press should be stiff and immediate (stiffness = 850f)
        // Release should be elastic and bouncy (stiffness = 520f)
        assertTrue(
            "Press spring stiffness should exceed release stiffness",
            OtaPulseMotion.StackPressSpring.stiffness > OtaPulseMotion.StackReleaseSpring.stiffness
        )
    }

    @Test
    fun `stack release spring has playful bouncy damping`() {
        // Lower damping ratio produces playful elastic rebound
        assertTrue(
            "Release spring damping should be < 0.7f for bounce",
            OtaPulseMotion.StackReleaseSpring.dampingRatio < 0.7f
        )
    }

    @Test
    fun `stack slide and scale springs are calibrated for natural settling`() {
        // Damping around 0.70f - 0.75f gives organic overshoot with smooth settling
        assertTrue(OtaPulseMotion.StackSlideSpring.dampingRatio in 0.70f..0.75f)
        assertTrue(OtaPulseMotion.StackScaleSpring.dampingRatio in 0.68f..0.75f)
    }

    @Test
    fun `stack swipe spring is tuned for tactile gesture snap`() {
        assertTrue(OtaPulseMotion.StackSwipeSpring.dampingRatio in 0.60f..0.70f)
        assertTrue(OtaPulseMotion.StackSwipeSpring.stiffness in 500f..700f)
    }

    @Test
    fun `stack animation presets include all required styles`() {
        val presets = StackAnimationPreset.values()
        assertTrue(presets.contains(StackAnimationPreset.Natural))
        assertTrue(presets.contains(StackAnimationPreset.Deck3D))
        assertTrue(presets.contains(StackAnimationPreset.Snappy))
        assertTrue(presets.contains(StackAnimationPreset.Bouncy))
        assertTrue(presets.contains(StackAnimationPreset.Gentle))
    }
}
