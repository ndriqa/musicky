package com.ndriqa.musicky.di

import android.content.Context
import androidx.room.Room
import com.ndriqa.musicky.data.local.SongsDao
import com.ndriqa.musicky.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context = context,
            klass = AppDatabase::class.java,
            name = DATABASE_NAME
        ).build()
    }

    @Provides
    fun provideSongsDao(database: AppDatabase): SongsDao {
        return database.songsDao()
    }

    private const val DATABASE_NAME = "musicky_db"
}
