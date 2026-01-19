package com.ar.studyapp.note.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun NoteDetailRoute(
    noteId: String,
    onBackClick: () -> Unit,
    viewModel: NoteDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(noteId) {
        viewModel.loadNote(noteId)
    }

    val uiState by viewModel.uiState.collectAsState()

    NoteDetailScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onTitleChange = { viewModel.onEvent(NoteDetailEvent.TitleChanged(it)) },
        onContentChange = { viewModel.onEvent(NoteDetailEvent.ContentChanged(it)) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    uiState: NoteDetailUiState,
    onBackClick: () -> Unit,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (uiState) {
                            is NoteDetailUiState.Success -> uiState.titleDraft
                            else -> "Note Detail"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState) {
                is NoteDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is NoteDetailUiState.Error -> {
                    Text(
                        text = uiState.message,
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                is NoteDetailUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {

                        BasicTextField(
                            value = uiState.titleDraft,
                            onValueChange = onTitleChange,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.headlineSmall
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        BasicTextField(
                            value = uiState.contentDraft,
                            onValueChange = onContentChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 150.dp),
                            textStyle = MaterialTheme.typography.bodyLarge
                        )

                        if (uiState.saveError != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = uiState.saveError,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
