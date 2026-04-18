package com.abhinav.otapulse.core.common

import android.view.View
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

object AnimationUtils {

    private val fastOutSlowIn = FastOutSlowInInterpolator()

    /**
     * Staggered entrance animation: each view fades in and slides up from a
     * 20 dp offset.  A software layer is promoted for the duration of the
     * animation so simultaneous alpha + translation composites cheaply on the
     * GPU and is automatically released when the animation ends.
     *
     * @param views        Views to animate, in stagger order.
     * @param startDelay   Extra delay before the first view starts (ms).
     * @param staggerDelay Delay added per subsequent view (ms).
     */
    fun animateEntrance(views: List<View>, startDelay: Long = 0, staggerDelay: Long = 100) {
        views.forEachIndexed { index, view ->
            // Convert 20 dp → px so displacement is consistent across densities.
            val offsetPx = 20f * view.resources.displayMetrics.density

            view.alpha = 0f
            view.translationY = offsetPx
            view.isVisible = true // Ensure view is visible before animating

            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(startDelay + (index * staggerDelay))
                .setInterpolator(fastOutSlowIn)
                .withLayer()          // hardware layer during animation only
                .start()
        }
    }
}
