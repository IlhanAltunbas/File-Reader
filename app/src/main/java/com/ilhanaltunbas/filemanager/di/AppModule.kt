package com.ilhanaltunbas.filemanager.di

import android.content.Context
import androidx.room.Room
import com.ilhanaltunbas.filemanager.data.datasource.FilesDataSource
import com.ilhanaltunbas.filemanager.data.repo.FilesRepository
import com.ilhanaltunbas.filemanager.room.Database
import com.ilhanaltunbas.filemanager.room.FilesDao
import com.ilhanaltunbas.filemanager.uix.FileReader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {
    @Provides
    @Singleton
    fun provideFilesRepository(fds: FilesDataSource) : FilesRepository {
        return FilesRepository(fds)
    }
    @Provides
    @Singleton
    fun provideFilesDataSource(fdao: FilesDao) : FilesDataSource {
        return FilesDataSource(fdao)
    }

    @Provides
    @Singleton
    fun provideFilesDao(@ApplicationContext context: Context) : FilesDao {
        val db = Room.databaseBuilder(context, Database::class.java,"manager.sqlite")
            .createFromAsset("manager.sqlite").build()
        return db.getFilesDao()
    }

    @Provides
    @Singleton
    fun provideFileReader(@ApplicationContext context: Context) : FileReader {
        return FileReader(context)
    }


}