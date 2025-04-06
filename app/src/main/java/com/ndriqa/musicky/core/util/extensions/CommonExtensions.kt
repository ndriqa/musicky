package com.ndriqa.musicky.core.util.extensions

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.ViewModel
import timber.log.Timber
import java.time.ZonedDateTime
import java.util.Locale

fun Long.toFormattedTime(showMillis: Boolean = true): String {
    val locale = Locale.getDefault()
    val hours = this / 3600000
    val minutes = (this / 60000) % 60
    val seconds = (this / 1000) % 60
    val millis = this % 1000

    return when {
        hours > 0 && showMillis -> String.format(locale, "%02d:%02d:%02d:%03d", hours, minutes, seconds, millis)
        hours > 0 && !showMillis -> String.format(locale, "%02d:%02d:%02d", hours, minutes, seconds)
        hours <= 0 && showMillis -> String.format(locale, "%02d:%02d:%03d", minutes, seconds, millis)
        else -> String.format(locale, "%02d:%02d", minutes, seconds)
    }
}

fun Vibrator.vibratePattern(pattern: LongArray) {
    vibrate(VibrationEffect.createWaveform(pattern, -1)) // -1 means no repeat
}

fun Vibrator.bzz() {
    val amplitude =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) 100
        else VibrationEffect.DEFAULT_AMPLITUDE

    vibrate(VibrationEffect.createOneShot(
        /* milliseconds = */ 50,
        /* amplitude = */ amplitude
    ))
}

fun ZonedDateTime.formatDateTime(): String {
    return this.toLocalDate().toString()
}

fun ViewModel.simpleLog(text: String) {
    Timber.tag(this::class.java.simpleName).d(text)
}