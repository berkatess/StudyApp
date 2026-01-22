package com.ar.domain.note.usecase

import com.ar.core.result.Result
import com.ar.core.usecase.BaseUseCase
import com.ar.domain.note.model.Note
import com.ar.domain.note.repository.NoteRepository
import javax.inject.Inject

class CreateNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) : BaseUseCase<Note, Note>() {

    override suspend fun execute(params: Note): Result<Note> {
        if (params.title.isBlank() && params.content.isBlank()) {
            return Result.Error("Title and content cannot be empty")
        }

        return repository.createNote(params)
    }
}
