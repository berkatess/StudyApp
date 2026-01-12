package com.ar.domain.note.repository

import com.ar.core.data.FetchStrategy
import com.ar.core.result.Result
import com.ar.domain.note.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeNoteRepository : NoteRepository {

    private var notesResult: Result<List<Note>> = Result.Success(emptyList())
    private var emitLoadingFirst: Boolean = false

    fun setNotes(list: List<Note>) {
        notesResult = Result.Success(list)
    }

    fun setError(message: String) {
        notesResult = Result.Error(message)
    }

    fun setLoadingThenNotes(list: List<Note>) {
        emitLoadingFirst = true
        notesResult = Result.Success(list)
    }

    override fun getNotes(strategy: FetchStrategy): Flow<Result<List<Note>>>{
        // If enabled, emit Loading first, then the current result (Success/Error)
        return if (emitLoadingFirst) {
            emitLoadingFirst = false
            kotlinx.coroutines.flow.flow {
                emit(Result.Loading)
                emit(notesResult)
            }
        } else {
            flowOf(notesResult)
        }
    }

    override suspend fun hasAnyNotesLocally(): Boolean =
        (notesResult as? Result.Success)?.data?.isNotEmpty() == true

    override fun getNotesByCategory(categoryId: String): Flow<Result<List<Note>>> =
        error("Not used in this unit test")

    override fun getNoteById(id: String): Flow<Result<Note>> =
        error("Not used in this unit test")

    override suspend fun refreshNotes(): Result<Unit> =
        error("Not used in this unit test")

    override suspend fun createNote(note: Note): Result<Note> =
        error("Not used in this unit test")

    override suspend fun updateNote(note: Note): Result<Note> =
        error("Not used in this unit test")

    override suspend fun deleteNote(id: String): Result<Unit> =
        error("Not used in this unit test")
}
