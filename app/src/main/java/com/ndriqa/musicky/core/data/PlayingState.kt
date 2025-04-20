package com.ndriqa.musicky.core.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlayingState(
    val isPlaying: Boolean = false,
    val currentSong: Song? = null,
    val nextSong: Song? = null,
    val currentPosition: Long = 0L, // in ms
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.All,
    val autoStopTimeLeft: Long? = null // null if timer is not enabled
): Parcelable {
    val timeLeft: String?
        get() = autoStopTimeLeft?.let {
            val secondsLeft = (it / 1_000).toInt()
            val minutes = secondsLeft / 60
            val seconds = secondsLeft % 60
            val minText = "$minutes".padStart(2, '0')
            val secText = "$seconds".padStart(2, '0')
            "$minText:$secText"
        } // if more than a minute left, return in minutes, else in seconds
}
