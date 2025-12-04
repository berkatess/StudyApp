package com.ar.studyapp.note

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ar.domain.note.model.Note

@Composable
fun NotesScreen(
    viewModel: NoteListViewModel
) {
//    val state = viewModel.uiState

    val state by viewModel.uiState.collectAsState()

    when (state) {
        is NotesUiState.Loading -> LoadingState()
        is NotesUiState.Error -> ErrorState((state as NotesUiState.Error).message)
        is NotesUiState.Success -> NotesList((state as NotesUiState.Success).notes)
    }

//    when (state) {
//        is NotesUiState.Loading -> LoadingState()
//        is NotesUiState.Error -> ErrorState(state.message)
//        is NotesUiState.Success -> NotesList(state.notes)
//    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Error: $message",
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun NotesList(notes: List<Note>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        items(notes) { note ->
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
