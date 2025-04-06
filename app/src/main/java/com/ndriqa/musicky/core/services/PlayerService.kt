package com.ndriqa.musicky.core.services

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder

private const val ACTION_PLAY: String = "com.ndriqa.action.PLAY"
private const val ACTION_PAUSE: String = "com.ndriqa.action.PAUSE"
private const val ACTION_RESUME: String = "com.ndriqa.action.RESUME"
private const val ACTION_STOP: String = "com.ndriqa.action.STOP"

private const val EXTRA_SONG_PATH: String = "EXTRA_SONG_PATH"

class PlayerService: Service(), MediaPlayer.OnPreparedListener {

    private var mMediaPlayer: MediaPlayer? = null
    private var currentSongPath: String? = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when(intent.action) {
            ACTION_PLAY -> {
                mMediaPlayer = MediaPlayer()
                mMediaPlayer?.apply {
                    val songPath = intent.getStringExtra(EXTRA_SONG_PATH)
                    songPath?.let { playSong(it) }

                    setOnPreparedListener(this@PlayerService)
                    prepareAsync() // prepare async to not block main thread
                }
            }

            ACTION_PAUSE -> mMediaPlayer?.pause()
            ACTION_RESUME -> mMediaPlayer?.start()
            ACTION_STOP -> stopSelf()
        }

        return START_STICKY
    }

    private fun playSong(path: String) {
        mMediaPlayer?.release()
        mMediaPlayer = MediaPlayer().apply {
            setDataSource(path)
            setOnPreparedListener(this@PlayerService)
            prepareAsync()
        }
        currentSongPath = path
//        startForeground(NOTIFICATION_ID, buildNotification("Loading..."))
    }

    override fun onPrepared(mediaPlayer: MediaPlayer) {
        mediaPlayer.start()
//        startForeground(NOTIFICATION_ID, buildNotification("Now playing"))
    }

    override fun onDestroy() {
        super.onDestroy()
        mMediaPlayer?.release()
        mMediaPlayer = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
