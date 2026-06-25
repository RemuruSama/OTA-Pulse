package com.abhinav.otapulse.core.ui

import android.app.Dialog
import android.content.res.Configuration
import android.os.Build
import android.view.WindowManager

fun Dialog.applyBackgroundBlur(
    backgroundBlurRadius: Int = 20,
    dimAmount: Float? = null
) {
    val window = window ?: return
    val context = context

    val isNightMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    val targetDim = dimAmount ?: if (isNightMode) 0.32f else 0.45f

    window.setDimAmount(targetDim)
    window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val radiusPx = (backgroundBlurRadius * context.resources.displayMetrics.density).toInt()
        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        window.attributes = window.attributes.apply {
            blurBehindRadius = radiusPx
        }
    }
}
