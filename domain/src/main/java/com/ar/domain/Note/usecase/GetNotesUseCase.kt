package com.ar.domain.Note.usecase

import com.ar.core.usecase.BaseUseCase
import com.ar.domain.Note.model.Note
import com.ar.domain.Note.repository.NoteRepository
import com.ar.core.result.Result

class GetNotesUseCase(
    private val repository: NoteRepository
) : BaseUseCase<Unit, List<Note>>() {

    override suspend fun execute(params: Unit): Result<List<Note>> {
        return repository.getNotes()
    }
}