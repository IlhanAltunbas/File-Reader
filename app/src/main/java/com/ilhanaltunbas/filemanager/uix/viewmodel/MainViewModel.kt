package com.ilhanaltunbas.filemanager.uix.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilhanaltunbas.filemanager.data.entity.Files
import com.ilhanaltunbas.filemanager.data.repo.FilesRepository
import com.ilhanaltunbas.filemanager.uix.SearchType
import com.tom_roush.pdfbox.pdmodel.PDDocument
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.text.substringAfterLast
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.hpsf.SummaryInformation as HSSFSummaryInformation // .xls için meta veri (isim çakışmasını önlemek için alias)


@HiltViewModel
class MainViewModel @Inject constructor(
    var frepo: FilesRepository,
    @ApplicationContext private val applicationContext: Context
) : ViewModel() {

    private val _filesList = mutableStateOf<List<Files>>(emptyList())
    val filesList: State<List<Files>> = _filesList
    private val _searchKey = mutableStateOf("")
    val searchKey: State<String> = _searchKey

    private val _selectedSearchType = mutableStateOf(SearchType.CONTAINS) // Varsayılan CONTAINS
    val selectedSearchType: State<SearchType> = _selectedSearchType

    // UI'daki arama modunu (TextField'ın görünüp görünmemesi) kontrol etmek için
    private val _isSearching = mutableStateOf(false)
    val isSearching: State<Boolean> = _isSearching

    init {
        // ViewModel başladığında tüm dosyaları yükle
        fetchAllFilesToList()
    }

    // Bu fonksiyon tüm dosyaları yükler ve filesList'i günceller
    private fun fetchAllFilesToList() {
        viewModelScope.launch {
            _filesList.value = frepo.loadFiles() // Mevcut loadFiles fonksiyonunuzu kullanın
        }
    }

    fun onSearchKeyChanged(newSearchKey: String) {
        _searchKey.value = newSearchKey
        performSearch()
    }

    fun onSearchTypeChanged(newSearchType: SearchType) {
        _selectedSearchType.value = newSearchType
        performSearch() // Arama türü değiştiğinde de aramayı tetikle
    }

    fun toggleSearch() {
        val newIsSearchingState = !_isSearching.value
        _isSearching.value = newIsSearchingState

        if (!newIsSearchingState) {
            if (_searchKey.value.isNotEmpty()) {
                _searchKey.value = ""
            }
            fetchAllFilesToList()
        } else {
        }
    }

    private fun performSearch() {
        val currentQuery = _searchKey.value
        val currentSearchType = _selectedSearchType.value


        if (currentQuery.isBlank()) {
            fetchAllFilesToList()
            return
        }

        viewModelScope.launch {
            _filesList.value = frepo.searchFilesByName(currentQuery, currentSearchType)
        }
    }

    fun delete(id: Int) {
        viewModelScope.launch {
            frepo.delete(id)
            fetchAllFilesToList()
        }
    }
    fun processSelectedFile(uri: Uri) {
        viewModelScope.launch {
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                applicationContext.contentResolver.takePersistableUriPermission(uri, takeFlags)

                var fileName: String? = null
                var fileSize: Long = 0L
                var lastModifiedFromCursor: Long = 0L

                val projection = arrayOf(
                    OpenableColumns.DISPLAY_NAME,
                    OpenableColumns.SIZE,
                    MediaStore.MediaColumns.DATE_MODIFIED
                )

                applicationContext.contentResolver.query(uri, projection, null, null, null)
                    ?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1) {
                                fileName = cursor.getString(nameIndex)
                            }

                            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                            if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                                fileSize = cursor.getLong(sizeIndex)
                            }

                            val dateModifiedColumnIndex =
                                cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                            if (dateModifiedColumnIndex != -1 && !cursor.isNull(dateModifiedColumnIndex)) {
                                lastModifiedFromCursor = cursor.getLong(dateModifiedColumnIndex) * 1000L
                            }
                        }
                    }

                if (fileName != null) {
                    Log.i("MainViewModel", "İşlenen dosya: Adı=$fileName, Boyut=$fileSize, URI=$uri")

                    val extension = getFileExtension(fileName)
                    val finalMimeForDb = extension.ifEmpty {
                        val mimeTypeFromResolverOriginal: String? = applicationContext.contentResolver.getType(uri)
                        Log.d("MIME_DEBUG", "Original MimeType from Resolver: $mimeTypeFromResolverOriginal for $fileName")
                        val resolvedMime = mimeTypeFromResolverOriginal
                        if (resolvedMime != null && resolvedMime.contains('/')) {
                            resolvedMime.substringAfterLast('/')
                        } else {
                            "file"
                        }
                    }
                    Log.d("MIME_DEBUG", "Final MimeType for DB: $finalMimeForDb for $fileName")

                    val finalLastModified = if (lastModifiedFromCursor > 0L) {
                        lastModifiedFromCursor
                    } else {
                        System.currentTimeMillis()
                    }

                    // yazar
                    var authorNameFromMeta: String = ""
                    if (fileName.isNotBlank()) { // fileName null olamaz ama yine de kontrol
                        authorNameFromMeta = withContext(Dispatchers.IO) {
                            var author: String? = null
                            try {
                                applicationContext.contentResolver.openInputStream(uri)?.use { inputStream ->
                                    when (extension.lowercase()) {
                                        "pdf" -> {
                                            PDDocument.load(inputStream).use { document ->
                                                author = document.documentInformation.author
                                            }
                                            Log.d("MainViewModel", "PDF Author for $fileName: $author")
                                        }
                                        "doc" -> {
                                            HWPFDocument(inputStream).use { document ->
                                                val summaryInfo: HSSFSummaryInformation? = document.summaryInformation
                                                author = summaryInfo?.author // veya lastAuthor
                                            }
                                            Log.d("MainViewModel", "DOC Author for $fileName: $author")
                                        }
                                        "xls" -> {
                                            HSSFWorkbook(inputStream).use { workbook ->
                                                val summaryInfo: HSSFSummaryInformation? = workbook.summaryInformation
                                                author = summaryInfo?.author // veya lastAuthor
                                            }
                                            Log.d("MainViewModel", "XLS Author for $fileName: $author")
                                        }
                                        "txt" -> {
                                            author = ""
                                            Log.d("MainViewModel", "TXT file, no standard author metadata for $fileName")
                                        }
                                        else -> {
                                            Log.d("MainViewModel", "Author extraction not supported for extension: $extension for $fileName")
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("MainViewModel", "Error reading author for $fileName (extension: $extension): ${e.message}", e)

                            }
                            author ?: ""
                        }
                    }

                    val newFileEntry = Files(
                        id = 0,
                        name = fileName,
                        mimeType = finalMimeForDb,
                        sizeInBytes = fileSize,
                        path = uri.toString(),
                        lastModifiedTimeStamp = finalLastModified,
                        owner = "ilhan",
                        author = authorNameFromMeta
                    )

                    try {
                        withContext(Dispatchers.IO) {
                            frepo.insertFile(newFileEntry)
                        }
                        Log.i("MainViewModel", "Dosya meta verileri kaydedildi: $fileName")
                        fetchAllFilesToList()
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Dosya kaydedilirken hata: $uri", e)
                    }

                } else {
                    Log.e("MainViewModel", "Dosya adı URI'den alınamadı: $uri")
                }

            } catch (e: SecurityException) {
                Log.e("MainViewModel", "Kalıcı izin/dosya işleme güvenlik hatası: $uri", e)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Dosya işlenirken genel hata: $uri", e)
            }
        }
    }


    private fun getFileExtension(fileName: String): String {
        val lastDotIndex = fileName.lastIndexOf('.')
        return if (lastDotIndex != -1 && lastDotIndex < fileName.length - 1) {
            fileName.substring(lastDotIndex + 1)
        } else {
            ""
        }
    }
}