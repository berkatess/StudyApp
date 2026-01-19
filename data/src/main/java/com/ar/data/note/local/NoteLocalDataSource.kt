package com.ar.data.note.local

import com.ar.data.sync.SyncState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class NoteLocalDataSource @Inject constructor(
    private val dao: NoteDao
) {
    suspend fun getNotes(): List<NoteEntity> = dao.getNotes()

    suspend fun getNotesByCategory(categoryId: String): List<NoteEntity> =
        dao.getNotesByCategory(categoryId)

    suspend fun getNoteById(id: String): NoteEntity? = dao.getNoteById(id)

    suspend fun saveNotes(notes: List<NoteEntity>) = dao.insertNotes(notes)

    suspend fun saveNote(note: NoteEntity) = dao.insertNote(note)

    suspend fun deleteNote(id: String) = dao.deleteNote(id)

    suspend fun getPendingNotes(): List<NoteEntity> {
        return dao.getNotesBySyncState(SyncState.PENDING)
    }

    suspend fun hasAnyNotes(): Boolean = dao.countNotes() > 0

    suspend fun markAsSynced(id: String) {
        dao.updateSyncState(id, SyncState.SYNCED)
    }

    suspend fun updateNote(note: NoteEntity){

    }

    fun observeNotes(): Flow<List<NoteEntity>> = dao.observeNotes()

    fun observeNotesByCategory(categoryId: String): Flow<List<NoteEntity>> =
        dao.observeNotesByCategory(categoryId)

    suspend fun markDeleted(id: String) = dao.markDeleted(id)

    suspend fun getPendingDeletes(): List<NoteEntity> =
        dao.getNotesBySyncState(SyncState.PENDING_DELETE)

    suspend fun hardDelete(id: String) = dao.hardDelete(id)
}