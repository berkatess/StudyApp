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

/**
 * UI model for the list screen.
 * We keep it screen-focused instead of using the domain model directly.
 */
data class NoteListItemUiModel(
    val id: String,
    val title: String,
    val contentPreview: String,
    val categoryName: String?,
    val categoryColorHex: String?
)

/**
 * List screen UI state.
 */
sealed class NotesUiState {
    object Loading : NotesUiState()
    data class Success(val notes: List<NoteListItemUiModel>) : NotesUiState()
    data class Error(val message: String) : NotesUiState()
}

@HiltViewModel
class NoteListViewModel @Inject constructor(
    private val getNotesUseCase: GetNotesUseCase,
    private val observeCategoriesUseCase: ObserveCategoriesUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val noteRepository: NoteRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotesUiState>(NotesUiState.Loading)
    val uiState: StateFlow<NotesUiState> = _uiState

    /**
     * Optional user preference.
     * You can keep this always FAST and never expose it to UI if you want.
     */
    private val userPreferredStrategy = MutableStateFlow(FetchStrategy.FAST)

    /**
     * Online state stream provided by NetworkMonitor.
     */
    private val isOnline: StateFlow<Boolean> =
        networkMonitor.isOnline
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    /**
     * Effective strategy used for *observing* data:
     * - Offline => force CACHED to avoid remote failures.
     * - Online  => use user's preference (typically FAST).
     *
     * IMPORTANT: We never put SYNCED here. SYNCED is only used as a one-shot refresh trigger.
     */
    private val effectiveStrategy: StateFlow<FetchStrategy> =
        combine(userPreferredStrategy, isOnline) { preferred, online ->
            if (!online) FetchStrategy.CACHED else preferred
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FetchStrategy.FAST)

    init {
        observeNotesAndCategories()

        // First install / new device scenario:
        // If local DB is empty and we have internet, trigger a one-shot sync to populate local cache.
        viewModelScope.launch {
            val onlineNow = networkMonitor.isOnlineNow()
            if (onlineNow && !noteRepository.hasAnyNotesLocally()) {
                getNotesUseCase(FetchStrategy.SYNCED).first()
            }
        }

        // If the app starts offline and later becomes online, and local is still empty,
        // trigger a one-shot sync again.
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

    /**
     * Called when the home screen becomes visible.
     * We use SYNCED as an imperative refresh trigger (remote one-shot -> update local cache).
     * UI updates automatically because the list observes the local database.
     */
    fun onHomeVisible() {
        viewModelScope.launch {
            if (networkMonitor.isOnlineNow()) {
                getNotesUseCase(FetchStrategy.SYNCED).first()
            }
        }
    }

    /**
     * Observes notes and categories together.
     *
     * Notes:
     * - getNotesUseCase(strategy) -> Flow<Result<List<Note>>>
     *
     * Categories (we added strategy support earlier):
     * - observeCategoriesUseCase(strategy) -> Flow<Result<List<Category>>>
     *
     * We bind both streams to the same effectiveStrategy using flatMapLatest,
     * so switching between FAST/CACHED automatically re-subscribes.
     */
    private fun observeNotesAndCategories() {
        val notesFlow = effectiveStrategy.flatMapLatest { strategy ->
            getNotesUseCase(strategy)
        }

        val categoriesFlow = effectiveStrategy.flatMapLatest { strategy ->
            observeCategoriesUseCase(strategy)
        }

        viewModelScope.launch {
            combine(notesFlow, categoriesFlow) { notesResult, categoriesResult ->
                notesResult to categoriesResult
            }.collectLatest { (notesResult, categoriesResult) ->

                // If any stream is loading, show loading.
                if (notesResult is Result.Loading || categoriesResult is Result.Loading) {
                    _uiState.value = NotesUiState.Loading
                    return@collectLatest
                }

                // Error handling.
                if (notesResult is Result.Error) {
                    _uiState.value = NotesUiState.Error(
                        notesResult.message ?: "Failed to load notes"
                    )
                    return@collectLatest
                }

                if (categoriesResult is Result.Error) {
                    _uiState.value = NotesUiState.Error(
                        categoriesResult.message ?: "Failed to load categories"
                    )
                    return@collectLatest
                }

                // Success: build UI models.
                if (notesResult is Result.Success && categoriesResult is Result.Success) {
                    val notes: List<Note> = notesResult.data
                    val categories: List<Category> = categoriesResult.data

                    val categoriesById = categories.associateBy { it.id }

                    val uiItems = notes.map { note ->
                        val category = note.categoryId?.let { categoriesById[it] }
                        NoteListItemUiModel(
                            id = note.id,
                            title = note.title,
                            contentPreview = note.content.take(80),
                            categoryName = category?.name,
                            categoryColorHex = category?.colorHex
                        )
                    }

                    _uiState.value = NotesUiState.Success(uiItems)
                }
            }
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            deleteNoteUseCase(noteId)
            // No explicit refresh is needed here:
            // - The UI observes local DB
            // - Local deletion updates UI immediately
            // - Remote sync can happen via your existing sync mechanism
        }
    }

    /**
     * Optional manual pull-to-refresh hook (if you want it later).
     */
    fun refresh() {
        viewModelScope.launch {
            if (networkMonitor.isOnlineNow()) {
                getNotesUseCase(FetchStrategy.SYNCED).first()
            }
        }
    }
}
