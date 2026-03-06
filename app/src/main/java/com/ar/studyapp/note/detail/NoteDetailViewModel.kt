package com.ar.studyapp.note.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ar.core.result.Result
import com.ar.domain.note.model.Note
import com.ar.domain.note.usecase.CreateNoteUseCase
import com.ar.domain.note.usecase.UpdateNoteUseCase
import com.ar.studyapp.note.navigation.NoteDestinations
import com.ar.studyapp.R
import com.ar.studyapp.error.toMessageResOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface NoteDetailUiState {
    object Loading : NoteDetailUiState
    data class Error(
        val messageRes: Int,
        val messageArg: String? = null
    ) : NoteDetailUiState

    data class Success(
        val note: Note,
        val isNew: Boolean,
        val titleDraft: String,
        val contentDraft: String,
        val selectedCategoryId: String? = null,
        val isSaving: Boolean = false,
        val saveErrorRes: Int? = null,
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

    fun start(mode: String?, noteId: String?) {
        val normalizedMode = mode ?: NoteDestinations.MODE_EDIT
        val normalizedId = noteId?.trim()

        val key = "${normalizedMode}_${normalizedId.orEmpty()}"
        if (currentKey == key) return
        currentKey = key

        if (normalizedMode == NoteDestinations.MODE_CREATE) {
            startNewDraft()
        } else {
            setLoading()
        }
    }

    fun setLoading() {
        val current = _uiState.value
        if (current is NoteDetailUiState.Success && current.isDirty) return
        _uiState.value = NoteDetailUiState.Loading
    }

    fun showNotFound(noteId: String) {
        val current = _uiState.value
        if (current is NoteDetailUiState.Success && current.isDirty) return
        _uiState.value = NoteDetailUiState.Error(
            messageRes = R.string.note_error_not_found_with_id,
            messageArg = noteId
        )
    }

    fun onNoteUpdated(note: Note) {
        val current = _uiState.value

        if (current !is NoteDetailUiState.Success) {
            _uiState.value = NoteDetailUiState.Success(
                note = note,
                isNew = false,
                titleDraft = note.title,
                contentDraft = note.content,
                selectedCategoryId = note.categoryId
            ).withDerivedState()
            return
        }

        if (current.isDirty) {
            _uiState.value = current.copy(note = note, saveErrorRes = null).withDerivedState()
            return
        }

        _uiState.value = current.copy(
            note = note,
            titleDraft = note.title,
            contentDraft = note.content,
            selectedCategoryId = note.categoryId,
            saveErrorRes = null
        ).withDerivedState()
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
            selectedCategoryId = null
        ).withDerivedState()
    }

    fun onEvent(event: NoteDetailEvent) {
        when (event) {
            is NoteDetailEvent.TitleChanged -> updateForm { copy(titleDraft = event.value) }
            is NoteDetailEvent.ContentChanged -> updateForm { copy(contentDraft = event.value) }
            is NoteDetailEvent.CategoryChanged -> updateForm { copy(selectedCategoryId = event.categoryId) }
        }
    }

    private fun updateForm(
        transform: NoteDetailUiState.Success.() -> NoteDetailUiState.Success
    ) {
        val current = _uiState.value as? NoteDetailUiState.Success ?: return
        _uiState.value = current
            .transform()
            .copy(saveErrorRes = null)
            .withDerivedState()
    }

    fun save(onCreated: (String) -> Unit) {
        val current = _uiState.value as? NoteDetailUiState.Success ?: return
        if (!current.canSave) return

        viewModelScope.launch {
            _uiState.value = current.copy(
                isSaving = true,
                saveErrorRes = null
            ).withDerivedState()

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
                        saveErrorRes = null
                    ).withDerivedState()

                    if (current.isNew) onCreated(saved.id)
                }

                is Result.Error -> {
                    _uiState.value = current.copy(
                        isSaving = false,
                        saveErrorRes = result.error.toMessageResOrNull()
                            ?: R.string.note_error_save_failed_fallback
                    ).withDerivedState()
                }

                is Result.Loading -> Unit
            }
        }
    }

    private fun NoteDetailUiState.Success.withDerivedState(): NoteDetailUiState.Success {
        val dirty = if (isNew) {
            titleDraft.isNotBlank() || contentDraft.isNotBlank() || selectedCategoryId != null
        } else {
            titleDraft != note.title ||
                    contentDraft != note.content ||
                    selectedCategoryId != note.categoryId
        }

        val hasMeaningfulInput = titleDraft.isNotBlank() || contentDraft.isNotBlank()

        val saveEnabled = !isSaving &&
                dirty &&
                hasMeaningfulInput

        return copy(
            isDirty = dirty,
            canSave = saveEnabled
        )
    }
}