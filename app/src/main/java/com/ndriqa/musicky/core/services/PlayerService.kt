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
import com.ndriqa.musicky.core.data.Song
import com.ndriqa.musicky.core.util.extensions.loadAsBitmap
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

    private var currentIndex = -1
    private var progressJob: Job? = null

    private val queue = mutableListOf<Song>()
    private val playState: PlayingState?
        get() {
            val song = queue.getOrNull(currentIndex) ?: return null
            val isPlaying =
                try { mediaPlayer?.isPlaying == true }
                catch (e: IllegalStateException) { false }
            val currentPosition =
                try { mediaPlayer?.currentPosition?.toLong() ?: 0L }
                catch (e: IllegalStateException) { 0L }

            return PlayingState(
                currentSong = song,
                isPlaying = isPlaying,
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
            })
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when (intent.action) {
            ACTION_PLAY -> {
                val newQueue = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(EXTRA_QUEUE, Song::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra<Song>(EXTRA_QUEUE)
                }
                val songPath = intent.getStringExtra(EXTRA_SONG_PATH)

                if (!newQueue.isNullOrEmpty()) {
                    queue.clear()
                    queue.addAll(newQueue)
                }

                songPath?.let { path ->
                    val index = queue.indexOfFirst { it.data == path }
                    if (index != -1) {
                        currentIndex = index
                        playCurrent()
                    }
                }
            }

            ACTION_PAUSE -> pause()
            ACTION_RESUME -> resume()
            ACTION_NEXT -> next()
            ACTION_PREVIOUS -> previous()
            ACTION_STOP -> stopSelf()
            ACTION_SEEK_TO -> {
                val pos = intent.getIntExtra(EXTRA_SEEK_POSITION, 0)
                seekTo(pos)
            }
        }

        return START_STICKY
    }

    private fun pause() {
        mediaPlayer?.pause()
        refreshNotificationAndBroadcast()
    }

    private fun resume() {
        mediaPlayer?.start()
        refreshNotificationAndBroadcast()
    }

    private fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
        refreshNotificationAndBroadcast()
    }

    private fun playCurrent() {
        val state = playState ?: return
        val song = state.currentSong ?: return

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(song.data)
            setOnPreparedListener(this@PlayerService)
            setOnCompletionListener(this@PlayerService)
            prepareAsync()
        }

        startForeground(NOTIFICATION_ID, buildSongNotification(state))
        refreshNotificationAndBroadcast()
    }

    private fun next() {
        if (queue.isEmpty()) return
        currentIndex = (currentIndex + 1) % queue.size
        playCurrent()
    }

    private fun previous() {
        if (queue.isEmpty()) return
        currentIndex = if (currentIndex - 1 < 0) queue.lastIndex else currentIndex - 1
        playCurrent()
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
        val song = queue.getOrNull(currentIndex) ?: return

        Intent(ACTION_BROADCAST_UPDATE).apply {
            setPackage(packageName)
            putExtra(EXTRA_PLAYING_STATE, playState)
            putExtra(EXTRA_SONG_PATH, song.data)
        }.also { intent -> sendBroadcast(intent) }
    }

    private fun buildSongNotification(state: PlayingState): Notification {
        val song = state.currentSong
        val isPlaying = state.isPlaying
        val channel = NotificationChannelInfo.Playing
        val maxProgress = song?.duration?.toInt() ?: 0
        val progress = state.currentPosition.toInt()

        val playIconResId = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        val prevAction = Intent(this, PlayerService::class.java)
            .apply { action = ACTION_PREVIOUS }
            .let { intent -> PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE) }
            .let { pendingIntent -> NotificationCompat.Action(R.drawable.ic_prev, "", pendingIntent) }
        val playPauseAction = Intent(this, PlayerService::class.java)
            .apply { action = if (isPlaying) ACTION_PAUSE else ACTION_RESUME }
            .let { intent -> PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_IMMUTABLE) }
            .let { pendingIntent -> NotificationCompat.Action(playIconResId, "", pendingIntent) }
        val nextAction = Intent(this, PlayerService::class.java)
            .apply { action = ACTION_NEXT }
            .let { intent -> PendingIntent.getService(this, 2, intent, PendingIntent.FLAG_IMMUTABLE) }
            .let { pendingIntent -> NotificationCompat.Action(R.drawable.ic_next, "", pendingIntent) }
        val cancelAction = Intent(this, PlayerService::class.java)
            .apply { action = ACTION_STOP }
            .let { intent -> PendingIntent.getService(this, 3, intent, PendingIntent.FLAG_IMMUTABLE) }

        val mediaStyle = MediaNotificationCompat.MediaStyle()
            .setShowActionsInCompactView(0, 1, 2)
            .setShowCancelButton(true)
            .setCancelButtonIntent(cancelAction)
            .setMediaSession(mediaSession.sessionToken)

        return NotificationCompat.Builder(this, channel.id)
            .setContentTitle(song?.title ?: "No song playing")
            .setContentText(song?.artist ?: "Unknown")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .setStyle(mediaStyle)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            .setProgress(maxProgress, progress, false)
            .build()
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                refreshNotificationAndBroadcast()
                delay(1000L)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun refreshNotificationAndBroadcast() {
        val state = playState ?: return
        val notification = buildSongNotification(state)
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
                        PlaybackStateCompat.ACTION_SEEK_TO
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
            captureSize = Visualizer.getCaptureSizeRange()[1]
            setDataCaptureListener(
                /* listener = */ object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer,
                        waveform: ByteArray,
                        samplingRate: Int
                    ) {
                        if (mediaPlayer?.isPlaying != true) return
                        // send waveform data to the UI using broadcast (or another mechanism)
                        Intent(ACTION_VISUALIZER_UPDATE).apply {
                            setPackage(packageName)
                            putExtra(VISUALIZER_WAVEFORM, waveform)
                        }.also { intent -> sendBroadcast(intent) }
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer,
                        fft: ByteArray,
                        samplingRate: Int
                    ) {
                        // optionally broadcast FFT data instead (or both)
                    }
                },
                /* rate = */ Visualizer.getMaxCaptureRate() / 2,
                /* waveform = */ true,
                /* fft = */ false
            )
            enabled = true
        }
    }

    companion object {
        const val ACTION_PLAY = "com.ndriqa.action.PLAY"
        const val ACTION_PAUSE = "com.ndriqa.action.PAUSE"
        const val ACTION_RESUME = "com.ndriqa.action.RESUME"
        const val ACTION_NEXT = "com.ndriqa.action.NEXT"
        const val ACTION_PREVIOUS = "com.ndriqa.action.PREVIOUS"
        const val ACTION_STOP = "com.ndriqa.action.STOP"
        const val ACTION_SEEK_TO = "com.ndriqa.action.SEEK_TO"

        const val ACTION_BROADCAST_UPDATE = "com.ndriqa.action.UPDATE"
        const val ACTION_VISUALIZER_UPDATE = "com.ndriqa.action.VISUALIZER_UPDATE"

        const val EXTRA_SONG_PATH = "extra_song_path"
        const val EXTRA_PLAYING_STATE = "extra_playing_state"
        const val EXTRA_QUEUE = "extra_queue"
        const val EXTRA_SEEK_POSITION = "extra_seek_position"
        const val VISUALIZER_WAVEFORM = "visualizer_waveform"

        private const val VISUALIZER_RETRY_ATTEMPTS = 10 //times
        private const val VISUALIZER_RETRY_INTERVALS = 50L //ms

        const val NOTIFICATION_ID = 1001
    }
}
