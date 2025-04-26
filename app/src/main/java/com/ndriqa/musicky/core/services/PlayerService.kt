package com.ndriqa.musicky.core.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.media.audiofx.Visualizer
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import com.ndriqa.musicky.R
import com.ndriqa.musicky.core.data.PlayingState
import com.ndriqa.musicky.core.data.RepeatMode
import com.ndriqa.musicky.core.data.Song
import com.ndriqa.musicky.core.util.extensions.debugLog
import com.ndriqa.musicky.core.util.extensions.loadAsBitmap
import com.ndriqa.musicky.core.util.helpers.AudioAnalyzer
import com.ndriqa.musicky.core.util.helpers.FftAnalyzer
import com.ndriqa.musicky.core.util.notifications.NotificationChannelInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import androidx.media.app.NotificationCompat as MediaNotificationCompat

class PlayerService : Service(), MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {
    private var mediaPlayer: MediaPlayer? = null
    private var audioVisualizer: Visualizer? = null
    private val fftAnalyzer = FftAnalyzer()
    private val audioAnalyzer = AudioAnalyzer()

    private var progressJob: Job? = null
    private var autoPauseTimerJob: Job? = null
    private var autoKillProcessJob: Job? = null

    private var autoStopTimeLeft: Long? = null
    private var currentIndex = -1
    private var shuffleEnabled = false
    private var highCaptureRateEnabled = false
    private var repeatMode: RepeatMode = RepeatMode.All

    private val originalQueue = mutableListOf<Song>()
    private val shuffledQueue = mutableListOf<Song>()

    private val activeQueue: List<Song>
        get() = if (shuffleEnabled) shuffledQueue else originalQueue

    private val playState: PlayingState?
        get() {
            val song = activeQueue.getOrNull(currentIndex) ?: return null

            val nextIndex = when (repeatMode) {
                RepeatMode.All -> (currentIndex + 1) % activeQueue.size
                RepeatMode.One -> currentIndex
                RepeatMode.None -> null
            }
            val nextSong = nextIndex?.let { activeQueue.getOrNull(it) }
            val isPlaying =
                try { mediaPlayer?.isPlaying == true }
                catch (_: IllegalStateException) { false }
            val currentPosition =
                try { mediaPlayer?.currentPosition?.toLong() ?: 0L }
                catch (_: IllegalStateException) { 0L }

            return PlayingState(
                currentSong = song,
                nextSong = nextSong,
                isPlaying = isPlaying,
                isShuffleEnabled = shuffleEnabled,
                repeatMode = repeatMode,
                autoStopTimeLeft = autoStopTimeLeft,
                currentPosition = currentPosition
            )
        }

    private lateinit var mediaSession: MediaSessionCompat

    override fun onCreate() {
        super.onCreate()
        initializeMediaSession()
    }

    private fun initializeMediaSession() {
        mediaSession = MediaSessionCompat(this, "MusickySession").apply {
            isActive = true
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() = resume()
                override fun onPause() = pause()
                override fun onSkipToNext() = next()
                override fun onSkipToPrevious() = previous()
                override fun onSeekTo(pos: Long) = seekTo(pos.toInt())
                override fun onSetRepeatMode(repeatMode: Int) = toggleRepeatMode(repeatMode)
                override fun onSetShuffleMode(shuffleMode: Int) = toggleShuffleMode(shuffleMode)
            })
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val actualIntent = intent ?: return START_NOT_STICKY

        when (actualIntent.action) {
            ACTION_PLAY -> startPlayback(actualIntent)
            ACTION_PAUSE -> pause()
            ACTION_RESUME -> resume()
            ACTION_NEXT -> next(manualClick = true)
            ACTION_PREVIOUS -> previous(manualClick = true)
            ACTION_STOP -> stopPlayback()
            ACTION_SEEK_TO -> seekTo(actualIntent)
            ACTION_TOGGLE_SHUFFLE -> toggleShuffle()
            ACTION_TOGGLE_REPEAT -> toggleRepeat()
            ACTION_TOGGLE_TIMER -> toggleTimer(actualIntent)
            ACTION_HIGH_RATE_UPDATE -> toggleHighRate(actualIntent)
        }

        return START_STICKY
    }

    private fun toggleHighRate(intent: Intent) {
        highCaptureRateEnabled = intent.getBooleanExtra(EXTRA_HIGH_CAPTURE_RATE, false)
        mediaPlayer?.audioSessionId?.let { sessionId -> setupVisualizer(sessionId) }
    }

    private fun toggleTimer(intent: Intent) {
        val timerMillis = intent.getLongExtra(EXTRA_TIMER_MILLIS, 0L)
        if (timerMillis > 0L) {
            startSleepTimer(timerMillis)
        } else {
            cancelSleepTimer()
        }
        refreshNotificationAndBroadcast()
    }

    private fun toggleRepeatMode(repeatMode: Int) {
        val newRepeatMode = when(repeatMode) {
            PlaybackStateCompat.REPEAT_MODE_ALL -> RepeatMode.All
            PlaybackStateCompat.REPEAT_MODE_ONE -> RepeatMode.One
            else -> RepeatMode.None
        }
        toggleRepeat(newRepeatMode = newRepeatMode)
    }

    private fun toggleShuffleMode(shuffleMode: Int) {
        toggleShuffle(enabled = shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL)
    }

    private fun seekTo(intent: Intent) {
        val pos = intent.getIntExtra(EXTRA_SEEK_POSITION, 0)
        seekTo(pos)
    }

    private fun startPlayback(intent: Intent) {
        highCaptureRateEnabled = intent.getBooleanExtra(EXTRA_HIGH_CAPTURE_RATE, false)
        val newQueue = intent.extractQueue()
        val songPath = intent.getStringExtra(EXTRA_SONG_PATH) ?: newQueue.firstOrNull()?.data

        if (newQueue.isNotEmpty()) {
            originalQueue.clear()
            originalQueue.addAll(newQueue)

            if (shuffleEnabled) {
                shuffledQueue.clear()
                shuffledQueue.addAll(originalQueue.shuffled())
            }

            currentIndex = 0
        }

        songPath?.let { path ->
            val index = activeQueue.indexOfFirst { it.data == path }
            if (index != -1) {
                currentIndex = index
                playCurrent()
            }
        }
    }

    private fun stopPlayback() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        stopProgressUpdates()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun pause() {
        mediaPlayer?.pause()
        refreshNotificationAndBroadcast()
        stopProgressUpdates()
        startSelfSabotage()
    }

    private fun resume() {
        stopSelfSabotage()
        mediaPlayer?.start()
        refreshNotificationAndBroadcast()
        startProgressUpdates()
    }

    private fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
        refreshNotificationAndBroadcast()
    }

    private fun playCurrent(goingForward: Boolean = true) {
        val song = playState?.currentSong ?: return

        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(song.data)
                setOnPreparedListener(this@PlayerService)
                setOnCompletionListener(this@PlayerService)
                prepareAsync()
            }

            buildSongNotification()?.let { startForeground(NOTIFICATION_ID, it) }
            refreshNotificationAndBroadcast()
        } catch (e: Exception) {
            debugLog("song file missing or can't be played: ${song.data}", e)
            debugLog("trying the next song now!")

            if (activeQueue.isNotEmpty()) {
                currentIndex = if (goingForward) {
                    (currentIndex + 1) % activeQueue.size
                } else {
                    if (currentIndex - 1 < 0) activeQueue.lastIndex else currentIndex - 1
                }
                playCurrent(goingForward)
            }
        }
    }

    private fun next(manualClick: Boolean = false) {
        if (activeQueue.isEmpty()) return
        currentIndex = if (manualClick) {
            (currentIndex + 1) % activeQueue.size
        } else when(repeatMode) {
            RepeatMode.All -> (currentIndex + 1) % activeQueue.size
            RepeatMode.One -> currentIndex
            RepeatMode.None -> if (currentIndex < activeQueue.lastIndex) currentIndex + 1 else return
        }
        playCurrent(goingForward = true)
    }

    private fun previous(manualClick: Boolean = false) {
        if (activeQueue.isEmpty()) return
        currentIndex = if (currentIndex - 1 < 0) activeQueue.lastIndex else currentIndex - 1
        playCurrent(goingForward = false)
    }

    private fun toggleRepeat(newRepeatMode: RepeatMode? = null) {
        repeatMode = newRepeatMode ?: repeatMode.calculateNextMode()
        refreshNotificationAndBroadcast()
    }

    private fun toggleShuffle(enabled: Boolean? = null) {
        val currentSong = playState?.currentSong ?: return
        shuffleEnabled = enabled ?: !shuffleEnabled

        if (shuffleEnabled) {
            shuffledQueue.clear()
            shuffledQueue.addAll(originalQueue.shuffled())
        }

        currentIndex = activeQueue.indexOfFirst { it.data == currentSong.data }
        refreshNotificationAndBroadcast()
    }

    override fun onPrepared(mp: MediaPlayer) {
        mp.start()
        try { setupVisualizer(mp.audioSessionId) }
        catch (e: Exception) { Timber.e(e) }
        // if not with progress updates, then call to update noti and broad manually here
        startProgressUpdates()
    }

    override fun onCompletion(mp: MediaPlayer?) {
        next()
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanUpAudioVisualizer()
        cleanUpMediaPlayer()
        stopProgressUpdates()
    }

    private fun cleanUpAudioVisualizer() {
        audioVisualizer?.release()
        audioVisualizer = null
    }

    private fun cleanUpMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun sendStateBroadcast() {
        Intent(ACTION_BROADCAST_UPDATE).apply {
            setPackage(packageName)
            putExtra(EXTRA_PLAYING_STATE, playState)
        }.also { intent -> sendBroadcast(intent) }
    }

    private fun sendStateStoppedBroadcast() {
        Intent(ACTION_BROADCAST_UPDATE)
            .apply { setPackage(packageName) }
            .also { intent -> sendBroadcast(intent) }
    }

    private fun removeSongNotification() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun buildSongNotification(): Notification? {
        val state = playState ?: return null
        val song = state.currentSong
        val isPlaying = state.isPlaying
        val channel = NotificationChannelInfo.Playing
        val maxProgress = song?.duration?.toInt() ?: 0
        val progress = state.currentPosition.toInt()

        val playIconResId = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        val shuffleIconResId = if (state.isShuffleEnabled) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle_off

        val shuffleAction = Intent(this, PlayerService::class.java)
            .apply { action = ACTION_TOGGLE_SHUFFLE }
            .let { intent -> PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE) }
            .let { pendingIntent -> NotificationCompat.Action(shuffleIconResId, "", pendingIntent) }
        val prevAction = Intent(this, PlayerService::class.java)
            .apply { action = ACTION_PREVIOUS }
            .let { intent -> PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_IMMUTABLE) }
            .let { pendingIntent -> NotificationCompat.Action(R.drawable.ic_prev, "", pendingIntent) }
        val playPauseAction = Intent(this, PlayerService::class.java)
            .apply { action = if (isPlaying) ACTION_PAUSE else ACTION_RESUME }
            .let { intent -> PendingIntent.getService(this, 2, intent, PendingIntent.FLAG_IMMUTABLE) }
            .let { pendingIntent -> NotificationCompat.Action(playIconResId, "", pendingIntent) }
        val nextAction = Intent(this, PlayerService::class.java)
            .apply { action = ACTION_NEXT }
            .let { intent -> PendingIntent.getService(this, 3, intent, PendingIntent.FLAG_IMMUTABLE) }
            .let { pendingIntent -> NotificationCompat.Action(R.drawable.ic_next, "", pendingIntent) }
        val cancelAction = Intent(this, PlayerService::class.java)
            .apply { action = ACTION_STOP }
            .let { intent -> PendingIntent.getService(this, 4, intent, PendingIntent.FLAG_IMMUTABLE) }
            .let { pendingIntent -> NotificationCompat.Action(R.drawable.ic_musicky_logo, "", pendingIntent) }

        val mediaStyle = MediaNotificationCompat.MediaStyle()
//            .setShowCancelButton(true)
//            .setCancelButtonIntent(cancelAction)
            .setMediaSession(mediaSession.sessionToken)
            .setShowActionsInCompactView(1, 2, 3)

        return NotificationCompat.Builder(this, channel.id)
            .setContentTitle(song?.title ?: getString(R.string.no_song_playing))
            .setContentText(song?.artist ?: getString(R.string.unknown))
            .setSmallIcon(android.R.drawable.ic_media_play)
            .addAction(shuffleAction)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .addAction(cancelAction)
            .setStyle(mediaStyle)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            .setProgress(maxProgress, progress, false)
            .build()
    }

    fun startSelfSabotage() {
        debugLog("started service self sabotage")
        stopForeground(STOP_FOREGROUND_DETACH)
        autoKillProcessJob?.cancel()
        autoKillProcessJob = CoroutineScope(Dispatchers.Default).launch {
            delay(AUTO_STOP_TIMEOUT)
            if (playState?.isPlaying != true) {
                sendStateStoppedBroadcast()
                removeSongNotification()
                stopSelf()
                debugLog("service self unalived")
            }
        }
    }

    fun stopSelfSabotage() {
        autoKillProcessJob?.cancel()
        autoKillProcessJob = null
        buildSongNotification()?.let { startForeground(NOTIFICATION_ID, it) }
        debugLog("canceled self sabotage")
    }

    private fun startSleepTimer(duration: Long) {
        autoStopTimeLeft = null
        autoPauseTimerJob?.cancel()
        autoPauseTimerJob = CoroutineScope(Dispatchers.Default).launch {
            var timeLeft = duration
            autoStopTimeLeft = timeLeft

            while (isActive && timeLeft > 0) {
                delay(REFRESH_FREQUENCY)
                timeLeft -= REFRESH_FREQUENCY
                autoStopTimeLeft = timeLeft
                debugLog("sleep timer ticking: $timeLeft ms left")
                refreshNotificationAndBroadcast()
            }

            if (isActive) {
                autoStopTimeLeft = null
                pause()
                debugLog("sleep timer done. stopped playback.")
            }

            refreshNotificationAndBroadcast()
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                refreshNotificationAndBroadcast()
                delay(REFRESH_FREQUENCY)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun cancelSleepTimer() {
        autoPauseTimerJob?.cancel()
        autoPauseTimerJob = null
        autoStopTimeLeft = null
    }

    private fun refreshNotificationAndBroadcast() {
        val state = playState ?: return
        val notification = buildSongNotification() ?: return
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(NOTIFICATION_ID, notification)
        updateMediaSessionMetadata(state)
        sendStateBroadcast()
    }

    private fun updateMediaSessionMetadata(state: PlayingState) {
        val song = state.currentSong ?: return
        val artworkBitmap = song.artworkUri?.loadAsBitmap(this)

        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration)
            .apply {
                artworkBitmap?.let { bitmap ->
                    putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap)
                    putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
                }
            }
            .build()

        val mediaState =
            if (state.isPlaying) PlaybackStateCompat.STATE_PLAYING
            else PlaybackStateCompat.STATE_PAUSED

        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SEEK_TO or
                PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE or
                PlaybackStateCompat.ACTION_STOP
            )
            .setState(
                /* state = */ mediaState,
                /* position = */ state.currentPosition,
                /* playbackSpeed = */ 1f
            )
            .build()

        mediaSession.apply {
            setMetadata(metadata)
            setPlaybackState(playbackState)
        }
    }

    private fun setupVisualizer(audioSessionId: Int) {
        audioVisualizer?.release() // always release before setting a new one
        audioVisualizer = Visualizer(audioSessionId).apply {
            val captureRate = Visualizer.getMaxCaptureRate() / if (highCaptureRateEnabled) 1 else 2

            captureSize = Visualizer.getCaptureSizeRange()[1]
            setDataCaptureListener(
                /* listener = */ object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer,
                        waveform: ByteArray,
                        samplingRate: Int
                    ) {
                        if (mediaPlayer?.isPlaying != true) return
                        val audioFeatures = audioAnalyzer.analyze(waveform)
                        // send waveform data to the UI using broadcast (or another mechanism)
                        Intent(ACTION_VISUALIZER_UPDATE)
                            .setPackage(packageName)
                            .putExtra(VISUALIZER_WAVEFORM, waveform)
                            .putExtra(VISUALIZER_AUDIO_FEATURES, audioFeatures)
                            .also { intent -> sendBroadcast(intent) }
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer,
                        fft: ByteArray,
                        samplingRate: Int
                    ) {
                        // optionally broadcast FFT data instead (or both)
                        val fftFeatures = fftAnalyzer.analyze(fft, samplingRate)
                        Intent(ACTION_VISUALIZER_UPDATE)
                            .setPackage(packageName)
                            .putExtra(VISUALIZER_FFT_FEATURES, fftFeatures)
                            .also { intent -> sendBroadcast(intent) }
                    }
                },
                /* rate = */ captureRate,
                /* waveform = */ true,
                /* fft = */ true
            )
            enabled = true
        }
    }

    fun RepeatMode.calculateNextMode(): RepeatMode {
        val nextIndex = (ordinal + 1) % RepeatMode.entries.size
        return RepeatMode.entries[nextIndex]
    }

    private fun Intent.extractQueue(): ArrayList<Song> {
        val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableArrayListExtra(EXTRA_QUEUE, Song::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableArrayListExtra<Song>(EXTRA_QUEUE)
        } ?: arrayListOf()

        return list
    }

    companion object {
        private const val REFRESH_FREQUENCY = 1000L

        const val ACTION_PLAY = "com.ndriqa.action.PLAY"
        const val ACTION_PAUSE = "com.ndriqa.action.PAUSE"
        const val ACTION_RESUME = "com.ndriqa.action.RESUME"
        const val ACTION_NEXT = "com.ndriqa.action.NEXT"
        const val ACTION_PREVIOUS = "com.ndriqa.action.PREVIOUS"
        const val ACTION_STOP = "com.ndriqa.action.STOP"
        const val ACTION_SEEK_TO = "com.ndriqa.action.SEEK_TO"

        const val ACTION_TOGGLE_SHUFFLE = "com.ndriqa.action.TOGGLE_SHUFFLE"
        const val ACTION_TOGGLE_REPEAT = "com.ndriqa.action.TOGGLE_REPEAT"
        const val ACTION_TOGGLE_TIMER = "com.ndriqa.action.TOGGLE_TIMER"

        const val ACTION_BROADCAST_UPDATE = "com.ndriqa.action.UPDATE"
        const val ACTION_VISUALIZER_UPDATE = "com.ndriqa.action.VISUALIZER_UPDATE"
        const val ACTION_HIGH_RATE_UPDATE = "com.ndriqa.action.HIGH_RATE_UPDATE"

        const val EXTRA_SONG_PATH = "EXTRA_SONG_PATH"
        const val EXTRA_PLAYING_STATE = "EXTRA_PLAYING_STATE"
        const val EXTRA_QUEUE = "EXTRA_QUEUE"
        const val EXTRA_SEEK_POSITION = "EXTRA_SEEK_POSITION"
        const val EXTRA_TIMER_MILLIS = "EXTRA_TIMER_MILLIS"
        const val EXTRA_HIGH_CAPTURE_RATE = "EXTRA_HIGH_CAPTURE_RATE"

        const val VISUALIZER_WAVEFORM = "VISUALIZER_WAVEFORM"
        const val VISUALIZER_AUDIO_FEATURES = "VISUALIZER_AUDIO_FEATURES"
        const val VISUALIZER_FFT_FEATURES = "VISUALIZER_FFT_FEATURES"

        const val AUTO_STOP_TIMEOUT = 30 * 60 * 1_000L // minutes * seconds * millis

        const val NOTIFICATION_ID = 69420
    }
}
