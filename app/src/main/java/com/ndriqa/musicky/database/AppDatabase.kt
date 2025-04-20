package com.ndriqa.musicky.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ndriqa.musicky.core.data.Song
import com.ndriqa.musicky.data.local.SongsDao
import com.ndriqa.musicky.database.converters.UriTypeConverter

@Database(
    entities = [Song::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(UriTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songsDao(): SongsDao
}
