package com.ndriqa.musicky.core.util.extensions

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import com.ndriqa.musicky.core.data.VisualizerType
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

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

fun ByteArray.blendEdges(blendingElements: Int = 6): ByteArray {
    if (isEmpty() || blendingElements <=0) return this.copyOf()
    val start = last().toInt() and 0xFF
    val end = first().toInt() and 0xFF
    val step = (end - start) / blendingElements.toFloat()

    return ByteArray(size + blendingElements) { i ->
        if (i < size) this[i]
        else {
            val blendedValue = start + step * (i - size + 1)
            blendedValue.roundToInt().coerceIn(0, 255).toByte()
        }
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

            unsigned().forEachIndexed { i, byte ->
                val normalized = byte / 255f * 2f - 1f
                val yOffset = normalized * centerY
                val startY = centerY - yOffset * 0.1f // try this from 0 < c < 1 to see some magic
                val y = centerY - yOffset
                val x = i * xStep
                path.lineTo(x, y)
            }

            path.lineTo(width, centerY)
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
            val waveform = blendEdges().unsigned()
            val maxRadius = min(width, height) / 2  // half of the inside square
            val baseRadius = maxRadius * 2 / 3      // 2/3 of max radius
            val radiusVariation = maxRadius / 3     // 1/3 of max radius
            val pointCount = waveform.size
            val angleStep = (2 * Math.PI / pointCount).toFloat()

            waveform.forEachIndexed { i, byte ->
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

        VisualizerType.Flower -> {
            val waveform = blendEdges().unsigned()
            val maxRadius = min(width, height) / 2  // half of the inside square
            val baseRadius = maxRadius * 2 / 3      // 2/3 of max radius
            val radiusVariation = maxRadius / 3     // 1/3 of max radius
            val pointCount = waveform.size
            val angleStep = (2 * Math.PI / pointCount).toFloat()

            waveform.forEachIndexed { i, byte ->
                val normalized = byte / 255f
                val dynamicRadius = baseRadius + normalized * radiusVariation

                val angle = i * angleStep
                val xOffset = cos(angle) * dynamicRadius
                val yOffset = sin(angle) * dynamicRadius

                val x = centerX + xOffset
                val y = centerY + yOffset

                val startX = centerX + xOffset * normalized
                val startY = centerY + yOffset * normalized

                if (i == 0) path.moveTo(x, y)
                else {
                    path.moveTo(startX, startY)
                    path.lineTo(x, y)
                }
            }

            path.close()
        }

        VisualizerType.Noise -> {
            val bytes = resampleTo(625).unsigned()
            val gridSize = sqrt(bytes.size.toFloat()).toInt()
            val cellWidth = width / gridSize.toFloat()
            val cellHeight = height / gridSize.toFloat()
            val maxDotSize = min(cellWidth, cellHeight)
            val dotRadius = maxDotSize / 3f

            bytes
                .take(gridSize * gridSize)
                .forEachIndexed { index, byte ->
                    val row = index / gridSize
                    val col = index % gridSize

                    val cx = col * cellWidth + cellWidth / 2f
                    val cy = row * cellHeight + cellHeight / 2f
                    val radius = (byte / 255f) * maxDotSize

                    path.addRoundRect(
                        roundRect = RoundRect(
                            left = cx - radius,
                            top = cy - radius,
                            right = cx + radius,
                            bottom = cy + radius,
                            cornerRadius = CornerRadius(dotRadius, dotRadius)
                        ),
                        direction = Path.Direction.Clockwise
                    )
                }
        }
    }

    return path
}