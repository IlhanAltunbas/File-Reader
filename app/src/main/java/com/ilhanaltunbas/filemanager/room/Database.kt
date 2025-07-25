package com.ilhanaltunbas.filemanager.room

import androidx.room.RoomDatabase
import androidx.room.Database
import com.ilhanaltunbas.filemanager.data.entity.Files

@Database(entities = [Files::class], version = 1)
abstract class Database : RoomDatabase() {
    abstract fun getFilesDao(): FilesDao
}