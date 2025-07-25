package com.ilhanaltunbas.filemanager.uix.view

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ilhanaltunbas.filemanager.R
import com.ilhanaltunbas.filemanager.uix.SearchType
import com.ilhanaltunbas.filemanager.uix.viewmodel.DetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailPage(navController: NavController, receivedId: Int?, receivedName: String?,detailViewModel: DetailViewModel) {
    val isSearching = remember { mutableStateOf(false) }
    val searchingKey = remember { mutableStateOf("") }

    var searchTypeMenuExpanded by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current // Klavye kontrolü için


    val searchResult by detailViewModel.searchResults.collectAsState()
    val content by detailViewModel.fileContent.observeAsState()

    LaunchedEffect(key1 = receivedId) {
        Log.d("DetailPage", "LaunchedEffect triggered. receivedId: $receivedId")
        if (receivedId != null) {
            Log.d("DetailPage", "Calling loadFileContentById with ID: $receivedId")
            isSearching.value = false
            searchingKey.value = ""
            detailViewModel.loadFileContentById(receivedId)
        } else {
            Log.e("DetailPage", "receivedId is null. Cannot load file content.")
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching.value) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextField(
                                value = searchingKey.value,
                                onValueChange = { newValue ->
                                    searchingKey.value = newValue
                                    Log.d("DetailPage_TF", "onValueChange: '$newValue'")
                                    detailViewModel.searchInCurrentFileContent(newValue)
                                },
                                placeholder = { Text(text = "Search") },
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
                                    unfocusedIndicatorColor = Color.Transparent,
                                ),
                                textStyle = MaterialTheme.typography.bodyMedium,
                            )
                            Box(modifier = Modifier.padding(start = 4.dp)) {
                                IconButton(onClick = {
                                    Log.d("DetailPage_DM", "Filter icon clicked. Expanding menu.")
                                    searchTypeMenuExpanded = true
                                }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.outline_more_vert_24),
                                        contentDescription = "Arama Türünü Seç"
                                    )
                                }
                                DropdownMenu(
                                    expanded = searchTypeMenuExpanded,
                                    onDismissRequest = {
                                        searchTypeMenuExpanded = false
                                    }
                                ) {
                                    SearchType.values().forEach { type ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(text = when (type) {
                                                    SearchType.CONTAINS -> "İçeren"
                                                    SearchType.STARTS_WITH -> "Başlayan"
                                                    SearchType.ENDS_WITH -> "Biten"
                                                })
                                            },
                                            onClick = {
                                                Log.d("DetailPage_DM_Item", "SearchType selected: $type. Current searchKey: '${searchingKey.value}'")
                                                detailViewModel.updateSearchType(type)
                                                searchTypeMenuExpanded = false
                                                if (searchingKey.value.isNotBlank()) {
                                                    detailViewModel.searchInCurrentFileContent(searchingKey.value)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        if (receivedName != null){
                            Text(
                                text = "$receivedName"

                            )
                        } else {
                            Text(
                                text = "Dosya içeriği yükleniyor..."
                            )

                        }
                    }
                },

                navigationIcon = {
                    if (!isSearching.value) { // Sadece arama modunda değilken geri ikonunu göster
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Geri"
                            )
                        }
                    }
                },
                actions = {

                    IconButton(
                        onClick = {
                            isSearching.value = !isSearching.value
                            if (!isSearching.value) { // Arama kapatılıyorsa
                                searchingKey.value = ""
                                detailViewModel.searchInCurrentFileContent("")
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isSearching.value) R.drawable.baseline_close_24
                                else R.drawable.baseline_search_24
                            ),
                            contentDescription = if (isSearching.value) "Aramayı Kapat" else "Ara",
                        )
                    }
                }
            )
        }
    ){
        paddingValues ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (content != null || searchResult.isNotEmpty()) {
                val scrollState = rememberScrollState()

                SelectionContainer {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(16.dp)
                    ) {

                        Box(
                            // ...
                        ) {
                            Column {

                                if (isSearching.value && searchingKey.value.isNotBlank()) {
                                    if (searchResult.isNotEmpty()) {
                                        searchResult.forEachIndexed { index, resultLine ->
                                            Text(
                                                text = resultLine,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 14.sp,
                                                lineHeight = 20.sp,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            if (index < searchResult.size - 1) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                            }
                                        }
                                    } else {

                                        Text(
                                            text = "Aradığınız kriterlere uygun sonuç bulunamadı.",
                                            modifier = Modifier
                                                .align(Alignment.CenterHorizontally)
                                                .padding(16.dp) // Ortalamak için
                                        )

                                    }
                                } else {

                                    content?.let {
                                        Text(
                                            text = it,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                }
                            }
                        }
                    }
                }

            } else if (content == null && searchResult.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Dosya içeriği yükleniyor...")
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Gösterilecek içerik bulunamadı.")
                }
            }
        }
    }
}
