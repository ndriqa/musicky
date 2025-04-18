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
): Parcelable
