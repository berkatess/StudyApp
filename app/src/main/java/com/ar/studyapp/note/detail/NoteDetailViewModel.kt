package com.ar.studyapp.note.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ar.core.result.Result
import com.ar.domain.note.model.Note
import com.ar.domain.note.repository.NoteRepository
import com.ar.domain.note.usecase.GetNoteByIdUseCase
import com.ar.domain.note.usecase.UpdateNoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface NoteDetailUiState {
    object Loading : NoteDetailUiState
    data class Error(val message: String) : NoteDetailUiState

    data class Success(
        val note: Note,
        val titleDraft: String,
        val contentDraft: String,
        val isSaving: Boolean = false,
        val saveError: String? = null,
        val isDirty: Boolean = false
    ) : NoteDetailUiState
}

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    private val getNoteByIdUseCase: GetNoteByIdUseCase,
    private val updateNoteUseCase: UpdateNoteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<NoteDetailUiState>(NoteDetailUiState.Loading)
    val uiState: StateFlow<NoteDetailUiState> = _uiState

    fun loadNote(noteId: String) {
        viewModelScope.launch {
            getNoteByIdUseCase(noteId).collect { result ->
                when (result) {
                    is Result.Loading -> _uiState.value = NoteDetailUiState.Loading

                    is Result.Error -> _uiState.value = NoteDetailUiState.Error(
                        result.message ?: "Failed to load note"
                    )

                    is Result.Success -> {
                        val note = result.data
                        _uiState.value = NoteDetailUiState.Success(
                            note = note,
                            titleDraft = note.title,
                            contentDraft = note.content,
                            isDirty = false
                        )
                    }
                }
            }
        }
    }

    fun onEvent(event: NoteDetailEvent) {
        val current = _uiState.value
        if (current !is NoteDetailUiState.Success) return

        when (event) {
            is NoteDetailEvent.TitleChanged -> {
                val newTitle = event.value
                _uiState.value = current.copy(
                    titleDraft = newTitle,
                    isDirty = newTitle != current.note.title || current.contentDraft != current.note.content,
                    saveError = null
                )
            }

            is NoteDetailEvent.ContentChanged -> {
                val newContent = event.value
                _uiState.value = current.copy(
                    contentDraft = newContent,
                    isDirty = current.titleDraft != current.note.title || newContent != current.note.content,
                    saveError = null
                )
            }
        }
    }

    private fun save(current: NoteDetailUiState.Success) {
        if (!current.isDirty || current.isSaving) return

        viewModelScope.launch {
            _uiState.value = current.copy(isSaving = true, saveError = null)

            val updated = current.note.copy(
                title = current.titleDraft,
                content = current.contentDraft
            )

            when (val res = updateNoteUseCase(updated)) {
                is Result.Success -> {
                    val saved = res.data
                    _uiState.value = current.copy(
                        note = saved,
                        titleDraft = saved.title,
                        contentDraft = saved.content,
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
}
