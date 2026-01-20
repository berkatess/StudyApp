package com.ar.studyapp.note.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ar.core.result.Result
import com.ar.domain.note.model.Note
import com.ar.domain.note.usecase.UpdateNoteUseCase
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
        val titleDraft: String,
        val contentDraft: String,
        val selectedCategoryId: String? = null,
        val isSaving: Boolean = false,
        val saveError: String? = null,
        val isDirty: Boolean = false
    ) : NoteDetailUiState
}

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    private val updateNoteUseCase: UpdateNoteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<NoteDetailUiState>(NoteDetailUiState.Loading)
    val uiState: StateFlow<NoteDetailUiState> = _uiState

    /**
     * Receives note updates from Home (Single Source of Truth).
     *
     * Rules:
     * - Detail screen must NOT fetch its own data.
     * - If the user is editing (dirty), do NOT override drafts.
     * - If not dirty, keep drafts fully in sync with the latest note.
     */
    fun onNoteUpdated(note: Note) {
        val current = _uiState.value

        // Initial state setup
        if (current !is NoteDetailUiState.Success) {
            _uiState.value = NoteDetailUiState.Success(
                note = note,
                titleDraft = note.title,
                contentDraft = note.content,
                selectedCategoryId = note.categoryId,
                isDirty = false
            )
            return
        }

        // Defensive reset if a different note arrives
        if (current.note.id != note.id) {
            _uiState.value = NoteDetailUiState.Success(
                note = note,
                titleDraft = note.title,
                contentDraft = note.content,
                selectedCategoryId = note.categoryId,
                isDirty = false
            )
            return
        }

        // If user is not editing, sync drafts with the latest note
        if (!current.isDirty) {
            _uiState.value = current.copy(
                note = note,
                titleDraft = note.title,
                contentDraft = note.content,
                selectedCategoryId = note.categoryId,
                saveError = null
            )
            return
        }

        // User is editing: update only base note + recompute dirty
        _uiState.value = current.copy(
            note = note,
            isDirty = isDirty(
                note = note,
                titleDraft = current.titleDraft,
                contentDraft = current.contentDraft,
                selectedCategoryId = current.selectedCategoryId
            )
        )
    }

    fun onEvent(event: NoteDetailEvent) {
        val current = _uiState.value
        if (current !is NoteDetailUiState.Success) return

        when (event) {
            is NoteDetailEvent.TitleChanged -> {
                val newTitle = event.value
                _uiState.value = current.copy(
                    titleDraft = newTitle,
                    isDirty = isDirty(
                        note = current.note,
                        titleDraft = newTitle,
                        contentDraft = current.contentDraft,
                        selectedCategoryId = current.selectedCategoryId
                    ),
                    saveError = null
                )
            }

            is NoteDetailEvent.ContentChanged -> {
                val newContent = event.value
                _uiState.value = current.copy(
                    contentDraft = newContent,
                    isDirty = isDirty(
                        note = current.note,
                        titleDraft = current.titleDraft,
                        contentDraft = newContent,
                        selectedCategoryId = current.selectedCategoryId
                    ),
                    saveError = null
                )
            }

            is NoteDetailEvent.CategoryChanged -> {
                val newCategoryId = event.categoryId
                _uiState.value = current.copy(
                    selectedCategoryId = newCategoryId,
                    isDirty = isDirty(
                        note = current.note,
                        titleDraft = current.titleDraft,
                        contentDraft = current.contentDraft,
                        selectedCategoryId = newCategoryId
                    ),
                    saveError = null
                )
            }
        }
    }

    fun save() {
        val current = _uiState.value
        if (current !is NoteDetailUiState.Success) return
        if (!current.isDirty || current.isSaving) return

        viewModelScope.launch {
            _uiState.value = current.copy(isSaving = true, saveError = null)

            val updated = current.note.copy(
                title = current.titleDraft,
                content = current.contentDraft,
                categoryId = current.selectedCategoryId,
                updatedAt = Instant.now()
            )

            when (val res = updateNoteUseCase(updated)) {
                is Result.Success -> {
                    val saved = res.data
                    _uiState.value = current.copy(
                        note = saved,
                        titleDraft = saved.title,
                        contentDraft = saved.content,
                        selectedCategoryId = saved.categoryId,
                        isSaving = false,
                        isDirty = false,
                        saveError = null
                    )
                }

                is Result.Error -> {
                    _uiState.value = current.copy(
                        isSaving = false,
                        saveError = res.message ?: "Save failed"
                    )
                }

                is Result.Loading -> Unit
            }
        }
    }

    private fun isDirty(
        note: Note,
        titleDraft: String,
        contentDraft: String,
        selectedCategoryId: String?
    ): Boolean {
        return titleDraft != note.title ||
                contentDraft != note.content ||
                selectedCategoryId != note.categoryId
    }
}
