package com.ar.domain.Note.usecase

import com.ar.core.usecase.BaseUseCase
import com.ar.core.result.Result
import com.ar.domain.Note.model.Note
import com.ar.domain.Note.repository.NoteRepository

class CreateNoteUseCase(
    private val repository: NoteRepository
) : BaseUseCase<Note, Note>() {

    override suspend fun execute(params: Note): Result<Note> {
        if (params.title.isBlank()) {
            return Result.Error("Title cannot be empty")
        }
        if (params.content.isBlank()) {
            return Result.Error("Content cannot be empty")
        }
        return repository.createNote(params)
    }
}