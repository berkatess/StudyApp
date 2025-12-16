package com.ar.studyapp.note.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ar.core.result.Result
import com.ar.domain.category.model.Category
import com.ar.domain.category.usecase.ObserveCategoriesUseCase
import com.ar.domain.note.model.Note
import com.ar.domain.note.usecase.CreateNoteUseCase
import com.ar.domain.note.usecase.GetNoteByIdUseCase
import com.ar.domain.note.usecase.UpdateNoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Not oluşturma/düzenleme formunun state'i.
 */
data class NoteEditFormState(
    val title: String = "",
    val content: String = "",
    val selectedCategoryId: String? = null,
    val isNew: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Ekrandaki genel state:
 * - isLoading: ilk yükleme (not + kategoriler)
 * - categories: kategori listesi (chip'ler için)
 * - form: form verileri
 */
data class NoteEditUiState(
    val isLoading: Boolean = true,
    val categories: List<Category> = emptyList(),
    val form: NoteEditFormState = NoteEditFormState()
)

@HiltViewModel
class NoteEditViewModel @Inject constructor(
    private val getNoteByIdUseCase: GetNoteByIdUseCase,
    private val createNoteUseCase: CreateNoteUseCase,
    private val updateNoteUseCase: UpdateNoteUseCase,
    private val observeCategoriesUseCase: ObserveCategoriesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteEditUiState())
    val uiState: StateFlow<NoteEditUiState> = _uiState

    private var currentNoteId: String? = null
    private var categoriesJob: Job? = null

    // Update sırasında createdAt'ı korumak için:
    private var existingCreatedAt: Instant? = null

    /**
     * Route'tan gelen noteId (null ise yeni not).
     * Bunu NoteEditRoute içinde LaunchedEffect ile çağıracağız.
     */
    fun start(noteId: String?) {
        // Aynı noteId için tekrar başlatma
        if (currentNoteId == noteId && _uiState.value.isLoading.not()) return

        currentNoteId = noteId
        existingCreatedAt = null

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            form = _uiState.value.form.copy(
                errorMessage = null,
                isSaving = false
            )
        )

        observeCategoriesOnce()

        if (noteId != null) {
            loadNote(noteId)
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                form = NoteEditFormState(isNew = true)
            )
        }
    }

    private fun observeCategoriesOnce() {
        if (categoriesJob != null) return

        categoriesJob = viewModelScope.launch {
            observeCategoriesUseCase().collectLatest { result ->
                when (result) {
                    is Result.Loading -> Unit
                    is Result.Success -> {
                        _uiState.value = _uiState.value.copy(categories = result.data)
                    }
                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            form = _uiState.value.form.copy(
                                errorMessage = result.message ?: "Failed to load categories"
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Edit modunda, tek seferlik not yükler.
     * Flow<Result<Note>> içinden ilk "Loading olmayan" sonucu alıyoruz.
     */
    private fun loadNote(id: String) {
        viewModelScope.launch {
            val result = getNoteByIdUseCase(id).first { it !is Result.Loading }

            when (result) {
                is Result.Success -> {
                    val note = result.data
                    existingCreatedAt = note.createdAt

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        form = NoteEditFormState(
                            title = note.title,
                            content = note.content,
                            selectedCategoryId = note.categoryId,
                            isNew = false,
                            isSaving = false,
                            errorMessage = null
                        )
                    )
                }

                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        form = _uiState.value.form.copy(
                            errorMessage = result.message ?: "Failed to load note"
                        )
                    )
                }

                is Result.Loading -> Unit // first { !Loading } dediğimiz için pratikte gelmez
            }
        }
    }

    // --- Form event'leri ---

    fun onTitleChange(newTitle: String) {
        _uiState.value = _uiState.value.copy(
            form = _uiState.value.form.copy(
                title = newTitle,
                errorMessage = null
            )
        )
    }

    fun onContentChange(newContent: String) {
        _uiState.value = _uiState.value.copy(
            form = _uiState.value.form.copy(
                content = newContent,
                errorMessage = null
            )
        )
    }

    fun onCategorySelected(categoryId: String?) {
        _uiState.value = _uiState.value.copy(
            form = _uiState.value.form.copy(
                selectedCategoryId = categoryId,
                errorMessage = null
            )
        )
    }

    /**
     * Kaydet butonu.
     * - isNew → Create
     * - değil → Update
     */
    fun onSaveNote(onSuccess: () -> Unit) {
        val state = _uiState.value
        val form = state.form

        // Basit validasyon
        if (form.title.isBlank()) {
            _uiState.value = state.copy(form = form.copy(errorMessage = "Title cannot be empty"))
            return
        }
        if (form.content.isBlank()) {
            _uiState.value = state.copy(form = form.copy(errorMessage = "Content cannot be empty"))
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                form = _uiState.value.form.copy(isSaving = true, errorMessage = null)
            )

            val now = Instant.now()
            val note = Note(
                id = currentNoteId.orEmpty(), // Create tarafında repo/id üretimi varsa bunu boş bırakmak OK
                title = form.title,
                content = form.content,
                categoryId = form.selectedCategoryId,
                createdAt = if (form.isNew) now else (existingCreatedAt ?: now),
                updatedAt = now
            )

            val result = if (form.isNew) createNoteUseCase(note) else updateNoteUseCase(note)

            when (result) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        form = _uiState.value.form.copy(isSaving = false, errorMessage = null)
                    )
                    onSuccess()
                }

                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        form = _uiState.value.form.copy(
                            isSaving = false,
                            errorMessage = result.message ?: "Failed to save note"
                        )
                    )
                }

                is Result.Loading -> Unit
            }
        }
    }
}
