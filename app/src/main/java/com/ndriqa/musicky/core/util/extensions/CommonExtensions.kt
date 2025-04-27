package com.ndriqa.musicky.core.util.extensions

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.ui.graphics.Path
import com.ndriqa.musicky.core.data.Album
import com.ndriqa.musicky.core.data.Song
import com.ndriqa.musicky.core.data.VisualizerType
import timber.log.Timber
import java.time.ZonedDateTime
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

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

fun ByteArray.resampleTo(size: Int): ByteArray {
    if (this.size <= size) return this.copyOf(size)

    val step = this.size / size.toFloat()
    return ByteArray(size) { i ->
        val start = (i * step).toInt()
        val end = ((i + 1) * step).toInt().coerceAtMost(this.size)
        this.slice(start until end).average().toInt().toByte()
    }
}

fun ByteArray.waveformToPath(
    width: Float,
    height: Float,
    visualizerType: VisualizerType
): Path {
    val path = Path()
    if (isEmpty()) return path

    val centerY = height / 2f
    val centerX = width / 2f
    val xStep = width / size

    when(visualizerType) {
        VisualizerType.LineCenter -> {
            path.moveTo(0f, centerY)

            forEachIndexed { i, byte ->
                val unsigned = byte.toInt() and 0xFF
                val normalized = unsigned / 255f * 2f - 1f
                val y = centerY - (normalized * centerY)
                val x = i * xStep
                path.lineTo(x, y)
            }

            path.lineTo(width, centerY)
        }

        VisualizerType.LineBottom -> {
            path.moveTo(0f, height)

            forEachIndexed { i, byte ->
                val unsigned = byte.toInt() and 0xFF
                val normalized = unsigned / 255f * 2f - 1f
                val y = height - (normalized.absoluteValue * height)
                val x = i * xStep
                path.lineTo(x, y)
            }

            path.lineTo(width, height)
        }

        VisualizerType.Circular -> {
            val maxRadius = min(width, height) / 2  // half of the inside square
            val baseRadius = maxRadius * 2 / 3      // 2/3 of max radius
            val radiusVariation = maxRadius / 3     // 1/3 of max radius
            val pointCount = size
            val angleStep = (2 * Math.PI / pointCount).toFloat()

            forEachIndexed { i, byte ->
                val unsigned = byte.toInt() and 0xFF
                val normalized = unsigned / 255f
                val dynamicRadius = baseRadius + normalized * radiusVariation

                val angle = i * angleStep
                val x = centerX + cos(angle) * dynamicRadius
                val y = centerY + sin(angle) * dynamicRadius

                if (i == 0) path.moveTo(x, y)
                else path.lineTo(x, y)
            }

            path.close()
        }
    }

    return path
}

inline fun <reified T : Parcelable> Intent.getSafeParcelableExtra(key: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(key)
    }
}

inline fun <T> T?.ifNull(producer: () -> T): T {
    return this ?: producer()
}

fun Any.debugLog(text: String, error: Throwable? = null) {
    Timber.tag(this.javaClass.simpleName).d(error, text)
}