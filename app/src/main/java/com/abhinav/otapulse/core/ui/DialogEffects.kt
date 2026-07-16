package com.abhinav.otapulse.core.ui

import android.app.ActivityManager
import android.app.Dialog
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.PowerManager
import android.view.ViewParent
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider

fun <T : Dialog> T.applyBackgroundBlur(
    backgroundBlurRadius: Int = 20,
    dimAmount: Float? = null
): T {
    window?.applyBackgroundBlur(backgroundBlurRadius, dimAmount)
    return this
}

fun Window.applyBackgroundBlur(
    backgroundBlurRadius: Int = 20,
    dimAmount: Float? = null
): Window {
    val context = context

    val isNightMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    val targetDim = dimAmount ?: if (isNightMode) 0.32f else 0.45f

    var changed = false
    val attrs = attributes

    if (attrs.dimAmount != targetDim) {
        attrs.dimAmount = targetDim
        changed = true
    }
    if ((attrs.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND) == 0) {
        attrs.flags = attrs.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
        changed = true
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager

        val isLowEndOrBatterySaving = (activityManager?.isLowRamDevice == true) ||
                (powerManager?.isPowerSaveMode == true)
        val isBlurSupported = (windowManager?.isCrossWindowBlurEnabled != false) && !isLowEndOrBatterySaving

        if (isBlurSupported) {
            val radiusPx = (backgroundBlurRadius * context.resources.displayMetrics.density).toInt()
            if ((attrs.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND) == 0) {
                attrs.flags = attrs.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                changed = true
            }
            if (attrs.blurBehindRadius != radiusPx) {
                attrs.blurBehindRadius = radiusPx
                changed = true
            }
        } else {
            if ((attrs.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND) != 0) {
                attrs.flags = attrs.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
                changed = true
            }
        }
    }

    if (changed) {
        attributes = attrs
    }
    return this
}

@Composable
fun ApplyDialogBlurEffect(
    backgroundBlurRadius: Int = 20,
    dimAmount: Float? = null
) {
    val view = LocalView.current
    val window = remember(view) {
        var dialogWindow: Window? = if (view is DialogWindowProvider) view.window else null
        var current: ViewParent? = view.parent
        while (dialogWindow == null && current != null) {
            if (current is DialogWindowProvider) {
                dialogWindow = current.window
                break
            }
            current = current.parent
        }
        dialogWindow
    }

    SideEffect {
        window?.applyBackgroundBlur(backgroundBlurRadius, dimAmount)
    }
}

