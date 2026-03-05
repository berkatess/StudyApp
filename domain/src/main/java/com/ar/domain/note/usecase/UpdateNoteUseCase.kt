package com.ar.domain.note.usecase

import com.ar.core.data.FetchStrategy
import com.ar.core.result.Result
import com.ar.core.usecase.BaseUseCase
import com.ar.domain.error.NoteError
import com.ar.domain.note.model.Note
import com.ar.domain.note.repository.NoteRepository
import javax.inject.Inject

class UpdateNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(note: Note): Result<Note> {
        if (note.id.isBlank()) return Result.Error(error = NoteError.EmptyNoteId)
        if ( note.title.isBlank() && note.content.isBlank())
            return Result.Error(error = NoteError.EmptyNoteId)
        return repository.updateNote(note)
    }
}


