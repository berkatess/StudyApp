package com.ar.studyapp.note.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ar.core.result.Result
import com.ar.domain.note.model.Note
import com.ar.domain.note.usecase.CreateNoteUseCase
import com.ar.domain.note.usecase.UpdateNoteUseCase
import com.ar.studyapp.note.navigation.NoteDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface NoteDetailUiState {
    object Loading : NoteDetailUiState
    data class Error(val message: String) : NoteDetailUiState

    data class Success(
        val note: Note,
        val isNew: Boolean,
        val titleDraft: String,
        val contentDraft: String,
        val selectedCategoryId: String? = null,
        val isSaving: Boolean = false,
        val saveError: String? = null,
        val isDirty: Boolean = false,
        val canSave: Boolean = false
    ) : NoteDetailUiState
}

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    private val createNoteUseCase: CreateNoteUseCase,
    private val updateNoteUseCase: UpdateNoteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<NoteDetailUiState>(NoteDetailUiState.Loading)
    val uiState: StateFlow<NoteDetailUiState> = _uiState

    private var currentKey: String? = null

    /**
     * Initializes the screen based on navigation args.
     *
     * Best practice:
     * - "Create" is explicit via mode flag (no sentinel ids).
     * - "Edit" waits for the SSOT (NoteListViewModel.notes) and gets injected via onNoteUpdated(note).
     */
    fun start(mode: String?, noteId: String?) {
        val normalizedMode = mode ?: NoteDestinations.MODE_EDIT
        val normalizedId = noteId?.trim()

        val key = "${normalizedMode}_${normalizedId.orEmpty()}"
        if (currentKey == key) return
        currentKey = key

        if (normalizedMode == NoteDestinations.MODE_CREATE) {
            startNewDraft()
        } else {
            // Edit mode -> we wait for SSOT injection via onNoteUpdated
            setLoading()
        }
    }

    /**
     * Puts the UI into loading state while waiting SSOT (list stream).
     * We avoid overriding user edits if the user is already editing (dirty=true).
     */
    fun setLoading() {
        val current = _uiState.value
        if (current is NoteDetailUiState.Success && current.isDirty) return
        _uiState.value = NoteDetailUiState.Loading
    }

    /**
     * Shows a "not found" state if the note cannot be resolved from the SSOT.
     */
    fun showNotFound(noteId: String) {
        val current = _uiState.value
        if (current is NoteDetailUiState.Success && current.isDirty) return
        _uiState.value = NoteDetailUiState.Error("Note not found: $noteId")
    }

    /**
     * SSOT injection point.
     * Route calls this when the note appears in NoteListViewModel.notes.
     *
     * Rule:
     * - If user is editing (dirty), do NOT overwrite drafts.
     * - Otherwise, sync drafts with the latest note.
     */
    fun onNoteUpdated(note: Note) {
        val current = _uiState.value

        if (current !is NoteDetailUiState.Success) {
            _uiState.value = NoteDetailUiState.Success(
                note = note,
                isNew = false,
                titleDraft = note.title,
                contentDraft = note.content,
                selectedCategoryId = note.categoryId,
                isDirty = false,
                canSave = false
            )
            return
        }

        if (current.isDirty) {
            // Keep drafts, only refresh backing note reference.
            _uiState.value = current.copy(note = note, saveError = null)
            return
        }

        // Not dirty -> keep UI fully in sync with SSOT.
        _uiState.value = current.copy(
            note = note,
            titleDraft = note.title,
            contentDraft = note.content,
            selectedCategoryId = note.categoryId,
            saveError = null,
            canSave = false
        )
    }

    private fun startNewDraft() {
        val now = Instant.now()
        val empty = Note(
            id = "",
            title = "",
            content = "",
            categoryId = null,
            createdAt = now,
            updatedAt = now
        )
        _uiState.value = NoteDetailUiState.Success(
            note = empty,
            isNew = true,
            titleDraft = "",
            contentDraft = "",
            selectedCategoryId = null,
            isDirty = false,
            canSave = false
        )
    }

    fun onEvent(event: NoteDetailEvent) {
        val current = _uiState.value as? NoteDetailUiState.Success ?: return
        when (event) {
            is NoteDetailEvent.TitleChanged -> onTitleChanged(current, event.value)
            is NoteDetailEvent.ContentChanged -> onContentChanged(current, event.value)
            is NoteDetailEvent.CategoryChanged -> onCategoryChanged(current, event.categoryId)
        }
    }

    private fun onTitleChanged(current: NoteDetailUiState.Success, newTitle: String) {
        val dirty = isDirty(
            note = current.note,
            isNew = current.isNew,
            titleDraft = newTitle,
            contentDraft = current.contentDraft,
            selectedCategoryId = current.selectedCategoryId
        )
        _uiState.value = current.copy(
            titleDraft = newTitle,
            isDirty = dirty,
            canSave = canSave(
                isNew = current.isNew,
                isDirty = dirty,
                isSaving = current.isSaving,
                titleDraft = newTitle,
                contentDraft = current.contentDraft
            ),
            saveError = null
        )
    }

    private fun onContentChanged(current: NoteDetailUiState.Success, newContent: String) {
        val dirty = isDirty(
            note = current.note,
            isNew = current.isNew,
            titleDraft = current.titleDraft,
            contentDraft = newContent,
            selectedCategoryId = current.selectedCategoryId
        )
        _uiState.value = current.copy(
            contentDraft = newContent,
            isDirty = dirty,
            canSave = canSave(
                isNew = current.isNew,
                isDirty = dirty,
                isSaving = current.isSaving,
                titleDraft = current.titleDraft,
                contentDraft = newContent
            ),
            saveError = null
        )
    }

    private fun onCategoryChanged(current: NoteDetailUiState.Success, categoryId: String?) {
        val dirty = isDirty(
            note = current.note,
            isNew = current.isNew,
            titleDraft = current.titleDraft,
            contentDraft = current.contentDraft,
            selectedCategoryId = categoryId
        )
        _uiState.value = current.copy(
            selectedCategoryId = categoryId,
            isDirty = dirty,
            canSave = canSave(
                isNew = current.isNew,
                isDirty = dirty,
                isSaving = current.isSaving,
                titleDraft = current.titleDraft,
                contentDraft = current.contentDraft
            ),
            saveError = null
        )
    }

    fun save(onCreated: (String) -> Unit) {
        val current = _uiState.value as? NoteDetailUiState.Success ?: return
        if (!current.canSave || current.isSaving) return

        viewModelScope.launch {
            _uiState.value = current.copy(isSaving = true, saveError = null)

            val now = Instant.now()

            val result = if (current.isNew) {
                val created = Note(
                    id = "",
                    title = current.titleDraft.trim(),
                    content = current.contentDraft.trim(),
                    categoryId = current.selectedCategoryId,
                    createdAt = now,
                    updatedAt = now
                )
                createNoteUseCase(created)
            } else {
                val updated = current.note.copy(
                    title = current.titleDraft.trim(),
                    content = current.contentDraft.trim(),
                    categoryId = current.selectedCategoryId,
                    updatedAt = now
                )
                updateNoteUseCase(updated)
            }

            when (result) {
                is Result.Success -> {
                    val saved = result.data
                    _uiState.value = current.copy(
                        note = saved,
                        isNew = false,
                        titleDraft = saved.title,
                        contentDraft = saved.content,
                        selectedCategoryId = saved.categoryId,
                        isSaving = false,
                        isDirty = false,
                        canSave = false,
                        saveError = null
                    )

                    // Notify caller to switch route into edit mode with real id.
                    if (current.isNew) onCreated(saved.id)
                }

                is Result.Error -> {
                    _uiState.value = current.copy(
                        isSaving = false,
                        saveError = result.message ?: "Failed to save note",
                        canSave = canSave(
                            isNew = current.isNew,
                            isDirty = current.isDirty,
                            isSaving = false,
                            titleDraft = current.titleDraft,
                            contentDraft = current.contentDraft
                        )
                    )
                }

                is Result.Loading -> Unit
            }
        }
    }

    private fun isDirty(
        note: Note,
        isNew: Boolean,
        titleDraft: String,
        contentDraft: String,
        selectedCategoryId: String?
    ): Boolean {
        return if (isNew) {
            titleDraft.isNotBlank() || contentDraft.isNotBlank() || selectedCategoryId != null
        } else {
            titleDraft != note.title ||
                    contentDraft != note.content ||
                    selectedCategoryId != note.categoryId
        }
    }

    private fun canSave(
        isNew: Boolean,
        isDirty: Boolean,
        isSaving: Boolean,
        titleDraft: String,
        contentDraft: String
    ): Boolean {
        if (isSaving) return false
        if (!isDirty) return false

        // For new note creation, require both fields.
        return if (isNew) {
            titleDraft.isNotBlank() && contentDraft.isNotBlank()
        } else {
            true
        }
    }
}
