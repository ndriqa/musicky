package com.ndriqa.musicky.core.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentUris
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.audiofx.Visualizer
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import androidx.media.MediaBrowserServiceCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.room.Room
import com.ndriqa.musicky.MainActivity
import com.ndriqa.musicky.R
import com.ndriqa.musicky.core.data.PlayingState
import com.ndriqa.musicky.core.data.RepeatMode
import com.ndriqa.musicky.core.data.Song
import com.ndriqa.musicky.core.util.extensions.debugLog
import com.ndriqa.musicky.core.util.extensions.loadAsBitmap
import com.ndriqa.musicky.core.preferences.DataStoreManager
import com.ndriqa.musicky.core.util.helpers.AudioAnalyzer
import com.ndriqa.musicky.core.util.helpers.FftAnalyzer
import com.ndriqa.musicky.core.util.notifications.NotificationChannelInfo
import com.ndriqa.musicky.data.repositories.SongsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@AndroidEntryPoint
class PlayerService : MediaBrowserServiceCompat(), Player.Listener {
    @Inject
    lateinit var songsRepository: SongsRepository

    @Inject
    lateinit var dataStoreManager: DataStoreManager

    private lateinit var exoPlayer: ExoPlayer
    private lateinit var mediaSession: MediaSessionCompat

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

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

    private val playState: PlayingState
        get() {
            val song = activeQueue.getOrNull(currentIndex)

            val nextIndex = if (activeQueue.isNotEmpty()) {
                when (repeatMode) {
                    RepeatMode.All -> (currentIndex + 1) % activeQueue.size
                    RepeatMode.One -> currentIndex
                    RepeatMode.None -> null
                }
            } else null
            val nextSong = nextIndex?.let { activeQueue.getOrNull(it) }
            val isPlaying = exoPlayer.isPlaying
            val currentPosition = exoPlayer.currentPosition

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

    override fun onCreate() {
        super.onCreate()
        initializeExoPlayer()
        initializeMediaSession()
        sessionToken = mediaSession.sessionToken
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot(ROOT_MEDIA_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        if (parentId != ROOT_MEDIA_ID) {
            result.sendResult(mutableListOf())
            return
        }

        result.detach()

        serviceScope.launch(Dispatchers.IO) {
            val songs = songsRepository.getAllSongs()
            if (originalQueue.isEmpty() && songs.isNotEmpty()) {
                originalQueue.addAll(songs)
            }

            val mediaItems = songs.map { song ->
                val artworkUri = song.artworkUri ?: ContentUris.withAppendedId(
                    /* contentUri = */ ALBUM_ART_URI.toUri(),
                    /* id = */ song.id
                )

                val description = MediaDescriptionCompat.Builder()
                    .setMediaId(song.id.toString())
                    .setTitle(song.title)
                    .setSubtitle(song.artist)
                    .setIconUri(artworkUri)
                    .build()

                MediaBrowserCompat.MediaItem(
                    /* description = */ description,
                    /* flags = */ MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
                )
            }.toMutableList()

            result.sendResult(mediaItems)
        }
    }

    private fun initializeExoPlayer() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply { addListener(this@PlayerService) }
    }

    override fun onPlaybackStateChanged(state: Int) {
        when (state) {
            Player.STATE_READY -> {
                refreshNotificationAndBroadcast()
                startVisualizerUpdates()
                startProgressUpdates()
            }
            Player.STATE_ENDED -> next()
            else -> {}
        }
    }

    @OptIn(UnstableApi::class)
    private fun startVisualizerUpdates() {
        try { setupVisualizer(exoPlayer.audioSessionId) }
        catch (e: Exception) { Timber.d(e) }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        refreshNotificationAndBroadcast()
        if (isPlaying) {
            stopSelfSabotage()
            startProgressUpdates()
        } else {
            persistCurrentPlaybackState()
            stopProgressUpdates()
            startSelfSabotage()
        }
    }

    private fun initializeMediaSession() {
        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
            .apply { setPackage(packageName) }
        val pendingIntent = PendingIntent.getBroadcast(
            /* context = */ applicationContext,
            /* requestCode = */ 0,
            /* intent = */ mediaButtonIntent,
            /* flags = */ PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        mediaSession = MediaSessionCompat(
            /* context = */ this.applicationContext,
            /* tag = */ "MusickySession",
            /* mbrComponent = */ null,
            /* mbrIntent = */ pendingIntent
        ).apply {
            isActive = true
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
                    mediaId ?: return
                    val songId = mediaId.toLongOrNull() ?: return

                    serviceScope.launch(Dispatchers.IO) {
                        if (originalQueue.isEmpty()) {
                            val songs = songsRepository.getAllSongs()
                            originalQueue.addAll(songs)
                            if (shuffleEnabled) {
                                shuffledQueue.clear()
                                shuffledQueue.addAll(originalQueue.shuffled())
                            }
                        }

                        val index = activeQueue.indexOfFirst { it.id == songId }
                        if (index != -1) {
                            currentIndex = index
                            launch(Dispatchers.Main) {
                                playCurrent()
                            }
                        }
                    }
                }

                override fun onPlayFromSearch(query: String?, extras: Bundle?) {
                    if (query.isNullOrBlank()) {
                        onPlay()
                        return
                    }

                    serviceScope.launch(Dispatchers.IO) {
                        val songs = songsRepository.getAllSongs()
                        if (songs.isEmpty()) return@launch

                        val searchQuery = query.trim().lowercase()
                        val matchedSong = songs.find { it.title.lowercase().contains(searchQuery) }
                            ?: songs.find { it.artist.lowercase().contains(searchQuery) }
                            ?: songs.find { it.album?.lowercase()?.contains(searchQuery) == true }

                        originalQueue.clear()
                        originalQueue.addAll(songs)
                        if (shuffleEnabled) {
                            shuffledQueue.clear()
                            shuffledQueue.addAll(originalQueue.shuffled())
                        }

                        if (matchedSong != null) {
                            currentIndex = activeQueue.indexOfFirst { it.id == matchedSong.id }.takeIf { it != -1 } ?: 0
                        } else {
                            currentIndex = 0
                        }

                        launch(Dispatchers.Main) {
                            playCurrent()
                        }
                    }
                }

                override fun onPlay() = resume()
                override fun onPause() = pause()
                override fun onSkipToNext() = next()
                override fun onSkipToPrevious() = previous()
                override fun onSeekTo(pos: Long) = seekTo(pos)
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

    @OptIn(UnstableApi::class)
    private fun toggleHighRate(intent: Intent) {
        highCaptureRateEnabled = intent.getBooleanExtra(EXTRA_HIGH_CAPTURE_RATE, false)
        startVisualizerUpdates()
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
        val pos = intent.getLongExtra(EXTRA_SEEK_POSITION, 0L)
        seekTo(pos)
    }

    private fun startPlayback(intent: Intent) {
        highCaptureRateEnabled = intent.getBooleanExtra(EXTRA_HIGH_CAPTURE_RATE, false)
        val newQueue = IntentCompat
            .getParcelableArrayListExtra(intent, EXTRA_QUEUE, Song::class.java)
            ?.filterNotNull()
            ?: arrayListOf()
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
        persistCurrentPlaybackState()
        exoPlayer.stop()
        exoPlayer.release()

        removeSongsInfo()
        stopProgressUpdates()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        refreshNotificationAndBroadcast()
    }

    private fun pause() {
        persistCurrentPlaybackState()
        exoPlayer.pause()
        refreshNotificationAndBroadcast()
        stopProgressUpdates()
        startSelfSabotage()
    }

    private fun resume() {
        stopSelfSabotage()
        if (currentIndex == -1 || activeQueue.isEmpty()) {
            restoreLastPlayedAndPlay()
        } else {
            exoPlayer.play()
            refreshNotificationAndBroadcast()
            startProgressUpdates()
        }
    }

    private fun restoreLastPlayedAndPlay() {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val songs = songsRepository.getAllSongs()
                if (songs.isEmpty()) return@launch

                originalQueue.clear()
                originalQueue.addAll(songs)
                if (shuffleEnabled) {
                    shuffledQueue.clear()
                    shuffledQueue.addAll(originalQueue.shuffled())
                }

                val savedSongId = dataStoreManager.lastSongId.firstOrNull()
                val savedPosition = dataStoreManager.lastSongPosition.firstOrNull() ?: 0L

                // Lenient matching: find song by ID, or fallback to first available song in library
                val targetIndex = if (savedSongId != null) {
                    activeQueue.indexOfFirst { it.id == savedSongId }.takeIf { it != -1 } ?: 0
                } else 0

                currentIndex = targetIndex
                val targetSong = activeQueue.getOrNull(currentIndex) ?: return@launch

                // Validate saved position against duration
                val validPosition = if (savedPosition in 0..targetSong.duration) savedPosition else 0L

                launch(Dispatchers.Main) {
                    playCurrent(startPosition = validPosition)
                }
            } catch (e: Exception) {
                debugLog("Error restoring last played song, falling back safely", e)
                if (originalQueue.isNotEmpty()) {
                    currentIndex = 0
                    launch(Dispatchers.Main) { playCurrent() }
                }
            }
        }
    }

    private fun persistCurrentPlaybackState() {
        val currentSong = playState.currentSong ?: return
        val position = exoPlayer.currentPosition
        serviceScope.launch(Dispatchers.IO) {
            try {
                dataStoreManager.saveLastPlayed(currentSong.id, position)
            } catch (e: Exception) {
                Timber.d(e, "Failed to persist last played song state")
            }
        }
    }

    private fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
        refreshNotificationAndBroadcast()
        persistCurrentPlaybackState()
    }

    private fun playCurrent(goingForward: Boolean = true, startPosition: Long = 0L) {
        val song = playState.currentSong ?: return
        startForegroundCompat(NOTIFICATION_ID, buildSongNotification())

        try {
            exoPlayer.apply {
                stop()
                clearMediaItems()
                setMediaItem(MediaItem.fromUri(song.data.toUri()))
                prepare()
                if (startPosition > 0L) {
                    seekTo(startPosition)
                }
                playWhenReady = true
            }

            refreshNotificationAndBroadcast()
            persistCurrentPlaybackState()
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
        val currentSong = playState.currentSong ?: return
        shuffleEnabled = enabled ?: !shuffleEnabled

        if (shuffleEnabled) {
            shuffledQueue.clear()
            shuffledQueue.addAll(originalQueue.shuffled())
        }

        currentIndex = activeQueue.indexOfFirst { it.data == currentSong.data }
        refreshNotificationAndBroadcast()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        cleanUpAudioVisualizer()
        cleanUpMediaPlayer()
        stopProgressUpdates()
    }

    private fun cleanUpAudioVisualizer() {
        audioVisualizer?.release()
        audioVisualizer = null
    }

    private fun cleanUpMediaPlayer() {
        exoPlayer.release()
        mediaSession.release()
    }

    override fun onBind(intent: Intent?): IBinder? = super.onBind(intent)

    private fun sendStateBroadcast() {
        Intent(ACTION_BROADCAST_UPDATE).apply {
            setPackage(packageName)
            putExtra(EXTRA_PLAYING_STATE, playState)
        }.also { intent -> sendBroadcast(intent) }
    }

    private fun removeSongNotification() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun buildSongNotification(): Notification {
        val state = playState
        val song = state.currentSong
        val isPlaying = state.isPlaying
        val channel = NotificationChannelInfo.Playing
        val maxProgress = song?.duration?.toInt() ?: 0
        val progress = state.currentPosition.toInt()

        val playIconResId = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        val shuffleIconResId =
            if (state.isShuffleEnabled) R.drawable.ic_shuffle_on
            else R.drawable.ic_shuffle_off
        val repeatModeResId = when(state.repeatMode) {
            RepeatMode.All -> R.drawable.ic_repeat_all
            RepeatMode.One -> R.drawable.ic_repeat_one
            RepeatMode.None -> R.drawable.ic_repeat_none
        }

        val shuffleAction = Intent(this, PlayerService::class.java)
            .apply { action = ACTION_TOGGLE_SHUFFLE }
            .let { intent -> PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE) }
            .let { pendingIntent -> NotificationCompat.Action(shuffleIconResId, "shuffle", pendingIntent) }
        val prevAction = Intent(this, PlayerService::class.java)
            .apply { action = ACTION_PREVIOUS }
            .let { intent -> PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_IMMUTABLE) }
            .let { pendingIntent -> NotificationCompat.Action(R.drawable.ic_prev, "prev", pendingIntent) }
        val playPauseAction = Intent(this, PlayerService::class.java)
            .apply { action = if (isPlaying) ACTION_PAUSE else ACTION_RESUME }
            .let { intent -> PendingIntent.getService(this, 2, intent, PendingIntent.FLAG_IMMUTABLE) }
            .let { pendingIntent -> NotificationCompat.Action(playIconResId, "play", pendingIntent) }
        val nextAction = Intent(this, PlayerService::class.java)
            .apply { action = ACTION_NEXT }
            .let { intent -> PendingIntent.getService(this, 3, intent, PendingIntent.FLAG_IMMUTABLE) }
            .let { pendingIntent -> NotificationCompat.Action(R.drawable.ic_next, "next", pendingIntent) }
        val repeatAction = Intent(this, PlayerService::class.java)
            .apply { action = ACTION_TOGGLE_REPEAT }
            .let { intent -> PendingIntent.getService(this, 4, intent, PendingIntent.FLAG_IMMUTABLE) }
            .let { pendingIntent -> NotificationCompat.Action(repeatModeResId, "repeat", pendingIntent) }

        val cancelAction = Intent(this, PlayerService::class.java)
            .apply { action = ACTION_STOP }
            .let { intent -> PendingIntent.getService(this, 5, intent, PendingIntent.FLAG_IMMUTABLE) }
//            .let { pendingIntent -> NotificationCompat.Action(R.drawable.ic_close, "cancel", pendingIntent) }

        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setShowCancelButton(true)
            .setCancelButtonIntent(cancelAction)
            .setMediaSession(mediaSession.sessionToken)
            .setShowActionsInCompactView(1, 2, 3)

        val contentIntent = Intent(this, MainActivity::class.java)
            .apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP }

        val pendingIntent = PendingIntent.getActivity(
            /* context = */ this,
            /* requestCode = */ 0,
            /* intent = */ contentIntent,
            /* flags = */ PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, channel.id)
            .setContentTitle(song?.title ?: getString(R.string.no_song_playing))
            .setContentText(song?.artist ?: getString(R.string.unknown))
            .setSmallIcon(android.R.drawable.ic_media_play)
            .addAction(shuffleAction)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .addAction(repeatAction)
            .setStyle(mediaStyle)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            .setContentIntent(pendingIntent)
            .build()
    }

    fun startSelfSabotage() {
        debugLog("started service self sabotage")
        autoKillProcessJob?.cancel()
        autoKillProcessJob = CoroutineScope(Dispatchers.Main).launch {
            delay(AUTO_STOP_TIMEOUT.milliseconds)
            if (!playState.isPlaying) {
                removeSongsInfo()
                refreshNotificationAndBroadcast()
                removeSongNotification()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                this@PlayerService.debugLog("service self unalived")
            }
        }
    }

    private fun removeSongsInfo() {
        currentIndex = -1
        originalQueue.clear()
        shuffledQueue.clear()
    }

    fun stopSelfSabotage() {
        autoKillProcessJob?.let {
            it.cancel()
            debugLog("canceled self sabotage")
        }
        autoKillProcessJob = null
        startForegroundCompat(NOTIFICATION_ID, buildSongNotification())
    }

    private fun startForegroundCompat(id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(id, notification)
        }
    }

    private fun startSleepTimer(duration: Long) {
        autoStopTimeLeft = null
        autoPauseTimerJob?.cancel()
        autoPauseTimerJob = CoroutineScope(Dispatchers.Main).launch {
            var timeLeft = duration
            autoStopTimeLeft = timeLeft

            while (isActive && timeLeft > 0) {
                delay(REFRESH_FREQUENCY.milliseconds)
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
        progressJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                refreshNotificationAndBroadcast()
                delay(REFRESH_FREQUENCY.milliseconds)
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
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, buildSongNotification())
        updateMediaSessionMetadata(playState)
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

        val mediaState = when (state.isPlaying) {
            true -> PlaybackStateCompat.STATE_PLAYING
            else -> PlaybackStateCompat.STATE_PAUSED
        }

        val shuffleModeCompat = when (state.isShuffleEnabled) {
            true -> PlaybackStateCompat.SHUFFLE_MODE_ALL
            else -> PlaybackStateCompat.SHUFFLE_MODE_NONE
        }

        val repeatModeCompat = when (state.repeatMode) {
            RepeatMode.All -> PlaybackStateCompat.REPEAT_MODE_ALL
            RepeatMode.One -> PlaybackStateCompat.REPEAT_MODE_ONE
            RepeatMode.None -> PlaybackStateCompat.REPEAT_MODE_NONE
        }

        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SEEK_TO or
                PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE or
                PlaybackStateCompat.ACTION_SET_REPEAT_MODE or
                PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH or
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
            setShuffleMode(shuffleModeCompat)
            setRepeatMode(repeatModeCompat)
        }
    }

    @OptIn(UnstableApi::class)
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
                        if (!exoPlayer.isPlaying) return
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

    companion object {
        private const val ROOT_MEDIA_ID = "musicky_root"
        private const val ALBUM_ART_URI = "content://media/external/audio/albumart"

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

        const val AUTO_STOP_TIMEOUT = 10 * 60 * 1_000L // minutes * seconds * millis

        const val NOTIFICATION_ID = 69420
    }
}
