package com.ar.studyapp.note.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ar.core.result.Result
import com.ar.domain.note.model.Note
import com.ar.domain.note.usecase.GetNoteByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * UI'nin kullanacağı state.
 *
 * Loading  : Veri yükleniyor
 * Success  : Not başarıyla yüklendi
 * Error    : Hata mesajı ile birlikte
 */
sealed class NoteDetailUiState {
    object Loading : NoteDetailUiState()
    data class Success(val note: Note) : NoteDetailUiState()
    data class Error(val message: String) : NoteDetailUiState()
}

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    private val getNoteByIdUseCase: GetNoteByIdUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<NoteDetailUiState>(NoteDetailUiState.Loading)
    val uiState: StateFlow<NoteDetailUiState> = _uiState

    // Aynı anda birden fazla akış (Flow) toplamayalım diye Job saklıyoruz
    private var loadJob: Job? = null

    /**
     * Detay ekranı, navigation'dan gelen noteId ile bu fonksiyonu çağırıyor.
     *
     * Bu fonksiyon:
     * 1) Eski dinlemeyi iptal eder (loadJob?.cancel())
     * 2) Yeni Flow'u collect etmeye başlar (getNoteByIdUseCase(id))
     * 3) Gelen Result'a göre NoteDetailUiState günceller
     */
    fun loadNote(id: String) {
        // Eski dinleme varsa iptal et
        loadJob?.cancel()

        // İlk anda Loading göster
        _uiState.value = NoteDetailUiState.Loading

        loadJob = viewModelScope.launch {
            getNoteByIdUseCase(id).collectLatest { result ->
                _uiState.value = when (result) {
                    is Result.Loading -> NoteDetailUiState.Loading

                    is Result.Success -> {
                        NoteDetailUiState.Success(result.data)
                    }

                    is Result.Error -> {
                        NoteDetailUiState.Error(
                            result.message ?: "Failed to load note"
                        )
                    }
                }
            }
        }
    }
}
