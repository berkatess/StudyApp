package com.ar.studyapp.note.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    onAddNoteClick: () -> Unit,
    onManageCategoriesClick: () -> Unit,
    viewModel: NoteListViewModel = hiltViewModel()
) {
    // ViewModel'deki StateFlow'u Compose state'e çeviriyoruz
    val uiState by viewModel.uiState.collectAsState()

    NoteListScreen(
        uiState = uiState,
        onNoteClick = onNoteClick,
        onAddNoteClick = onAddNoteClick,
        onManageCategoriesClick = onManageCategoriesClick
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
    onAddNoteClick: () -> Unit,
    onManageCategoriesClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notes") },
                actions = {
                    IconButton(onClick = onManageCategoriesClick) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Manage categories"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNoteClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add note"
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState) {
                is NotesUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is NotesUiState.Error -> {
                    Text(
                        text = uiState.message,
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                is NotesUiState.Success -> {
                    if (uiState.notes.isEmpty()) {
                        Text(
                            text = "No notes yet",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = uiState.notes,
                                key = { it.id }
                            ) { item ->
                                NoteListItem(
                                    item = item,
                                    onClick = { onNoteClick(item.id) }
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
 * Tek bir not item'i.
 * Arka plan rengi, note'un kategorisinin rengiyle aynı.
 */
@Composable
fun NoteListItem(
    item: NoteListItemUiModel,
    onClick: () -> Unit
) {
    // Kategori rengi varsa onu kullan, yoksa default surface
    val backgroundColor: Color = item.categoryColorHex?.let { hex ->
        try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (e: Exception) {
            MaterialTheme.colorScheme.surface
        }
    } ?: MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface // kategori rengi koyu ise override etmeyi düşünebiliriz
            )

            if (!item.categoryName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.categoryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }

            if (item.contentPreview.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.contentPreview,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}