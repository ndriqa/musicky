package com.ndriqa.musicky.features.player

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ndriqa.musicky.core.data.PlayingState
import com.ndriqa.musicky.core.data.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext context: Context
): ViewModel() {
    private val _playingState = MutableStateFlow(PlayingState())
    val playingState: StateFlow<PlayingState> = _playingState.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _currentIndex = _playingState
        .map {
            it.currentSong
                ?.let { _queue.value.indexOf(it) }
                ?: -1
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, -1)

    private var progressJob: Job? = null
    private var player: MediaPlayer? = null

    init {
        initMediaPlayer(context)
    }

    private fun initMediaPlayer(context: Context) {
        player = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            MediaPlayer(context)
        } else MediaPlayer()

        player?.apply {
            setOnCompletionListener { next() }
            setOnPreparedListener { startProgressUpdates() }
        }
    }

    private fun startProgressUpdates() {
        stopProgressUpdates()

        progressJob = viewModelScope.launch {
            while (isActive) {
                val current = player?.currentPosition?.toLong() ?: 0L
                _playingState.update { it.copy(currentPosition = current) }
                delay(1000L)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    fun play(song: Song? = null) {
        val song = song ?: _queue.value.first()
        val mediaPlayer = player ?: return

        try {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(song.data)
            mediaPlayer.prepare()
            mediaPlayer.start()

            _playingState.update {
                it.copy(
                    currentSong = song,
                    isPlaying = true
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _playingState.update {
                it.copy(
                    isPlaying = false,
                    currentSong = null
                )
            }
        }
    }

    fun togglePlayPause() {
        val mediaPlayer = player ?: return
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            _playingState.update { it.copy(isPlaying = false) }
        } else {
            mediaPlayer.start()
            _playingState.update { it.copy(isPlaying = true) }
        }
    }

    fun seekTo(position: Long) {
        player?.seekTo(position, MediaPlayer.SEEK_CLOSEST)
    }

    fun next() {
        if (_queue.value.isEmpty()) return
        val trueNextIndex = _currentIndex.value + 1
        val nextIndex = if (trueNextIndex > _queue.value.lastIndex) 0 else trueNextIndex
        if (nextIndex != _currentIndex.value) {
            play(_queue.value[nextIndex])
        }
    }

    fun previous() {
        if (_queue.value.isEmpty()) return
        val truePrevIndex = _currentIndex.value - 1
        val prevIndex = if (truePrevIndex < 0) _queue.value.lastIndex else truePrevIndex
        if (prevIndex != _currentIndex.value) {
            play(_queue.value[prevIndex])
        }
    }

    fun setQueue(songs: List<Song>) {
        _queue.value = songs
    }

    fun release() {
        player?.release()
        player = null
    }
}