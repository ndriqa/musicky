package com.ndriqa.musicky.core.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FftFeatures(
    val magnitudes: List<Double> = emptyList(),
    val bass: Double = 0.0,
    val mid: Double = 0.0,
    val treble: Double = 0.0,
    val spectralCentroid: Double = 0.0,
    val normalizedCentroid: Double = 0.0 // value between 0 (warm) and 1 (cold)
): Parcelable {
    val isBass: Boolean
        get() = bass > BASS_THRESHOLD

    companion object {
        private const val BASS_THRESHOLD = 90
    }
}