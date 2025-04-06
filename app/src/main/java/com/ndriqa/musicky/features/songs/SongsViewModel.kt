package com.ndriqa.musicky.features.songs

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import com.ndriqa.musicky.core.data.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import com.ndriqa.musicky.core.util.extensions.simpleLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class SongsViewModel @Inject constructor(
    @ApplicationContext context: Context
): ViewModel() {
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs = _songs.asStateFlow()

    fun startLoadingSongs(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            getAllSongs(context).let { foundSongs ->
                _songs.update { foundSongs }
                simpleLog("Songs found: ${foundSongs.size}")
            }
        }
    }

    private fun getAllSongs(context: Context): List<Song> {
        val songList = mutableListOf<Song>()

        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} ASC"

        context.contentResolver.query(uri, projection, selection, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateAdded = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val duration = cursor.getLong(durationCol)
                if (duration < 45_000) continue // skip short clips

                songList += Song(
                    id = cursor.getLong(idCol),
                    title = cursor.getString(titleCol)?.trim() ?: "Unknown Title",
                    artist = cursor.getString(artistCol) ?: "Unknown Artist",
                    album = cursor.getString(albumCol),
                    duration = duration,
                    dateAdded = cursor.getString(dateAdded),
                    data = cursor.getString(dataCol),
                    artworkUri = ContentUris.withAppendedId(
                        /* contentUri = */ "content://media/external/audio/albumart".toUri(),
                        /* id = */ cursor.getLong(albumIdCol)
                    )
                )
            }
        }

        return songList
    }
}