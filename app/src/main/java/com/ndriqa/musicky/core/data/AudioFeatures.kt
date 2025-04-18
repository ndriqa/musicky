package com.ndriqa.musicky.core.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AudioFeatures(
    val energy: Double = 0.0,
    val rms: Double = 0.0,
    val peak: Int = 0,
    val zcr: Double = 0.0,
    val isBeat: Boolean = false
): Parcelable {
    val disturbance: Double
        get() = rms * zcr + peak * 0.05

    val normalizedDisturbance: Double
        get() = (disturbance / MAX_DISTURBANCE).coerceIn(0.0, 1.0)

    companion object {
        private const val MAX_DISTURBANCE = 12.0
    }
}
