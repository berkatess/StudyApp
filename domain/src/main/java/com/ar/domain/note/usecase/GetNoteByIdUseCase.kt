package com.ar.domain.note.usecase

import com.ar.core.result.Result
import com.ar.domain.note.model.Note
import com.ar.domain.note.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNoteByIdUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    operator fun invoke(id: String): Flow<Result<Note>> {
        return repository.getNoteById(id)
    }
}
