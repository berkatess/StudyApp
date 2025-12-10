package com.ar.studyapp.note.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ar.core.result.Result
import com.ar.domain.note.model.Note
import com.ar.domain.note.usecase.GetNoteByIdUseCase
import com.ar.studyapp.note.navigation.NoteDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// 🔴 ÖNEMLİ: import dagger.hilt.android.lifecycle.HiltViewModel OLMALI
// androidx.hilt.lifecycle.ViewModelInject vs. KULLANMA!

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    private val getNoteByIdUseCase: GetNoteByIdUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Navigation argümanlarından gelen noteId
    private val noteId: String = checkNotNull(
        savedStateHandle[NoteDestinations.NOTE_ID_ARG]
    )

    private val _uiState = MutableStateFlow<NoteDetailUiState>(NoteDetailUiState.Loading)
    val uiState: StateFlow<NoteDetailUiState> = _uiState

    init {
        observeNote()
    }

    private fun observeNote() {
        viewModelScope.launch {
            // GetNoteByIdUseCase Flow<Result<Note>> dönüyor varsayımı
            getNoteByIdUseCase(noteId).collect { result ->
                _uiState.value = when (result) {
                    is Result.Loading -> NoteDetailUiState.Loading
                    is Result.Success -> NoteDetailUiState.Success(result.data)
                    is Result.Error -> NoteDetailUiState.Error(result.message ?: "Unknown error")
                }
            }
        }
    }
}

sealed class NoteDetailUiState {
    object Loading : NoteDetailUiState()
    data class Success(val note: Note) : NoteDetailUiState()
    data class Error(val message: String) : NoteDetailUiState()
}


