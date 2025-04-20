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
    val timeLeft: Long? = null // null if timer is not enabled
): Parcelable
