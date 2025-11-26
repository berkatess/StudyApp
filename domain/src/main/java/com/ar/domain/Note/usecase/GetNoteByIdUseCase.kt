package com.ar.domain.Note.usecase


import com.ar.core.result.Result
import com.ar.core.usecase.BaseUseCase
import com.ar.domain.Note.model.Note
import com.ar.domain.Note.repository.NoteRepository

class GetNoteByIdUseCase(
    private val repository: NoteRepository
) : BaseUseCase<String, Note>() {

    override suspend fun execute(params: String): Result<Note> {
        if (params.isBlank()) {
            return Result.Error("Note id cannot be empty")
        }
        return repository.getNoteById(params)
    }
}