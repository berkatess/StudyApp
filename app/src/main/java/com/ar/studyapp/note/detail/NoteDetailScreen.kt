package com.ar.studyapp.note.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.ar.studyapp.category.ui.toComposeColorOrFallback
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ar.domain.category.model.Category
import com.ar.studyapp.R
import com.ar.studyapp.note.list.NoteListViewModel
import com.ar.studyapp.note.list.NotesUiState
import com.ar.studyapp.note.navigation.NoteDestinations
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun NoteDetailRoute(
    mode: String?,
    noteId: String?,
    noteListViewModel: NoteListViewModel,
    categories: List<Category>,
    onBackClick: () -> Unit,
    onCreated: (String) -> Unit,
    viewModel: NoteDetailViewModel = hiltViewModel()
) {
    // Keep the original "fill detail from list" approach (SSOT = NoteListViewModel.notes),
    // but make it reactive to avoid race conditions (blank detail on first open).
    val notes by noteListViewModel.notes.collectAsStateWithLifecycle()
    val listUiState by noteListViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(mode, noteId) {
        viewModel.start(mode = mode, noteId = noteId)
    }

    LaunchedEffect(mode, noteId, notes, listUiState) {
        // Create mode: do not wait for list data, draft is already ready.
        if (mode == NoteDestinations.MODE_CREATE) return@LaunchedEffect

        val id = noteId?.trim()
        if (id.isNullOrBlank()) {
            viewModel.showNotFound("null")
            return@LaunchedEffect
        }

        val selected = notes.firstOrNull { it.id == id }

        when {
            selected != null -> viewModel.onNoteUpdated(selected)

            listUiState is NotesUiState.Error -> {
                // If list stream failed, there is no reliable way to resolve the note from SSOT.
                viewModel.showNotFound(id)
            }

            else -> {
                // Still waiting list stream to deliver items.
                viewModel.setLoading()
            }
        }
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NoteDetailScreen(
        uiState = uiState,
        categories = categories,
        onBackClick = onBackClick,
        onTitleChange = { viewModel.onEvent(NoteDetailEvent.TitleChanged(it)) },
        onContentChange = { viewModel.onEvent(NoteDetailEvent.ContentChanged(it)) },
        onCategorySelected = { viewModel.onEvent(NoteDetailEvent.CategoryChanged(it)) },
        onSaveClick = { viewModel.save(onCreated = onCreated) }
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
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
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
                            contentDescription = stringResource(R.string.common_save)
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
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = noteDateTimeText(
                                    updatedAt = uiState.note.updatedAt,
                                    createdAt = uiState.note.createdAt
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            NoteCategorySelector(
                                categories = categories,
                                selectedCategoryId = uiState.selectedCategoryId,
                                onCategorySelected = onCategorySelected
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        BasicTextField(
                            value = uiState.titleDraft,
                            onValueChange = onTitleChange,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.headlineSmall,
                            decorationBox = { innerTextField ->
                                Box {
                                    if (uiState.titleDraft.isBlank()) {
                                        Text(
                                            text = stringResource(R.string.note_detail_title_placeholder),
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        BasicTextField(
                            value = uiState.contentDraft,
                            onValueChange = onContentChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 150.dp),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            decorationBox = { innerTextField ->
                                Box {
                                    if (uiState.contentDraft.isBlank()) {
                                        Text(
                                            text = stringResource(R.string.note_detail_content_placeholder),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

//                        if (uiState.isSaving) {
//                            Spacer(modifier = Modifier.height(12.dp))
//                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
//                        }

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

@Composable
private fun NoteCategorySelector(
    categories: List<Category>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCategory = remember(categories, selectedCategoryId) {
        categories.firstOrNull { it.id == selectedCategoryId }
    }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Show selected category color indicator in collapsed state (like category menus)
            selectedCategory?.let { category ->
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            // If you created the shared extension:
                            category.toComposeColorOrFallback(MaterialTheme.colorScheme.outlineVariant)

                            // If you haven't created the shared extension yet, use your local helper:
                            // category.toComposeColorOrFallback()
                        )
                )
            }

            Text(
                text = selectedCategory?.name ?: stringResource(R.string.note_detail_category_label),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(R.string.note_detail_select_category_cd),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.category_none)) },
                onClick = {
                    onCategorySelected(null)
                    expanded = false
                }
            )

            categories.forEach { category ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Color indicator for category (same UX pattern as category list/menu)
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(category.toComposeColorOrFallback(MaterialTheme.colorScheme.outlineVariant))
                            )

                            Text(text = category.name)
                        }
                    },
                    onClick = {
                        onCategorySelected(category.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun noteDateTimeText(
    updatedAt: Instant,
    createdAt: Instant
): String {
    val locale = Locale.getDefault()
    val zoneId = ZoneId.systemDefault()
    val referenceInstant = if (updatedAt.isAfter(createdAt)) updatedAt else createdAt
    val referenceDateTime = referenceInstant.atZone(zoneId)
    val nowDate = Instant.now().atZone(zoneId).toLocalDate()
    val noteDate = referenceDateTime.toLocalDate()

    val timeText = remember(locale) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
    }.format(referenceDateTime)

    return if (noteDate == nowDate) {
        "${stringResource(R.string.note_detail_today)}, $timeText"
    } else if (noteDate == nowDate.minus(1, ChronoUnit.DAYS)) {
        "${stringResource(R.string.note_detail_yesterday)}, $timeText"
    } else {
        val dateText = remember(locale) {
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
        }.format(referenceDateTime)
        "$dateText, $timeText"
    }
}