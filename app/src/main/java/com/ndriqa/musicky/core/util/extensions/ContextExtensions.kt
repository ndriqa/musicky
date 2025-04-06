package com.ndriqa.musicky.core.util.extensions

import android.content.Context
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager

fun Context.getVibrator(): Vibrator? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = getSystemService(VibratorManager::class.java)
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Vibrator::class.java)
    }
}