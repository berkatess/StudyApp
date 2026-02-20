package com.ar.domain.note.repository

import com.ar.core.data.FetchStrategy
import com.ar.core.result.Result
import com.ar.domain.note.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf

class FakeNoteRepository : NoteRepository {

    // Basit in-memory “DB”
    private val notesState = MutableStateFlow<List<Note>>(emptyList())

    // getNotes() için Result kontrolü
    private var notesResult: Result<List<Note>> = Result.Success(emptyList())
    private var emitLoadingFirst: Boolean = false

    fun setNotes(list: List<Note>) {
        notesState.value = list
        notesResult = Result.Success(list)
    }

    fun setError(message: String) {
        notesResult = Result.Error(message)
    }

    fun setLoadingThenNotes(list: List<Note>) {
        emitLoadingFirst = true
        setNotes(list)
    }

    override fun getNotes(strategy: FetchStrategy): Flow<Result<List<Note>>> {
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

    override suspend fun hasAnyNotesLocally(): Boolean = notesState.value.isNotEmpty()


    override suspend fun createNote(note: Note): Result<Note> {
        val newList = notesState.value + note
        notesState.value = newList
        return Result.Success(note)
    }

    override suspend fun updateNote(note: Note): Result<Note> {
        val newList = notesState.value.map { if (it.id == note.id) note else it }
        notesState.value = newList
        return Result.Success(note)
    }

    override suspend fun deleteNote(id: String): Result<Unit> {
        notesState.value = notesState.value.filterNot { it.id == id }
        return Result.Success(Unit)
    }

    override fun getNotesByCategory(categoryId: String, strategy: FetchStrategy): Flow<Result<List<Note>>> {
        error("Not used in these unit tests")
    }

    override fun getNoteById(id: String): Flow<Result<Note>> =
        error("Not used in these unit tests")

    override suspend fun refreshNotes(): Result<Unit> =
        error("Not used in these unit tests")
}
