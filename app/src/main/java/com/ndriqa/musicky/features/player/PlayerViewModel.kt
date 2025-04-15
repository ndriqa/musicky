package com.ndriqa.musicky.features.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ndriqa.musicky.core.data.PlayingState
import com.ndriqa.musicky.core.data.Song
import com.ndriqa.musicky.core.services.PlayerService
import com.ndriqa.musicky.core.services.PlayerService.Companion.VISUALIZER_WAVEFORM
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

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

    private val visualizerUpdate = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val newWaveform = intent?.getByteArrayExtra(VISUALIZER_WAVEFORM) ?: return
            updateWaveform(newWaveform)
        }
    }

    private val _playingState = MutableStateFlow(PlayingState())
    val playingState: StateFlow<PlayingState> = _playingState.asStateFlow()

    private val _songEnergyRecordings = MutableStateFlow(byteArrayOf())
    val songEnergyRecordings = _songEnergyRecordings.asStateFlow()
    val averageSongEnergy = _songEnergyRecordings
        .map { it.map { abs(it.toInt()) }.average().toInt().toByte() }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    private val _pulse = MutableSharedFlow<Boolean>()
    val pulse = _pulse.asSharedFlow()

    private val _waveform = MutableStateFlow(byteArrayOf())
    val waveform = _waveform.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private fun updateWaveform(waveform: ByteArray) {
        viewModelScope.launch {
            val resampledWaveform = waveform //.resampleTo(WAVEFORM_BARS)
            val energyRecordings = _songEnergyRecordings.value
            val currentEnergy = resampledWaveform.map { abs(it.toInt()) }.average().toInt().toByte()

            _pulse.emit(currentEnergy - PULSE_THRESHOLD > averageSongEnergy.value)
            _songEnergyRecordings.value = energyRecordings + currentEnergy
            _waveform.value = resampledWaveform
        }
    }

    fun resetSongAverageEnergy() {
        _songEnergyRecordings.value = byteArrayOf()
    }

    fun registerPlayerUpdates(context: Context) {
        val filter = IntentFilter(PlayerService.ACTION_BROADCAST_UPDATE)
        ContextCompat.registerReceiver(
            /* context = */ context,
            /* receiver = */ playerUpdateReceiver,
            /* filter = */ filter,
            /* flags = */ ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    fun registerVisualizerUpdates(context: Context) {
        val filter = IntentFilter(PlayerService.ACTION_VISUALIZER_UPDATE)
        ContextCompat.registerReceiver(
            /* context = */ context,
            /* receiver = */ visualizerUpdate,
            /* filter = */ filter,
            /* flags = */ ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    fun unregisterPlayerUpdates(context: Context) {
        context.unregisterReceiver(playerUpdateReceiver)
    }

    fun unregisterVisualizerUpdates(context: Context) {
        context.unregisterReceiver(visualizerUpdate)
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

    companion object {
        private const val PULSE_THRESHOLD = 15
    }
}