package com.ar.domain.Note.usecase

import com.ar.core.result.Result
import com.ar.core.usecase.BaseUseCase
import com.ar.domain.Note.repository.NoteRepository


class DeleteNoteUseCase(
    private val repository: NoteRepository
) : BaseUseCase<String, Unit>() {

    override suspend fun execute(params: String): Result<Unit> {
        if (params.isBlank()) {
            return Result.Error("Note id cannot be empty")
        }
        return repository.deleteNote(params)
    }
}