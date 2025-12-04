package com.ar.domain.note.usecase

import com.ar.core.result.Result
import com.ar.core.usecase.BaseUseCase
import com.ar.domain.note.repository.NoteRepository
import javax.inject.Inject

class DeleteNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) : BaseUseCase<String, Unit>() {

    override suspend fun execute(params: String): Result<Unit> {
        val id = params.trim()
        if (id.isEmpty()) {
            return Result.Error("Note id cannot be empty")
        }

        return repository.deleteNote(id)
    }
}
