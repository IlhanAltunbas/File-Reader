package com.ilhanaltunbas.filemanager.data.datasource

import android.util.Log
import com.ilhanaltunbas.filemanager.data.entity.Files
import com.ilhanaltunbas.filemanager.room.FilesDao
import com.ilhanaltunbas.filemanager.uix.SearchType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FilesDataSource(var fdao: FilesDao) {


    suspend fun delete(id: Int) {
        val deletedFile = Files(id,"","",0,"",0,"","")
        fdao.delete(deletedFile)
    }
    suspend fun searchFilesByName(searchKey: String, searchType: SearchType): List<Files> = withContext(Dispatchers.IO) {
        return@withContext when (searchType) {
            SearchType.CONTAINS -> fdao.searchFilesByNameContaining(searchKey)
            SearchType.STARTS_WITH -> fdao.searchFilesByNameStartingWith(searchKey)
            SearchType.ENDS_WITH -> fdao.searchFilesByNameEndingWith(searchKey)
        }
    }

    suspend fun insertFile(file: Files) {
        fdao.insertFile(file)

    }



    suspend fun loadFiles(): List<Files> = withContext(Dispatchers.IO) {
        return@withContext fdao.loadFiles()
    }
    suspend fun getFileById(id: Int) : Files? = withContext(Dispatchers.IO) {
        return@withContext fdao.getFileById(id)
    }
}