package com.ar.domain.note.usecase

import com.ar.core.result.Result
import com.ar.domain.note.repository.NoteRepository
import javax.inject.Inject

class RefreshNotesUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.refreshNotes()
}
