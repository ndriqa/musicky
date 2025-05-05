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

fun ByteArray.trim(): ByteArray {
    val first = indexOfFirst { it != 0.toByte() }
    val last = indexOfLast { it != 0.toByte() }

    if (first == -1 || last == -1 || first > last) return byteArrayOf(0)

    return copyOfRange(first, last + 1)
}

fun ByteArray.unsigned(): List<Int> {
    return map { it.toInt() and 0xFF }
}

fun ByteArray.blendEdges(blendingRatio: Float): ByteArray {
    if (isEmpty() || blendingRatio <= 0f) return this.copyOf()

    val blendCount = (size * (blendingRatio / 2f)).toInt().coerceAtLeast(1)

    return ByteArray(size) { i ->
        when {
            i < blendCount -> {
                // blend start with end
                val a = this[i].toInt() and 0xFF
                val b = this[size - blendCount + i].toInt() and 0xFF
                ((a + b) / 2).toByte()
            }

            i >= size - blendCount -> {
                // blend end with start
                val a = this[i].toInt() and 0xFF
                val b = this[i - (size - blendCount)].toInt() and 0xFF
                ((a + b) / 2).toByte()
            }

            else -> this[i]
        }
    }
}

fun ByteArray.mirrorEnds(mirroringRatio: Float): ByteArray {
    if (isEmpty() || mirroringRatio <= 0f) return this.copyOf()

    val mirrorCount = (size * mirroringRatio).toInt().coerceAtLeast(1)
    val result = this.copyOf()

    for (i in 0 until mirrorCount) {
        result[size - 1 - i] = this[i]
    }

    return result
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

            unsigned().forEachIndexed { i, byte ->
                val normalized = byte / 255f * 2f - 1f
                val y = centerY - (normalized * centerY)
                val x = i * xStep
                path.lineTo(x, y)
            }

            path.lineTo(width, centerY)
        }

        VisualizerType.Bars -> {
            val resampledWaveform = resampleTo(69).unsigned()
            val xStep = width / resampledWaveform.size

            resampledWaveform.forEachIndexed { i, byte ->
                val normalized = byte / 255f
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

            unsigned().forEachIndexed { i, byte ->
                val normalized = byte / 255f
                val dynamicRadius = baseRadius + normalized * radiusVariation

                val angle = i * angleStep
                val x = centerX + cos(angle) * dynamicRadius
                val y = centerY + sin(angle) * dynamicRadius

                if (i == 0) path.moveTo(x, y)
                else path.lineTo(x, y)
            }

            path.close()
        }

        VisualizerType.LineSmooth -> {
            resampleTo(20).unsigned().let {
                val xStep = width / it.size

                path.moveTo(0f, centerY)

                var prevX = 0f
                var prevY = centerY

                it.forEachIndexed { i, byte ->
                    val normalized = byte / 255f * 2f - 1f
                    val y = centerY - (normalized * centerY)
                    val x = i * xStep

                    val midX = (prevX + x) / 2f
                    val midY = (prevY + y) / 2f

                    path.quadraticTo(prevX, prevY, midX, midY)

                    prevX = x
                    prevY = y
                }

                path.quadraticTo(prevX, prevY, width, centerY)
            }
        }
    }

    return path
}