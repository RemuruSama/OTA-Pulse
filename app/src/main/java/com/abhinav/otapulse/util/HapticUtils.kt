package com.abhinav.otapulse.util

import android.view.View
import android.os.Vibrator
import android.content.Context
import android.os.Build
import android.os.VibrationEffect

fun View.performHapticFeedback() {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        vibrator.vibrate(50)
    }
}

fun View.setHapticClickListener(listener: View.OnClickListener) {
    setOnClickListener {
        performHapticFeedback()
        listener.onClick(it)
    }
}
