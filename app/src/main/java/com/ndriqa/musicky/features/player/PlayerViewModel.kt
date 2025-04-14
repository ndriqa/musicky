package com.ndriqa.musicky.features.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ndriqa.musicky.core.data.PlayingState
import com.ndriqa.musicky.core.data.Song
import com.ndriqa.musicky.core.services.PlayerService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val playerUpdateReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            val playingState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(PlayerService.EXTRA_PLAYING_STATE, PlayingState::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<PlayingState>(PlayerService.EXTRA_PLAYING_STATE)
            }

            playingState?.let { newState -> _playingState.update { newState } }
        }
    }

    private val _playingState = MutableStateFlow(PlayingState())
    val playingState: StateFlow<PlayingState> = _playingState.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    fun registerPlayerUpdates(context: Context) {
        val filter = IntentFilter(PlayerService.ACTION_BROADCAST_UPDATE)
        ContextCompat.registerReceiver(
            /* context = */ context,
            /* receiver = */ playerUpdateReceiver,
            /* filter = */ filter,
            /* flags = */ ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    fun unregisterPlayerUpdates(context: Context) {
        context.unregisterReceiver(playerUpdateReceiver)
    }

    fun playPause(context: Context) {
        val isPlaying = _playingState.value.isPlaying
        return if (isPlaying) pause(context) else resume(context)
    }

    fun pause(context: Context) {
        Intent(context, PlayerService::class.java)
            .apply { action = PlayerService.ACTION_PAUSE }
            .also { context.startService(it) }
    }

    fun resume(context: Context) {
        Intent(context, PlayerService::class.java)
            .apply { action = PlayerService.ACTION_RESUME }
            .also { context.startService(it) }
    }

    fun next(context: Context) {
        Intent(context, PlayerService::class.java)
            .apply { action = PlayerService.ACTION_NEXT }
            .also { context.startService(it) }
    }

    fun previous(context: Context) {
        Intent(context, PlayerService::class.java)
            .apply { action = PlayerService.ACTION_PREVIOUS }
            .also { context.startService(it) }
    }

    fun play(context: Context, song: Song? = null) {
        val selectedSong = song ?: _queue.value.firstOrNull() ?: return

        val intent = Intent(context, PlayerService::class.java).apply {
            action = PlayerService.ACTION_PLAY
            putExtra(PlayerService.EXTRA_SONG_PATH, selectedSong.data)
            putParcelableArrayListExtra(PlayerService.EXTRA_QUEUE, ArrayList(_queue.value))
        }

        ContextCompat.startForegroundService(context, intent)
    }

    fun seekToProgress(context: Context, progress: Float) {
        val currentMax = _playingState.value.currentSong?.duration ?: 0
        seekTo(context, positionMillis = (currentMax * progress).toInt())
    }

    fun seekTo(context: Context, positionMillis: Int) {
        Intent(context, PlayerService::class.java)
            .apply {
                action = PlayerService.ACTION_SEEK_TO
                putExtra(PlayerService.EXTRA_SEEK_POSITION, positionMillis)
            }.also { context.startService(it) }
    }

    fun setQueue(songs: List<Song>) {
        _queue.value = songs
    }

    private fun handlePlayerUpdate(intent: Intent) {
        val path = intent.getStringExtra(PlayerService.EXTRA_SONG_PATH) ?: return
        val isPlaying = intent.getBooleanExtra(PlayerService.EXTRA_IS_PLAYING, false)
        val position = intent.getIntExtra(PlayerService.EXTRA_POSITION, 0)
        val duration = intent.getIntExtra(PlayerService.EXTRA_DURATION, 0)

        val currentSong = queue.value.find { it.data == path } ?: return

        _playingState.update {
            it.copy(
                currentSong = currentSong,
                isPlaying = isPlaying,
                currentPosition = position.toLong()
            )
        }
    }
}