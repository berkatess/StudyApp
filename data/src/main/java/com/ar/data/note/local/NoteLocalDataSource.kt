package com.ar.data.note.local

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
}