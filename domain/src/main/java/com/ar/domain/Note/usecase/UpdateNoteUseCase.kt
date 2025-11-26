package com.ar.domain.Note.usecase

import com.ar.domain.Note.repository.NoteRepository
import com.ar.domain.Note.model.Note
import com.ar.core.usecase.BaseUseCase
import com.ar.core.result.Result

class UpdateNoteUseCase(
    private val repository: NoteRepository
) : BaseUseCase<Note, Note>() {

    override suspend fun execute(params: Note): Result<Note> {
        if (params.id.isBlank()) {
            return Result.Error("Note id cannot be empty")
        }
        if (params.title.isBlank()) {
            return Result.Error("Title cannot be empty")
        }

        return repository.updateNote(params)
    }
}