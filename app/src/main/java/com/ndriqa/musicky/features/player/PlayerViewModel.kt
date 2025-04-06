package com.ndriqa.musicky.features.player

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import androidx.lifecycle.ViewModel
import com.ndriqa.musicky.core.data.PlayingState
import com.ndriqa.musicky.core.data.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext context: Context
): ViewModel() {
    // State
    private val _playingState = MutableStateFlow(PlayingState())
    val playingState: StateFlow<PlayingState> = _playingState.asStateFlow()

    // Playback queue
    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    // Current index
    private val _currentIndex = MutableStateFlow(-1)

    // Player logic
    private var player: MediaPlayer? = null // or ExoPlayer

    init {
        initMediaPlayer(context)
    }

    private fun initMediaPlayer(context: Context) {
        player = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            MediaPlayer(context)
        } else MediaPlayer()
    }

    fun play(song: Song) {
        val currentState = _playingState.value
        _playingState.update { currentState.copy(currentSong = song) }
    }

    fun togglePlayPause() { /* toggle player */ }

    fun seekTo(position: Long) { /* update position */ }

    fun next() { /* update index, play next */ }

    fun previous() { /* update index, play previous */ }

    fun setQueue(songs: List<Song>) { _queue.value = songs }

}