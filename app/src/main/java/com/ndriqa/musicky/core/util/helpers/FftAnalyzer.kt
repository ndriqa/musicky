package com.ndriqa.musicky.core.util.helpers

import com.ndriqa.musicky.core.data.FftFeatures
import kotlin.math.sqrt

class FftAnalyzer {
    fun analyze(fft: ByteArray, samplingRate: Int): FftFeatures {
        val magnitudes = mutableListOf<Double>()
        val numBins = fft.size / 2

        for (i in 2 until fft.size step 2) {
            val real = fft[i].toInt()
            val imag = fft[i + 1].toInt()
            val magnitude = sqrt(real * real + imag * imag.toDouble())
            magnitudes.add(magnitude)
        }

        // frequency resolution per bin
        val binHz = samplingRate / 2.0 / numBins

        // split bins into rough freq bands
        val bassBins = magnitudes.indices.filter { it * binHz < 250 }
        val midBins = magnitudes.indices.filter { it * binHz in 250.0..2000.0 }
        val trebleBins = magnitudes.indices.filter { it * binHz > 2000 }

        val bass = bassBins.map { magnitudes[it] }.averageOrZero()
        val mid = midBins.map { magnitudes[it] }.averageOrZero()
        val treble = trebleBins.map { magnitudes[it] }.averageOrZero()

        val weightedSum = magnitudes.mapIndexed { i, mag -> i * binHz * mag }.sum()
        val totalEnergy = magnitudes.sum()
        val centroid = if (totalEnergy != 0.0) weightedSum / totalEnergy else 0.0
        val normalized = centroid / (samplingRate / 2.0)

        return FftFeatures(
            magnitudes = magnitudes,
            bass = bass,
            mid = mid,
            treble = treble,
            spectralCentroid = centroid,
            normalizedCentroid = normalized.coerceIn(0.0, 1.0)
        )
    }

    private fun List<Double>.averageOrZero(): Double {
        return if (isNotEmpty()) average() else 0.0
    }
}