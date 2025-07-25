package com.ilhanaltunbas.filemanager.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "files")
data class Files (
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")  var id: Int,
    @ColumnInfo(name = "name")  var name: String,
    @ColumnInfo(name = "mimeType")  var mimeType: String,
    @ColumnInfo(name = "sizeInBytes")  var sizeInBytes: Long,
    @ColumnInfo(name = "path")  var path: String,
    @ColumnInfo(name = "lastModifiedTimeStamp")  var lastModifiedTimeStamp: Long,
    @ColumnInfo(name = "owner")  var owner: String,
    @ColumnInfo(name = "author")  var author: String) {
}