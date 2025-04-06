package com.ndriqa.musicky.core.data

data class PlayingState(
    val isPlaying: Boolean = false,
    val currentSong: Song? = null,
    val currentPosition: Long = 0L, // in ms
    val bufferedPosition: Long = 0L, // optional: if you're streaming
    val isPrepared: Boolean = false
)
