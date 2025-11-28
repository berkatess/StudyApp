package com.ar.domain.note.usecase

import com.ar.core.usecase.BaseUseCase
import com.ar.core.result.Result
import com.ar.domain.note.model.Note
import com.ar.domain.note.repository.NoteRepository

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