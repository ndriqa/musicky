package com.ndriqa.musicky.features.songs

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ndriqa.musicky.R
import com.ndriqa.musicky.core.data.Album
import com.ndriqa.musicky.core.data.Song
import com.ndriqa.musicky.core.data.SortingMode
import com.ndriqa.musicky.core.preferences.DataStoreManager
import com.ndriqa.musicky.core.util.extensions.contains
import com.ndriqa.musicky.data.repositories.SongsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import javax.inject.Inject

@HiltViewModel
class SongsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val dataStoreManager: DataStoreManager,
    private val songsRepository: SongsRepository
): ViewModel() {
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    private val _searchText = MutableStateFlow("")
    private val _preferredSortingMode = dataStoreManager.preferredSortingMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, SortingMode.Default)

    val query = _searchText.asStateFlow()
    val allSongs = combine(_songs, _preferredSortingMode) { songs, sortingMode ->
        when (sortingMode) {
            SortingMode.Default -> songs // no sort
            SortingMode.DateAsc -> songs.sortedBy { it.dateAdded }
            SortingMode.DateDesc -> songs.sortedByDescending { it.dateAdded }
            SortingMode.NameAsc -> songs.sortedBy { it.title.lowercase() }
            SortingMode.NameDesc -> songs.sortedByDescending { it.title.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    val songs = combine(allSongs, _searchText) { allSongs, query ->
        allSongs.filter { it.contains(query) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val albums = combine(allSongs, _searchText) { allSongs, query ->
        allSongs
            .groupBy { it.album }
            .map { Album(
                name = it.key ?: context.getString(R.string.unknown),
                songs = it.value
            ) }
            .filter { it.contains(query) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val requestScopedDelete = MutableSharedFlow<Uri?>()

    val minAudioLength = dataStoreManager.minAudioLength
        .stateIn(viewModelScope, SharingStarted.Eagerly, DataStoreManager.DEFAULT_MIN_AUDIO_LENGTH)

    fun startLoadingSongs(context: Context) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            getCachedSongs().let { cachedSongs ->
                _songs.value = cachedSongs
            }

            getAllSongs(context).let { foundSongs ->
                _songs.value = foundSongs
                songsRepository.syncSongs(foundSongs)
            }

            _isLoading.value = false
        }
    }

    private suspend fun getCachedSongs(): List<Song> {
        return songsRepository.getAllSongs()
    }

    private suspend fun getAllSongs(context: Context): List<Song> {
        /**
         * delay needed to avoid rare race condition where content access crashes
         * system may take a few ms to finalize permission grants internally after user accepts
         * happens more often on old Android versions (e.g., API 26) or slower devices
         */
        delay(100)
        val songList = mutableListOf<Song>()

        val uri = getAudioMediaUriCompat()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"

        context.contentResolver.query(uri, projection, selection, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateAdded = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val dateModified = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val duration = cursor.getLong(durationCol)
                if (duration < minAudioLength.value * 1000) continue // skip short clips

                val potentialArtworkUri = ContentUris.withAppendedId(
                    /* contentUri = */ "content://media/external/audio/albumart".toUri(),
                    /* id = */ cursor.getLong(albumIdCol)
                )

                val artworkUri = try {
                    context.contentResolver
                        .openFileDescriptor(potentialArtworkUri, "r")
                        ?.use { potentialArtworkUri }
                } catch (e: FileNotFoundException) { null }

                songList += Song(
                    id = cursor.getLong(idCol),
                    title = cursor.getString(titleCol)?.trim() ?: "Unknown Title",
                    artist = cursor.getString(artistCol) ?: "Unknown Artist",
                    album = cursor.getString(albumCol),
                    duration = duration,
                    dateAdded = cursor.getString(dateAdded),
                    dateModified = cursor.getString(dateModified),
                    data = cursor.getString(dataCol),
                    artworkUri = artworkUri
                )
            }
        }

        return songList
    }

    private fun getAudioMediaUriCompat(): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getSafeAudioMediaUri()
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getSafeAudioMediaUri(): Uri {
        return try {
            getAudioMediaUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } catch (e: IllegalArgumentException) {
            getAudioMediaUri(MediaStore.VOLUME_EXTERNAL)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getAudioMediaUri(volume: String): Uri {
        return MediaStore.Audio.Media.getContentUri(volume)
    }

    fun tryDeleteSongFile(context: Context, song: Song) {
        viewModelScope.launch {
            songsRepository.deleteSong(song)

            val songUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
            if (deleteSongFile(context, songUri)) {
                /**
                 * on older devices, < Android 10, we don't go through mediastore
                 * so it gets deleted directly, which means, we can remove it
                 * directly from the list without the need to refresh the whole list.
                 * on the contrary, >= Android 10, we refresh the songs list on
                 * [SongsScreen]'s launcher after song removal is done*/
                removeSongFromList(song)
            }
        }
    }

    fun removeSongFromList(song: Song) {
        _songs.update { _songs.value.filterNot { it.id == song.id } }
    }

    suspend fun deleteSongFile(context: Context, uri: Uri): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                requestScopedDelete.emit(uri)
                false
            } else {
                val deleted = context.contentResolver.delete(uri, null, null) > 0
                deleted
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun onSearch(query: String) {
        _searchText.value = query
    }

    fun resetSearch() {
        _searchText.value = ""
    }

    fun clearDeleteRequest() {
        viewModelScope.launch {
            requestScopedDelete.emit(null)
        }
    }

    companion object {
        const val DELETE_SONG_REQUEST_CODE = 6969
    }
}