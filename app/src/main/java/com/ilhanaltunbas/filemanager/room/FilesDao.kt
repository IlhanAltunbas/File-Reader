package com.ilhanaltunbas.filemanager.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.ilhanaltunbas.filemanager.data.entity.Files

@Dao
interface FilesDao {
    @Query("SELECT * FROM files")
    suspend fun loadFiles(): List<Files>

    @Query("SELECT * FROM files WHERE name LIKE '%' || :searchKey || '%' ")
    suspend fun searchFilesByNameContaining(searchKey: String): List<Files>

    // STARTS_WITH için yeni sorgu
    @Query("SELECT * FROM files WHERE name LIKE :searchKey || '%' ")
    suspend fun searchFilesByNameStartingWith(searchKey: String): List<Files>

    // ENDS_WITH için yeni sorgu
    @Query("SELECT * FROM files WHERE name LIKE '%' || :searchKey")
    suspend fun searchFilesByNameEndingWith(searchKey: String): List<Files>

    @Delete
    suspend fun delete(file: Files)

    @Insert
    suspend fun insertFile(file: Files)

    @Query("SELECT * FROM files WHERE id = :id")
    suspend fun getFileById(id: Int): Files
}