package com.ar.domain.Note.usecase

import com.ar.core.usecase.BaseUseCase
import com.ar.core.result.Result
import com.ar.domain.Note.model.Note
import com.ar.domain.Note.repository.NoteRepository

class GetNotesByCategoryUseCase(
    private val repository: NoteRepository
) : BaseUseCase<String, List<Note>>() {

    override suspend fun execute(params: String): Result<List<Note>> {
        if (params.isBlank()) {
            return Result.Error("Category id cannot be empty")
        }
        return repository.getNotesByCategory(params)
    }
}