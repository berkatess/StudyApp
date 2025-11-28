package com.ar.domain.note.usecase

import com.ar.core.usecase.BaseUseCase
import com.ar.domain.note.model.Note
import com.ar.domain.note.repository.NoteRepository
import com.ar.core.result.Result

class GetNotesUseCase(
    private val repository: NoteRepository
) : BaseUseCase<Unit, List<Note>>() {

    override suspend fun execute(params: Unit): Result<List<Note>> {
        return repository.getNotes()
    }
}