package com.ndriqa.musicky.di

import com.ndriqa.musicky.data.repositories.SongsRepo
import com.ndriqa.musicky.data.repositories.SongsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSongsRepository(
        impl: SongsRepo
    ): SongsRepository
}
