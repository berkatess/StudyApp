package com.ar.studyapp.note.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ar.core.result.Result
import com.ar.domain.note.model.Note
import com.ar.domain.note.usecase.GetNotesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteListViewModel @Inject constructor(
    private val getNotesUseCase: GetNotesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotesUiState>(NotesUiState.Loading)
    val uiState: StateFlow<NotesUiState> = _uiState

    init {
        observeNotes()
    }

    private fun observeNotes() {
        viewModelScope.launch {
            getNotesUseCase()
                .collect { result ->
                    _uiState.value = when (result) {
                        is Result.Success -> NotesUiState.Success(result.data)
                        is Result.Error -> NotesUiState.Error(result.message ?: "Unknown error")
                        is Result.Loading -> NotesUiState.Loading
                    }
                }
        }
    }
}

sealed class NotesUiState {
    object Loading : NotesUiState()
    data class Success(val notes: List<Note>) : NotesUiState()
    data class Error(val message: String) : NotesUiState()
}
