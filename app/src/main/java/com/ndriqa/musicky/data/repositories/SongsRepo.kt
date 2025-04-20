package com.ndriqa.musicky.data.repositories

import android.content.Context
import com.ndriqa.musicky.core.data.Song
import com.ndriqa.musicky.data.local.SongsDao
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface SongsRepository {
    suspend fun getAllSongs(): List<Song>
    suspend fun insertSong(song: Song)
    suspend fun insertSongs(songs: List<Song>)
    suspend fun deleteSong(song: Song)
    suspend fun syncSongs(systemSongs: List<Song>)
}

class SongsRepo @Inject constructor(
    @ApplicationContext context: Context,
    private val songsDao: SongsDao
): SongsRepository {
    override suspend fun getAllSongs(): List<Song> {
        return songsDao.getAllSongs()
    }

    override suspend fun insertSong(song: Song) {
        songsDao.insertSong(song)
    }

    override suspend fun insertSongs(songs: List<Song>) {
        songsDao.insertSongs(songs)
    }

    override suspend fun deleteSong(song: Song) {
        songsDao.deleteSong(song)
    }

    override suspend fun syncSongs(systemSongs: List<Song>) {
        val dbSongs = songsDao.getAllSongs()

        if (!areSongsEqual(systemSongs, dbSongs)) {
            songsDao.deleteAllSongs()
            songsDao.insertSongs(systemSongs)
        }
    }

    private fun areSongsEqual(list1: List<Song>, list2: List<Song>): Boolean {
        if (list1.size != list2.size) return false
        return list1.sortedBy { it.id } == list2.sortedBy { it.id }
    }
}


