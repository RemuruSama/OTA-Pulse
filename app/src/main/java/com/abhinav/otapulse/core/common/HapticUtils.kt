package com.abhinav.otapulse.core.common

import android.view.View
import android.os.Vibrator
import android.content.Context
import android.os.Build
import android.os.VibrationEffect

fun View.performHapticFeedback() {
    val vibrator = context.getSystemService(Vibrator::class.java)
    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
}

fun View.setHapticClickListener(listener: View.OnClickListener) {
    setOnClickListener {
        performHapticFeedback()
        listener.onClick(it)
    }
}
