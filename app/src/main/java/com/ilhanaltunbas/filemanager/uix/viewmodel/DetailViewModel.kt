package com.ilhanaltunbas.filemanager.uix.viewmodel

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.State
import com.ilhanaltunbas.filemanager.data.entity.Files
import com.ilhanaltunbas.filemanager.data.repo.FilesRepository
import com.ilhanaltunbas.filemanager.uix.FileReader
import com.ilhanaltunbas.filemanager.uix.SearchType
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val frepo: FilesRepository, // val olarak değiştirmek daha iyi olabilir
    private val fileReader: FileReader,
    @ApplicationContext private val applicationContext: Context
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<String>>(emptyList())
    val searchResults: StateFlow<List<String>> = _searchResults.asStateFlow()

    private val _fileContent = MutableLiveData<String?>()
    val fileContent: LiveData<String?> get() = _fileContent

    private var originalFullContentForSearch: String? = null




    private val _selectedSearchType = mutableStateOf(SearchType.CONTAINS)
    val selectedSearchType: State<SearchType> = _selectedSearchType

    fun updateSearchType(newSearchType: SearchType) {
        _selectedSearchType.value = newSearchType
    }


    fun loadFileContentById(fileId: Int?) {
        if (fileId == null) {
            Log.e("DetailViewModel", "File ID is null. Cannot load content.")
            _fileContent.postValue("Hata: Dosya ID'si alınamadı.")
            originalFullContentForSearch = null
            _searchResults.value = emptyList()
            return
        }

        Log.d("DetailViewModel", "Loading content for file ID: $fileId")


        viewModelScope.launch {
            var tempFileCreated: File? = null
            try {
                val fileEntity: Files? = frepo.getFileById(fileId)

                if (fileEntity == null) {
                    Log.e("DetailViewModel", "File not found in repository for ID: $fileId")
                    _fileContent.postValue("Dosya bulunamadı.")
                    originalFullContentForSearch = null
                    _searchResults.value = emptyList()
                    return@launch
                }

                val pathString = fileEntity.path
                if (pathString.isBlank()) {
                    Log.e("DetailViewModel", "File path is blank for ID: $fileId, Name: ${fileEntity.name}")
                    _fileContent.postValue("Dosya yolu bulunamadı.")
                    originalFullContentForSearch = null
                    _searchResults.value = emptyList()
                    return@launch
                }
                Log.d("DetailViewModel", "Path string from DB for ${fileEntity.name}: $pathString")

                val fileToRead: File = withContext(Dispatchers.IO) {
                    when {
                        pathString.startsWith("content://") -> {
                            Log.d("DetailViewModel", "Path is a content URI. Creating temp file.")
                            val fileUri = Uri.parse(pathString)
                            tempFileCreated = getFileFromUriHelper(applicationContext.contentResolver, fileUri, applicationContext.cacheDir)
                            tempFileCreated ?: throw IOException("URI'den geçici dosya oluşturulamadı: $fileUri")
                        }
                        pathString.startsWith("/") -> {
                            Log.d("DetailViewModel", "Path is a direct file system path.")
                            val directFile = File(pathString)
                            if (!directFile.exists() || !directFile.canRead()) {
                                throw java.io.FileNotFoundException("Dosya mevcut değil veya okunamıyor: $pathString")
                            }
                            directFile
                        }
                        else -> {
                            Log.e("DetailViewModel", "Unknown path string format: $pathString")
                            throw IllegalArgumentException("Bilinmeyen dosya yolu formatı: $pathString")
                        }
                    }
                }

                Log.d("DetailViewModel", "Reading file: ${fileToRead.absolutePath}, exists: ${fileToRead.exists()}")
                val content = withContext(Dispatchers.IO) {
                    fileReader.readFile(fileToRead)
                }


                if (content != null) {
                    originalFullContentForSearch = content
                    _fileContent.postValue(content)
                    Log.d("DetailVM_Load", "Content loaded successfully. originalFullContentForSearch length: ${originalFullContentForSearch?.length}. First 100 chars: ${originalFullContentForSearch?.take(100)}")
                } else {
                    originalFullContentForSearch = null
                    _fileContent.postValue("Dosya içeriği okunamadı veya boş.")
                    Log.w("DetailVM_Load", "Content is NULL after reading file: ${fileToRead.absolutePath}")
                }
                _searchResults.value = emptyList()

            } catch (e: Exception) {
                Log.e("DetailViewModel", "Error loading file content for ID: $fileId", e)
                _fileContent.postValue("Hata: ${e.localizedMessage ?: "Dosya içeriği okunamadı."}")
                originalFullContentForSearch = null
                _searchResults.value = emptyList()
            } finally {
                tempFileCreated?.let {

                }
            }
        }
    }

    fun searchInCurrentFileContent(searchKey: String) {
        Log.d("DetailVM_Entry", "searchInCurrentFileContent ENTERED. searchKey: '$searchKey'")

        val currentContentSnapshot = originalFullContentForSearch

        if (searchKey.isBlank() || currentContentSnapshot.isNullOrBlank()) {
            Log.d("DetailVM_EarlyExit", "Early exit: searchKey blank or no original content. searchKey: '$searchKey', originalContentIsNull: ${currentContentSnapshot == null}, originalContentIsBlank: ${currentContentSnapshot?.isBlank() ?: "N/A"}") // <<-- BU LOGU KONTROL EDİN
            _searchResults.value = emptyList()
            return
        }

        Log.d("DetailViewModel", "Searching for '$searchKey' with type: ${_selectedSearchType.value}")

        viewModelScope.launch(Dispatchers.Default) {
            val results = mutableListOf<String>()
            val lines = currentContentSnapshot.lines()

            lines.forEach { line ->
                val matchFound = when (_selectedSearchType.value) {
                    SearchType.CONTAINS -> line.contains(searchKey, ignoreCase = true)
                    SearchType.STARTS_WITH -> line.trim().startsWith(searchKey, ignoreCase = true)
                    SearchType.ENDS_WITH -> line.trim().endsWith(searchKey, ignoreCase = true)
                }

                if (matchFound) {
                    if (_selectedSearchType.value == SearchType.CONTAINS) {
                        var index = line.indexOf(searchKey, ignoreCase = true)
                        while (index != -1) {
                            val start = maxOf(0, index - 20) // Önceki -20 karakter
                            val end = minOf(line.length, index + searchKey.length + 20) // Sonraki +20 karakter
                            results.add("...${line.substring(start, end)}...")
                            index = line.indexOf(searchKey, index + searchKey.length, ignoreCase = true)
                        }
                    } else {
                        results.add(line)
                    }
                }
            }
            _searchResults.value = results
            Log.d("DetailViewModel", "arama sonucu ${results.size} ")
        }
    }

    companion object {
        @Throws(IOException::class)
        fun getFileFromUriHelper(contentResolver: ContentResolver, uri: Uri, cacheDir: File): File? {
            val fileExtension = getFileExtensionFromUriHelper(contentResolver, uri)
            val tempFile = File.createTempFile("temp_file_${System.currentTimeMillis()}_", ".$fileExtension", cacheDir)
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(tempFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: return null
                return tempFile
            } catch (e: Exception) {
                Log.e("DetailViewModel_FileUtils", "Uri ile geciçi dosya oluşturulamadı: $uri", e)
                tempFile.delete()
                throw e
            }
        }

        private fun getFileExtensionFromUriHelper(contentResolver: ContentResolver, uri: Uri): String {
            return contentResolver.getType(uri)?.substringAfterLast('/')
                ?: uri.lastPathSegment?.substringAfterLast('.')
                ?: "tmp"
        }
    }
}