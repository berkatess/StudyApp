package com.ar.domain.note.repository

import com.ar.core.result.Result
import com.ar.core.data.FetchStrategy
import com.ar.domain.note.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {

    fun getNotes(strategy: FetchStrategy = FetchStrategy.FAST): Flow<Result<List<Note>>>

    fun getNotesByCategory(
        categoryId: String,
        strategy: FetchStrategy = FetchStrategy.FAST
    ): Flow<Result<List<Note>>>


    fun getNoteById(id: String): Flow<Result<Note>>

    suspend fun refreshNotes(): Result<Unit>

    suspend fun hasAnyNotesLocally(): Boolean

    suspend fun createNote(note: Note): Result<Note>

    suspend fun updateNote(note: Note): Result<Note>

    suspend fun deleteNote(id: String): Result<Unit>
}
