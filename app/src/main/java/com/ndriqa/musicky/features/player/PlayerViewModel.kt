package com.ndriqa.musicky.features.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ndriqa.musicky.core.data.AudioFeatures
import com.ndriqa.musicky.core.data.FftFeatures
import com.ndriqa.musicky.core.data.PlayingState
import com.ndriqa.musicky.core.data.Song
import com.ndriqa.musicky.core.preferences.DataStoreManager
import com.ndriqa.musicky.core.services.PlayerService
import com.ndriqa.musicky.core.services.PlayerService.Companion.VISUALIZER_AUDIO_FEATURES
import com.ndriqa.musicky.core.services.PlayerService.Companion.VISUALIZER_FFT_FEATURES
import com.ndriqa.musicky.core.services.PlayerService.Companion.VISUALIZER_WAVEFORM
import com.ndriqa.musicky.core.util.extensions.getSafeParcelableExtra
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val dataStoreManager: DataStoreManager
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
            intent?.run {
                getByteArrayExtra(VISUALIZER_WAVEFORM)
                    ?.let { updateWaveform(it)  }

                getSafeParcelableExtra<AudioFeatures>(VISUALIZER_AUDIO_FEATURES)
                    ?.let { updateAudioFeatures(it) }

                getSafeParcelableExtra<FftFeatures>(VISUALIZER_FFT_FEATURES)
                    ?.let { updateFftFeatures(it) }
            }
        }
    }

    private val _playingState = MutableStateFlow(PlayingState())
    val playingState: StateFlow<PlayingState> = _playingState.asStateFlow()

    private val _waveform = MutableStateFlow(byteArrayOf())
    val waveform = _waveform.asStateFlow()

    private val _audioFeatures = MutableStateFlow(AudioFeatures())
    val audioFeatures = _audioFeatures.asStateFlow()

    private val _fftFeatures = MutableStateFlow(FftFeatures())
    val fftFeatures = _fftFeatures.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    val highCaptureRate = dataStoreManager.highCaptureRate
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    @VisibleForTesting
    internal fun updatePlayingStateTesting(playingState: PlayingState) {
        _playingState.value = playingState
    }

    private fun updateWaveform(waveform: ByteArray) {
        _waveform.update { waveform }
    }

    private fun updateFftFeatures(newFftFeatures: FftFeatures) {
        _fftFeatures.value = newFftFeatures
    }

    private fun updateAudioFeatures(newAudioFeatures: AudioFeatures) {
        _audioFeatures.value = newAudioFeatures
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
            .setAction(PlayerService.ACTION_PAUSE)
            .also { context.startService(it) }
    }

    fun resume(context: Context) {
        Intent(context, PlayerService::class.java)
            .setAction(PlayerService.ACTION_RESUME)
            .also { context.startService(it) }
    }

    fun next(context: Context) {
        Intent(context, PlayerService::class.java)
            .setAction(PlayerService.ACTION_NEXT)
            .also { context.startService(it) }
    }

    fun previous(context: Context) {
        Intent(context, PlayerService::class.java)
            .setAction(PlayerService.ACTION_PREVIOUS)
            .also { context.startService(it) }
    }

    fun play(context: Context, song: Song? = null) {
        val selectedSong = song ?: _queue.value.firstOrNull() ?: return

        Intent(context, PlayerService::class.java)
            .setAction(PlayerService.ACTION_PLAY)
            .putExtra(PlayerService.EXTRA_SONG_PATH, selectedSong.data)
            .putExtra(PlayerService.EXTRA_HIGH_CAPTURE_RATE, highCaptureRate.value)
            .putParcelableArrayListExtra(PlayerService.EXTRA_QUEUE, ArrayList(_queue.value))
            .also { ContextCompat.startForegroundService(context, it) }
    }

    fun seekToProgress(context: Context, progress: Float) {
        val currentMax = _playingState.value.currentSong?.duration ?: 0
        seekTo(context, positionMillis = (currentMax * progress).toInt())
    }

    fun seekTo(context: Context, positionMillis: Int) {
        Intent(context, PlayerService::class.java)
            .setAction(PlayerService.ACTION_SEEK_TO)
            .putExtra(PlayerService.EXTRA_SEEK_POSITION, positionMillis)
            .also { context.startService(it) }
    }

    fun toggleShuffle(context: Context) {
        Intent(context, PlayerService::class.java)
            .setAction(PlayerService.ACTION_TOGGLE_SHUFFLE)
            .also { context.startService(it) }
    }

    fun toggleRepeat(context: Context) {
        Intent(context, PlayerService::class.java)
            .setAction(PlayerService.ACTION_TOGGLE_REPEAT)
            .also { context.startService(it) }
    }

    fun toggleTimer(context: Context, time: Long? = null) { // if null, stop timer
        Intent(context, PlayerService::class.java)
            .setAction(PlayerService.ACTION_TOGGLE_TIMER)
            .apply { time?.let { putExtra(PlayerService.EXTRA_TIMER_MILLIS, it) } }
            .also { context.startService(it) }
    }

    fun setQueue(songs: List<Song>) {
        _queue.value = songs
    }

    companion object {
        private const val PULSE_THRESHOLD = 15

        internal const val TIMER_MINIMUM_MIN = 10f
        internal const val TIMER_MAXIMUM_MIN = 60f
        internal const val TIMER_STEPS = 9
    }
}