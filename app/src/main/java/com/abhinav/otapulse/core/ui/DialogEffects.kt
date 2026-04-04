package com.abhinav.otapulse.core.ui

import android.app.Dialog
import android.os.Build
import android.view.WindowManager

fun Dialog.applyBackgroundBlur(
    backgroundBlurRadius: Int = 28,
    dimAmount: Float = 0.5f
) {
    val window = window ?: return

    window.setDimAmount(dimAmount)
    window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        window.setBackgroundBlurRadius(backgroundBlurRadius)
    }
}
