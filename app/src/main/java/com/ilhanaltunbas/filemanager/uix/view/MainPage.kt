package com.ilhanaltunbas.filemanager.uix.view

import java.text.SimpleDateFormat
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ilhanaltunbas.filemanager.R
import com.ilhanaltunbas.filemanager.data.entity.Files
import com.ilhanaltunbas.filemanager.uix.SearchType
import com.ilhanaltunbas.filemanager.uix.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPage(
    navController: NavController,
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val filesList by mainViewModel.filesList
    val searchKey by mainViewModel.searchKey
    val isSearching by mainViewModel.isSearching

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var searchTypeMenuExpanded by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let { selectedFileUri ->
                Log.d("MainPage", "Dosya seçildi (OpenDocument) URI: $selectedFileUri")
                mainViewModel.processSelectedFile(selectedFileUri)
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextField(
                                value = searchKey,
                                onValueChange = { mainViewModel.onSearchKeyChanged(it) },
                                placeholder = { Text(text = "Ara...") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    keyboardController?.hide()
                                }),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                            Box(modifier = Modifier.padding(start = 4.dp)) {
                                IconButton(onClick = { searchTypeMenuExpanded = true }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.outline_more_vert_24),
                                        contentDescription = "Arama Türünü Seç"
                                    )
                                }
                                DropdownMenu(
                                    expanded = searchTypeMenuExpanded,
                                    onDismissRequest = { searchTypeMenuExpanded = false }
                                ) {
                                    SearchType.values().forEach { type ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(text = when (type) {
                                                    SearchType.CONTAINS -> "İçeren"
                                                    SearchType.STARTS_WITH -> "Başında Başlayan"
                                                    SearchType.ENDS_WITH -> "Sonunda Biten"
                                                })
                                            },
                                            onClick = {
                                                mainViewModel.onSearchTypeChanged(type)
                                                searchTypeMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(text = "File Manager")
                    }
                },
                actions = {
                    IconButton(onClick = { mainViewModel.toggleSearch() }) {
                        Icon(
                            painter = painterResource(
                                if (isSearching) R.drawable.baseline_close_24
                                else R.drawable.baseline_search_24
                            ),
                            contentDescription = if (isSearching) "Aramayı Kapat" else "Ara",
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                content = {
                    Icon(
                        painter = painterResource(R.drawable.baseline_add_24),
                        contentDescription = "Ekle"
                    )
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        if (filesList.isEmpty() && searchKey.isNotBlank() && isSearching) {
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { Text("Arama sonucu bulunamadı.") }
        } else if (filesList.isEmpty() && !isSearching && searchKey.isBlank()) {
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { Text("Gösterilecek dosya yok.") }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(
                    count = filesList.size,
                    key = { index -> filesList[index].id }
                ) { index ->
                    val file = filesList[index]
                    FileCardItem(
                        file = file,
                        navController = navController,
                        onDeleteClicked = {
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "${file.name} silinsin mi?",
                                    actionLabel = "Evet",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    mainViewModel.delete(file.id)

                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FileCardItem(
    file: Files,
    navController: NavController,
    onDeleteClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = 5.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(0.8.dp, Color.LightGray)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { navController.navigate("detailPage/${file.id}/${file.name}") }
                .padding(vertical = 14.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(id = getIconForFileType(file.mimeType, file.name)),
                contentDescription = "Dosya Türü İkonu",
                modifier = Modifier
                    .size(40.dp)
                    .padding(end = 8.dp),
                tint = Color.Unspecified
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = file.name ?: "İsimsiz Dosya",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = (file.mimeType ?: "").uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .background(
                                shape = RoundedCornerShape(percent = 50),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Dosya Boyutu
                    val fileSizeKb = file.sizeInBytes / 1024
                    val fileSizeMb = fileSizeKb / 1024
                    val displaySize = if (fileSizeMb > 0) "$fileSizeMb MB" else "$fileSizeKb KB"
                    Text(
                        text = displaySize,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant // Daha tema uyumlu bir renk
                    )

                    // Ayırıcı
                    Text(
                        text = "  •  ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    // Son Değiştirilme Tarihi (lastModifiedTimeStamp kullanarak)
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val formattedDate = sdf.format(Date(file.lastModifiedTimeStamp))
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }



                if (file.owner.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${file.owner}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Log.e("FileCardItem","Sahip bilgisi boş")
                }
                if (file.author.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${file.author}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline, // Biraz daha soluk bir renk
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Log.e("FileCardItem","Yazar bilgisi boş")
                }
                if (file.path.isNotBlank()) {
                    Log.e("Dosya yolu","${file.path}")
                    val parentDirectoryName = try {
                        val fullFile = File(file.path)
                        fullFile.parentFile?.name
                    } catch (e: Exception) {
                        null
                    }

                    parentDirectoryName?.takeIf { it.isNotBlank() }?.let { dirName ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "/$dirName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            IconButton(onClick = onDeleteClicked) {
                Icon(
                    painter = painterResource(R.drawable.outline_delete_24),
                    contentDescription = "Sil",
                    tint = Color.Unspecified
                )
            }
        }
    }
}


fun getIconForFileType(mimeType: String?, fileName: String?): Int {
    val extension = fileName?.substringAfterLast('.', "")?.lowercase() ?: ""
    return when {
        mimeType?.startsWith("application/pdf", ignoreCase = true) == true || extension == "pdf" -> R.drawable.pdf_icon
        mimeType?.startsWith("application/vnd.ms-excel", ignoreCase = true) == true ||
                mimeType?.startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ignoreCase = true) == true ||
                extension == "xls" || extension == "xlsx" -> R.drawable.xls_icon
        mimeType?.startsWith("application/msword", ignoreCase = true) == true ||
                mimeType?.startsWith("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ignoreCase = true) == true ||
                extension == "doc" || extension == "docx" -> R.drawable.docx
        mimeType?.startsWith("text/", ignoreCase = true) == true || extension == "txt" -> R.drawable.txt_icon
        else -> R.drawable.txt_icon
    }
}

