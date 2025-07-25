package com.ilhanaltunbas.filemanager.data.repo

import android.util.Log
import com.ilhanaltunbas.filemanager.data.datasource.FilesDataSource
import com.ilhanaltunbas.filemanager.data.entity.Files
import com.ilhanaltunbas.filemanager.uix.SearchType

class FilesRepository(var fds : FilesDataSource) {

    suspend fun searchFilesByName(searchKey: String, searchType: SearchType): List<Files> {
        return fds.searchFilesByName(searchKey, searchType)
    }

    suspend fun delete(id: Int) {
        fds.delete(id)
    }

    suspend fun loadFiles(): List<Files> = fds.loadFiles()

    suspend fun insertFile(file: Files) {
        fds.insertFile(file)
    }
    suspend fun getFileById(id: Int) : Files? {
        return fds.getFileById(id)
    }
}