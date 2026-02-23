package com.ar.data.note.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ar.data.sync.SyncState
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes")
    suspend fun getNotes(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE categoryId = :categoryId")
    suspend fun getNotesByCategory(categoryId: String): List<NoteEntity>

    @Query("UPDATE notes SET syncState = :state WHERE id = :id")
    suspend fun updateSyncState(id: String, state: SyncState)

    @Query("SELECT * FROM notes WHERE id = :id AND isDeleted = 0 LIMIT 1")
    suspend fun getNoteById(id: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE id = :id AND isDeleted = 0 LIMIT 1")
    fun observeNoteById(id: String): Flow<NoteEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteEntity>)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: String)

    @Query("UPDATE notes SET isDeleted = 1, syncState = :state WHERE id = :id")
    suspend fun markDeleted(id: String, state: SyncState = SyncState.PENDING_DELETE)

    @Query("SELECT * FROM notes WHERE syncState = :state")
    suspend fun getNotesBySyncState(state: SyncState): List<NoteEntity>

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY createdAtMillis DESC")
    fun observeNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND categoryId = :categoryId ORDER BY createdAtMillis DESC")
    fun observeNotesByCategory(categoryId: String): Flow<List<NoteEntity>>

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun countNotes(): Int
}
