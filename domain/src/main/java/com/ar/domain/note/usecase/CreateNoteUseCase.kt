package com.ar.domain.note.usecase

import com.ar.core.usecase.BaseUseCase
import com.ar.core.result.Result
import com.ar.domain.note.model.Note
import com.ar.domain.note.repository.NoteRepository

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