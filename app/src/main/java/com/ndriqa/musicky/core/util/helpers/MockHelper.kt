package com.ndriqa.musicky.core.util.helpers

import android.net.Uri
import androidx.core.net.toUri
import com.ndriqa.musicky.core.data.AudioFeatures
import com.ndriqa.musicky.core.data.FftFeatures
import com.ndriqa.musicky.core.data.PlayingState
import com.ndriqa.musicky.core.data.Song

object MockHelper {

    fun getMockPlayingState(): PlayingState {
        val song = getMockSong()
        return PlayingState(
            isPlaying = true,
            currentSong = song,
            currentPosition = song.duration / 3,
            bufferedPosition = 1,
            isPrepared = true
        )
    }

    fun getMockSong(): Song {
        return Song(
            id = 1,
            title = "Mpuq Mpuq",
            artist = "Genta Ismajli",
            album = "Lule Bore",
            duration = 3 * 60 * 1000,
            dateAdded = "sot",
            dateModified = "sot",
            data = "path/to/song",
            artworkUri = getMockArtworkUri()
        )
    }

    fun getMockWaveform(): ByteArray {
        return ByteArray(500) { (Byte.MIN_VALUE..Byte.MAX_VALUE).random().toByte() }
    }

    fun getMockAudioFeatures(): AudioFeatures {
        return AudioFeatures()
    }

    fun getMockFftFeatures(): FftFeatures {
        return FftFeatures()
    }

    fun getMockArtworkUri(): Uri {
        return "https://ndriqa.com/assets/brand/logo.png".toUri()
    }
}