package com.ar.studyapp.note.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ar.domain.note.model.Note

/**
 * Route layer:
 * - ViewModel'i alır (Hilt ile)
 * - Flow'u collect eder
 * - Saf UI olan NoteListScreen'e parametreleri geçirir
 */
@Composable
fun NoteListRoute(
    onNoteClick: (String) -> Unit,
    viewModel: NoteListViewModel = hiltViewModel()
) {
    // ViewModel'deki StateFlow'u Compose state'e çeviriyoruz
    val uiState by viewModel.uiState.collectAsState()

    NoteListScreen(
        uiState = uiState,
        onNoteClick = onNoteClick,
        onRetryClick = {
            // Şimdilik tekrar observe edebilirsin veya ileride explicit refresh ekleriz
            // viewModel.refresh() gibi bir fonksiyon yazılabilir.
        }
    )
}

/**
 * Saf UI katmanı:
 * - Gelen uiState'e göre ekranda ne gösterileceğine karar verir.
 * - ViewModel bilmez, sadece state + callback alır.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    uiState: NotesUiState,
    onNoteClick: (String) -> Unit,
    onRetryClick: () -> Unit
) {
    Scaffold(
        topBar = {
            // Üst bar – sadece başlık
            TopAppBar(
                title = { Text("Notes") }
            )
        }
    ) { innerPadding ->
        // Scaffold'un iç padding'ini Box'a aktarıyoruz
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState) {
                is NotesUiState.Loading -> {
                    // Yükleme durumunda ortada bir progress göster
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is NotesUiState.Error -> {
                    // Hata durumunda mesaj + retry butonu
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onRetryClick) {
                            Text("Retry")
                        }
                    }
                }

                is NotesUiState.Success -> {
                    if (uiState.notes.isEmpty()) {
                        // Veri var ama liste boşsa
                        Text(
                            text = "No notes yet",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        // Notları listele
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = uiState.notes,
                                key = { it.id } // stable key, performans için
                            ) { note ->
                                NoteListItem(
                                    note = note,
                                    onClick = { onNoteClick(note.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tek bir note satırını temsil eden composable.
 * - Card içinde başlık + içeriğin kısaltılmış hali gösteriliyor.
 * - Tıklanınca onClick tetikleniyor (navigation üst seviyede yapılıyor).
 */
@Composable
fun NoteListItem(
    note: Note,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Başlık kısmı
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))

            // İçerikten kısa bir preview (max 2 satır)
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
