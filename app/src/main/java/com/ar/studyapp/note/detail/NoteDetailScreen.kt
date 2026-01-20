package com.ar.studyapp.note.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ar.domain.category.model.Category
import com.ar.studyapp.category.menu.CategoryDropdown
import com.ar.studyapp.category.menu.CategoryFabMenu
import com.ar.studyapp.note.list.NoteListViewModel

@Composable
fun NoteDetailRoute(
    noteId: String,
    noteListViewModel: NoteListViewModel,
    categories: List<Category>,
    onBackClick: () -> Unit,
    viewModel: NoteDetailViewModel = hiltViewModel()
) {
    val notes by noteListViewModel.notes.collectAsState()
    val note = notes.firstOrNull { it.id == noteId }

    LaunchedEffect(note) {
        if (note != null) viewModel.onNoteUpdated(note)
    }

    val uiState by viewModel.uiState.collectAsState()

    NoteDetailScreen(
        uiState = uiState,
        categories = categories,
        onBackClick = onBackClick,
        onTitleChange = { viewModel.onEvent(NoteDetailEvent.TitleChanged(it)) },
        onContentChange = { viewModel.onEvent(NoteDetailEvent.ContentChanged(it)) },
        onCategorySelected = { viewModel.onEvent(NoteDetailEvent.CategoryChanged(it)) },
        onSaveClick = { viewModel.save() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    uiState: NoteDetailUiState,
    categories: List<Category>,
    onBackClick: () -> Unit,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onSaveClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {


                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    val canSave =
                        uiState is NoteDetailUiState.Success && uiState.isDirty && !uiState.isSaving

                    IconButton(
                        onClick = onSaveClick,
                        enabled = canSave
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save"
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

//                        Spacer(modifier = Modifier.height(12.dp))
//
//                        CategoryDropdown(
//                            categories = categories,
//                            selectedCategoryId = uiState.selectedCategoryId,
//                            onSelected = onCategorySelected,
//                            modifier = Modifier.fillMaxWidth()
//                        )
//
//                        if (uiState.isSaving) {
//                            Spacer(modifier = Modifier.height(12.dp))
//                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
//                        }

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
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .width(220.dp)
            ) {
                CategoryDropdown(
                    categories = categories,
                    selectedCategoryId = (uiState as? NoteDetailUiState.Success)?.selectedCategoryId,
                    onSelected = onCategorySelected
                )
            }
        }
    }
}
