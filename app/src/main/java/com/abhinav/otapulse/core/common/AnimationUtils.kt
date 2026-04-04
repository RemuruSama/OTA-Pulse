package com.abhinav.otapulse.core.common

import android.view.View
import androidx.core.view.isVisible

object AnimationUtils {

    fun animateEntrance(views: List<View>, startDelay: Long = 0, staggerDelay: Long = 100) {
        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 50f
            view.isVisible = true // Ensure view is visible before animating
            
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(startDelay + (index * staggerDelay))
                .start()
        }
    }
}
