package com.ar.domain.Note.repository

import com.ar.domain.Note.model.Note
import com.ar.core.result.Result

interface NoteRepository {

    suspend fun getNotes(): Result<List<Note>>

    suspend fun getNotesByCategory(categoryId: String): Result<List<Note>>

    suspend fun getNoteById(id: String): Result<Note>

    suspend fun createNote(note: Note): Result<Note>

    suspend fun updateNote(note: Note): Result<Note>

    suspend fun deleteNote(id: String): Result<Unit>
}