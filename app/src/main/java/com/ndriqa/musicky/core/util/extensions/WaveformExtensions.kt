package com.ndriqa.musicky.core.util.extensions

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import com.ndriqa.musicky.core.data.VisualizerType
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class ResamplingMethod {
    Mean, Mode
}

fun ByteArray.resampleTo(
    size: Int,
    resamplingMethod: ResamplingMethod = ResamplingMethod.Mode
): ByteArray {
    if (this.size <= size) return this.copyOf(size)

    val step = this.size / size.toFloat()
    return ByteArray(size) { i ->
        val start = (i * step).toInt()
        val end = ((i + 1) * step).toInt().coerceAtMost(this.size)
        this.slice(start until end)
            .let { when(resamplingMethod) {
                ResamplingMethod.Mean -> it.average()
                ResamplingMethod.Mode -> it.mode()
            } }
            .toInt().toByte()
    }
}

fun List<Byte>.mode(): Byte {
    return groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: 0
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
            val resampledWaveform = resampleTo(69)
            val xStep = width / resampledWaveform.size

            resampledWaveform.forEachIndexed { i, byte ->
                val unsigned = byte.toInt() and 0xFF
                val normalized = unsigned / 255f
                val barHeight = normalized * height
                val left = i * xStep
                val top = height - barHeight
                val right = left + xStep
                val bottom = height

                path.addRect(Rect(left, top, right, bottom), Path.Direction.Clockwise)
            }
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