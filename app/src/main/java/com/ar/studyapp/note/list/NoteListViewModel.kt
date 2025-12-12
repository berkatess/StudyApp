package com.ar.studyapp.note.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ar.core.result.Result
import com.ar.domain.note.model.Note
import com.ar.domain.category.model.Category
import com.ar.domain.note.usecase.GetNotesUseCase
import com.ar.domain.category.usecase.ObserveCategoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Liste ekranında kullanacağımız sadeleştirilmiş UI modeli.
 * Domain model yerine, ekranın ihtiyaç duyduğu alanları bir araya getiriyoruz.
 */
data class NoteListItemUiModel(
    val id: String,
    val title: String,
    val contentPreview: String,
    val categoryName: String?,
    val categoryColorHex: String?
)

/**
 * Liste ekranı UI state'i.
 */
sealed class NotesUiState {
    object Loading : NotesUiState()
    data class Success(val notes: List<NoteListItemUiModel>) : NotesUiState()
    data class Error(val message: String) : NotesUiState()
}

@HiltViewModel
class NoteListViewModel @Inject constructor(
    private val getNotesUseCase: GetNotesUseCase,
    private val observeCategoriesUseCase: ObserveCategoriesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotesUiState>(NotesUiState.Loading)
    val uiState: StateFlow<NotesUiState> = _uiState

    init {
        observeNotesAndCategories()
    }

    /**
     * Notları ve kategorileri aynı anda dinleriz.
     *
     * - getNotesUseCase() → Flow<Result<List<Note>>>
     * - observeCategoriesUseCase() → Flow<Result<List<Category>>>
     *
     * combine ile iki Flow'u birleştirip,
     * hem not hem kategori başarıyla geldiğinde
     * NoteListItemUiModel listesi üretiriz.
     */
    private fun observeNotesAndCategories() {
        viewModelScope.launch {
            combine(
                getNotesUseCase(),
                observeCategoriesUseCase()
            ) { notesResult, categoriesResult ->
                notesResult to categoriesResult
            }.collectLatest { (notesResult, categoriesResult) ->

                // Her iki flow'dan biri Loading ise Loading göster
                if (notesResult is Result.Loading || categoriesResult is Result.Loading) {
                    _uiState.value = NotesUiState.Loading
                    return@collectLatest
                }

                // Hata durumları
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

                // İkisi de Success ise UI modeli üret
                if (notesResult is Result.Success && categoriesResult is Result.Success) {
                    val notes = notesResult.data
                    val categories = categoriesResult.data

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
}
