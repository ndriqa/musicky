package com.ndriqa.musicky.core.util.helpers

import kotlin.math.exp
import kotlin.math.pow
import kotlin.random.Random

private const val STEP = 6
private const val BOTTOM = Byte.MIN_VALUE
private const val TOP = Byte.MAX_VALUE
private const val CENTER = 0.toByte()
private val SPACE = List(STEP) { CENTER }
private val CENTER_TO_BOTTOM = (CENTER downTo BOTTOM step STEP).map { it.toByte() }
private val TOP_TO_CENTER = (TOP downTo CENTER step STEP).map { it.toByte() }
private val CENTER_TO_TOP = (CENTER..TOP step STEP).map { it.toByte() }
private val BOTTOM_TO_CENTER = (BOTTOM..CENTER step STEP).map { it.toByte() }
private val BOTTOM_LINE = List(STEP) { BOTTOM }

private val M = buildList<Byte> {
    addAll(SPACE)
    add(BOTTOM)
    add(TOP)
    addAll(TOP_TO_CENTER)
    addAll(CENTER_TO_TOP)
    add(BOTTOM)
    add(CENTER)
    addAll(SPACE)
}

private val U = buildList {
    addAll(SPACE)
    add(TOP)
    addAll(CENTER_TO_BOTTOM)
    addAll(BOTTOM_LINE)
    addAll(BOTTOM_TO_CENTER)
    add(TOP)
    addAll(SPACE)
}


private val S = buildList {
    addAll(SPACE)
    add(TOP)
    repeat(STEP * 6) { add(if (it % 2 == 0) CENTER else TOP) }
    repeat(STEP * 6) { add(if (it % 2 == 0) CENTER else BOTTOM) }
    add(BOTTOM)
    addAll(SPACE)
}


private val I = buildList {
    addAll(SPACE)
    add(TOP)
    add(BOTTOM)
    addAll(SPACE)
}

private val C = buildList {
    addAll(SPACE)
    repeat(STEP * 6) { add(if (it % 2 == 0) TOP else BOTTOM)}
    addAll(SPACE)
}

val MUSIC_WAVEFORM: ByteArray =
    (SPACE + M + SPACE + U + SPACE + S + SPACE + I + SPACE + C + SPACE).toByteArray()

fun floweryGaussianWaveform(size: Int): ByteArray {
    val petals = 5
    return gaussianWaveform(size / petals) +
            gaussianWaveform(size / petals) +
            gaussianWaveform(size / petals) +
            gaussianWaveform(size / petals) +
            gaussianWaveform(size / petals)
}

fun gaussianWaveform(
    size: Int,
    amplitude: Int = 255, // full range span
    center: Float = 0.5f,
    stdDev: Float = 0.3f,
    noise: Int = 30
): ByteArray {
    return ByteArray(size) { i ->
        val x = i.toFloat() / size
        val exponent = -((x - center).pow(2) / (2 * stdDev.pow(2)))
        val normalized = exp(exponent) // 0..1
        val generatedNoise = Random.nextInt(-noise, noise)
        ((normalized * amplitude) - (amplitude / 2))
            .toInt()
            .minus(128)
            .plus(generatedNoise)
            .coerceIn(-256, -1)
            .toByte()
    }
}
