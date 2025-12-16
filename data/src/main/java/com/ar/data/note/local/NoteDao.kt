package com.ar.data.note.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes")
    suspend fun getNotes(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE categoryId = :categoryId")
    suspend fun getNotesByCategory(categoryId: String): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE syncState = :state")
    suspend fun getNotesBySyncState(state: SyncState): List<NoteEntity>

    @Query("UPDATE notes SET syncState = :state WHERE id = :id")
    suspend fun updateSyncState(id: String, state: SyncState)

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteEntity>)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: String)
}