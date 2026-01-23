package com.ar.studyapp.note.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ar.core.data.FetchStrategy
import com.ar.core.network.NetworkMonitor
import com.ar.core.result.Result
import com.ar.domain.category.model.Category
import com.ar.domain.category.usecase.ObserveCategoriesUseCase
import com.ar.domain.note.model.Note
import com.ar.domain.note.repository.NoteRepository
import com.ar.domain.note.usecase.DeleteNoteUseCase
import com.ar.domain.note.usecase.GetNotesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NoteListItemUiModel(
    val id: String,
    val title: String,
    val contentPreview: String,
    val categoryName: String?,
    val categoryColorHex: String?
)

sealed interface NotesUiState {
    object Loading : NotesUiState
    data class Success(
        val notes: List<NoteListItemUiModel>,
        val categories: List<Category>,
        val selectedCategoryId: String?,
        val searchQuery: String
    ) : NotesUiState
    data class Error(val message: String) : NotesUiState
}

private data class CombinedState(
    val notesResult: Result<List<Note>>,
    val categoriesResult: Result<List<Category>>,
    val selectedCategoryId: String?,
    val query: String
)

@HiltViewModel
class NoteListViewModel @Inject constructor(
    private val getNotesUseCase: GetNotesUseCase,
    private val observeCategoriesUseCase: ObserveCategoriesUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val noteRepository: NoteRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    /**
     * SSOT output for other screens (e.g., Detail).
     *
     * Home already owns the source-of-truth stream.
     * We simply expose the full Note list here so Detail can select a single note by id
     * without creating another data source (no extra remote/db fetch in detail).
     *
     * This does NOT change Home behavior.
     */
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _uiState = MutableStateFlow<NotesUiState>(NotesUiState.Loading)
    val uiState: StateFlow<NotesUiState> = _uiState

    private val userPreferredStrategy = MutableStateFlow(FetchStrategy.FAST)

    // Holds the search query for notes list
    private val searchQuery = MutableStateFlow("")

    // Called when user types into search field
    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    // Called when user closes search
    fun clearSearch() {
        searchQuery.value = ""
    }

    // Holds currently selected category id
    private val selectedCategoryId = MutableStateFlow<String?>(null)

    // Called when a category chip is selected from UI
    fun onCategorySelected(categoryId: String?) {
        val current = selectedCategoryId.value
        selectedCategoryId.value = if (current == categoryId) null else categoryId
    }

    private val isOnline: StateFlow<Boolean> =
        networkMonitor.isOnline
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val effectiveStrategy: StateFlow<FetchStrategy> =
        combine(userPreferredStrategy, isOnline) { preferred, online ->
            if (!online) FetchStrategy.CACHED else preferred
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FetchStrategy.FAST)

    init {
        observeNotesAndCategories()

        // Initial sync when app is online and local storage is empty.
        viewModelScope.launch {
            val onlineNow = networkMonitor.isOnlineNow()
            if (onlineNow && !noteRepository.hasAnyNotesLocally()) {
                getNotesUseCase(FetchStrategy.SYNCED).first()
            }
        }

        // When coming back online, try to sync if local storage is empty.
        viewModelScope.launch {
            networkMonitor.isOnline
                .filter { it }
                .collect {
                    if (!noteRepository.hasAnyNotesLocally()) {
                        getNotesUseCase(FetchStrategy.SYNCED).first()
                    }
                }
        }
    }

    fun onHomeVisible() {
        viewModelScope.launch {
            if (networkMonitor.isOnlineNow()) {
                getNotesUseCase(FetchStrategy.SYNCED).first()
            }
        }
    }

    private fun observeNotesAndCategories() {
        val notesFlow = effectiveStrategy.flatMapLatest { strategy ->
            getNotesUseCase(strategy)
        }

        val categoriesFlow = effectiveStrategy.flatMapLatest { strategy ->
            observeCategoriesUseCase(strategy)
        }

        viewModelScope.launch {
            // Combine all UI-affecting inputs in one place to keep SSOT behavior.
            combine(
                notesFlow,
                categoriesFlow,
                selectedCategoryId,
                searchQuery
            ) { notesResult, categoriesResult, selectedCategoryId, query ->
                CombinedState(
                    notesResult = notesResult,
                    categoriesResult = categoriesResult,
                    selectedCategoryId = selectedCategoryId,
                    query = query
                )
            }.collectLatest { state ->

                if (state.notesResult is Result.Loading || state.categoriesResult is Result.Loading) {
                    _uiState.value = NotesUiState.Loading
                    return@collectLatest
                }

                if (state.notesResult is Result.Error) {
                    _uiState.value = NotesUiState.Error(
                        state.notesResult.message ?: "Failed to load notes"
                    )
                    return@collectLatest
                }

                if (state.categoriesResult is Result.Error) {
                    _uiState.value = NotesUiState.Error(
                        state.categoriesResult.message ?: "Failed to load categories"
                    )
                    return@collectLatest
                }

                if (state.notesResult is Result.Success && state.categoriesResult is Result.Success) {
                    val notes: List<Note> = state.notesResult.data
                    val categories: List<Category> = state.categoriesResult.data
                    val categoriesById = categories.associateBy { it.id }

                    // Update SSOT list for consumers like Detail.
                    _notes.value = notes
                    _categories.value = categories

                    // 1) Category filtering
                    val categoryFiltered = if (state.selectedCategoryId == null) {
                        notes
                    } else {
                        notes.filter { it.categoryId == state.selectedCategoryId }
                    }

                    // 2) Query filtering (title + content)
                    // Note: Filtering in ViewModel keeps UI dumb and testable.
                    val q = state.query.trim()
                    val fullyFiltered = if (q.isBlank()) {
                        categoryFiltered
                    } else {
                        val needle = q.lowercase()
                        categoryFiltered.filter { note ->
                            note.title.lowercase().contains(needle) ||
                                    note.content.lowercase().contains(needle)
                        }
                    }

                    val uiItems = fullyFiltered.map { note ->
                        val category = note.categoryId?.let { categoriesById[it] }
                        NoteListItemUiModel(
                            id = note.id,
                            title = note.title,
                            contentPreview = note.content.take(80),
                            categoryName = category?.name,
                            categoryColorHex = category?.colorHex
                        )
                    }

                    _uiState.value = NotesUiState.Success(
                        notes = uiItems,
                        categories = categories,
                        selectedCategoryId = state.selectedCategoryId,
                        searchQuery = state.query
                    )
                }
            }
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            deleteNoteUseCase(noteId)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            if (networkMonitor.isOnlineNow()) {
                getNotesUseCase(FetchStrategy.SYNCED).first()
            }
        }
    }
}
