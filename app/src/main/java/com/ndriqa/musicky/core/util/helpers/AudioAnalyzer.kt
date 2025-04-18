package com.ndriqa.musicky.core.util.helpers

import com.ndriqa.musicky.core.data.AudioFeatures
import kotlin.math.abs
import kotlin.math.sqrt

class AudioAnalyzer {

    private var lastEnergy = 0.0
    private var beatThreshold = 1.5 // tune this to your liking

    fun analyze(byteArray: ByteArray): AudioFeatures {
        val size = byteArray.size.takeIf { it > 0 } ?: return AudioFeatures()

        // calculate energy
        val energy = byteArray.sumOf { (it * it).toDouble() } / size

        // rms = sqrt(energy)
        val rms = sqrt(energy)

        // peak value
        val peak = byteArray.maxOf { abs(it.toInt()) }

        // zero crossing rate
        var zcrCount = 0
        for (i in 1 until size) {
            val prev = byteArray[i - 1].toInt()
            val curr = byteArray[i].toInt()
            if ((prev > 0 && curr < 0) || (prev < 0 && curr > 0)) {
                zcrCount++
            }
        }
        val zcr = zcrCount.toDouble() / size

        // beat detection: if energy suddenly spikes compared to last
        val beat = if (lastEnergy > 0) energy / lastEnergy > beatThreshold else false
        lastEnergy = energy

        return AudioFeatures(
            energy = energy,
            rms = rms,
            peak = peak,
            zcr = zcr,
            isBeat = beat
        )
    }
}