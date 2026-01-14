package com.ar.domain.note.usecase

import com.ar.core.data.FetchStrategy
import com.ar.core.result.Result
import com.ar.domain.note.model.Note
import com.ar.domain.note.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNotesByCategoryUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    operator fun invoke(
        categoryId: String,
        strategy: FetchStrategy = FetchStrategy.FAST
    ): Flow<Result<List<Note>>> {
        return repository.getNotesByCategory(categoryId, strategy)
    }

}
