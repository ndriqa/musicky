package com.ndriqa.musicky.core.util.extensions

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import com.ndriqa.musicky.core.data.Album
import com.ndriqa.musicky.core.data.Song
import com.ndriqa.musicky.core.data.VisualizerType
import timber.log.Timber
import java.time.ZonedDateTime
import java.util.Locale

fun Long.toFormattedTime(showMillis: Boolean = false): String {
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

fun Song.contains(query: String): Boolean {
    return title.contains(query, ignoreCase = true)
            || artist.contains(query, ignoreCase = true)
            || (album?.contains(query, ignoreCase = true) == true)
}

fun Album.contains(query: String): Boolean {
    return name.contains(query, ignoreCase = true)
            || songs.any { it.contains(query) }
}

fun Uri.loadAsBitmap(context: Context): Bitmap? {
    return try {
        context.contentResolver.openInputStream(this)?.use { input ->
            BitmapFactory.decodeStream(input)
        }
    } catch (e: Exception) {
        null
    }
}

inline fun <T> T?.ifNull(producer: () -> T): T {
    return this ?: producer()
}

fun Any.debugLog(text: String, error: Throwable? = null) {
    Timber.tag(this.javaClass.simpleName).d(error, text)
}

fun VisualizerType.correctRotation(): Modifier = when(this) {
    VisualizerType.Bars -> Modifier.rotate(90f)
    else -> Modifier
}