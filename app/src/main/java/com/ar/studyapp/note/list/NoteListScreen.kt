package com.ar.studyapp.note.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ar.studyapp.anim.SwipeRevealItem
import com.ar.studyapp.category.component.CategoryBanner
import com.ar.studyapp.dialog.ConfirmDeleteDialog

/**
 * Route layer:
 * - Obtains the ViewModel (via Hilt)
 * - Collects the state Flow
 * - Passes data and callbacks to the pure UI NoteListScreen
 */
@Composable
fun NoteListRoute(
    onNoteClick: (String) -> Unit,
    onAddNoteClick: () -> Unit,
    onManageCategoriesClick: () -> Unit,
    viewModel: NoteListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onHomeVisible()
    }

    NoteListScreen(
        uiState = uiState,
        onNoteClick = onNoteClick,
        onAddNoteClick = onAddNoteClick,
        onManageCategoriesClick = onManageCategoriesClick,
        onDeleteNote = viewModel::deleteNote,
        onCategorySelected = viewModel::onCategorySelected,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onClearSearch = viewModel::clearSearch
    )
}


/**
 * Pure UI layer:
 * - Decides what to render based on the given uiState
 * - Does not know about the ViewModel, only receives state and callbacks
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    uiState: NotesUiState,
    onNoteClick: (String) -> Unit,
    onAddNoteClick: () -> Unit,
    onManageCategoriesClick: () -> Unit,
    onDeleteNote: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit
) {
    // UI-only state: controls whether search input is visible in the TopAppBar.
    var searchActive by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Read query from uiState (SSOT lives in ViewModel).
    val query = (uiState as? NotesUiState.Success)?.searchQuery.orEmpty()

    // Keep the search UI open if there is already a query (e.g., after rotation).
    LaunchedEffect(query) {
        if (query.isNotBlank()) {
            searchActive = true
        }
    }

    // Request focus automatically when search becomes active (brings up the keyboard).
    LaunchedEffect(searchActive) {
        if (searchActive) {
            focusRequester.requestFocus()
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (searchActive) {
                        IconButton(
                            onClick = {
                                // Close search, clear focus and reset query.
                                searchActive = false
                                focusManager.clearFocus(force = true)
                                onClearSearch()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Close search"
                            )
                        }
                    }
                },
                title = {
                    if (searchActive) {
                        TextField(
                            value = query,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(end = 16.dp)
                                .focusRequester(focusRequester),
                                singleLine = true,
                            placeholder = { Text("Search notes") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = { focusManager.clearFocus() }
                            ),
                            trailingIcon = {
                                if (query.isNotBlank()) {
                                    IconButton(
                                        onClick = onClearSearch
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear search"
                                        )
                                    }
                                }
                            },
                            // Make it feel like an inline search field (no filled box background).
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent
                            )
                        )
                    } else {
                        Text("Notes")
                    }
                },
                actions = {
                    if (!searchActive) {
                        IconButton(onClick = { searchActive = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }

                        IconButton(onClick = onManageCategoriesClick) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Manage categories"
                            )
                        }
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

                    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // NOTE: The banner must always be visible even if filtered list is empty.
                        item {
                            CategoryBanner(
                                categories = uiState.categories,
                                selectedCategoryId = uiState.selectedCategoryId,
                                onCategorySelected = onCategorySelected
                            )
                        }

                        if (uiState.notes.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "No notes yet")
                                }
                            }
                        } else {
                            items(
                                items = uiState.notes,
                                key = { it.id }
                            ) { item ->
                                SwipeRevealItem(
                                    onDeleteClick = { pendingDeleteId = item.id }
                                ) { shape ->
                                    NoteListItem(
                                        item = item,
                                        onClick = { onNoteClick(item.id) },
                                        shape = shape
                                    )
                                }
                            }
                        }
                    }

                    if (pendingDeleteId != null) {
                        ConfirmDeleteDialog(
                            onConfirm = {
                                onDeleteNote(pendingDeleteId!!)
                                pendingDeleteId = null
                            },
                            onDismiss = { pendingDeleteId = null }
                        )
                    }
                }
            }
        }
    }
}


/**
 * A single note list item.
 * The background color matches the note's category color.
 */
@Composable
fun NoteListItem(
    item: NoteListItemUiModel,
    onClick: () -> Unit,
    shape: Shape
) {
    // Use category color if available, otherwise fall back to the default surface color
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
        shape = shape,
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
                color = MaterialTheme.colorScheme.onSurface
                // If the category color is too dark, this could be overridden
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
