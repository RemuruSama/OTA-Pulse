package com.abhinav.otapulse.core.common

import android.view.View
import androidx.core.view.isVisible
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce

object AnimationUtils {

    /**
     * Enhanced entrance animation using physics-based Spring dynamics.
     * Each view fades in and slides up from a 24dp offset.
     *
     * @param views        Views to animate.
     * @param startDelay   Initial delay before the first view starts (ms).
     * @param staggerDelay Delay between each view's animation start (ms). Defaults to 0 for simultaneous entrance.
     */
    fun animateEntrance(views: List<View>, startDelay: Long = 0, staggerDelay: Long = 0) {
        views.forEachIndexed { index, view ->
            val totalDelay = startDelay + (index * staggerDelay)
            
            // Prepare view state
            val offsetPx = 24f * view.resources.displayMetrics.density
            view.alpha = 0f
            view.translationY = offsetPx
            view.isVisible = true

            val startAnim = Runnable {
                // 1. Fade in
                view.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start()

                // 2. Slide up using Spring Physics
                SpringAnimation(view, DynamicAnimation.TRANSLATION_Y, 0f).apply {
                    spring.stiffness = SpringForce.STIFFNESS_LOW
                    spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                }.start()
            }

            if (totalDelay > 0) {
                view.postDelayed(startAnim, totalDelay)
            } else {
                startAnim.run()
            }
        }
    }
}
