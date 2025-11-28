package com.ar.data.note.repository

import com.ar.core.result.Result
import com.ar.core.coroutines.DispatcherProvider
import com.ar.data.note.local.NoteLocalDataSource
import com.ar.data.note.mapper.toDomain
import com.ar.data.note.mapper.toEntity
import com.ar.data.note.mapper.toRemoteDto
import com.ar.data.note.remote.NoteRemoteDataSource
import com.ar.domain.note.model.Note
import com.ar.domain.note.repository.NoteRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val remote: NoteRemoteDataSource,
    private val local: NoteLocalDataSource,
    private val dispatchers: DispatcherProvider
) : NoteRepository {

    override suspend fun getNotes(): Result<List<Note>> = withContext(dispatchers.io) {
        try {
            //remote
            val remoteData = remote.getNotes()
            val domainNotes = remoteData.map { (id, dto) -> dto.toDomain(id) }

            //save to local
            local.saveNotes(domainNotes.map { it.toEntity() })

            Result.Success(domainNotes)
        } catch (e: Exception) {
            //failed remote try local
            val localEntities = local.getNotes()
            val localDomain = localEntities.map { it.toDomain() }

            if (localDomain.isNotEmpty()) {
                Result.Success(localDomain)
            } else {
                Result.Error("Failed to load notes", e)
            }
        }
    }

    override suspend fun getNotesByCategory(categoryId: String): Result<List<Note>> =
        withContext(dispatchers.io) {
            try {
                val remoteData = remote.getNotesByCategory(categoryId)
                val domainNotes = remoteData.map { (id, dto) -> dto.toDomain(id) }

                local.saveNotes(domainNotes.map { it.toEntity() })

                Result.Success(domainNotes)
            } catch (e: Exception) {
                val localEntities = local.getNotesByCategory(categoryId)
                val localDomain = localEntities.map { it.toDomain() }

                if (localDomain.isNotEmpty()) {
                    Result.Success(localDomain)
                } else {
                    Result.Error("Failed to load notes by category", e)
                }
            }
        }

    override suspend fun getNoteById(id: String): Result<Note> =
        withContext(dispatchers.io) {
            try {
                val remoteResult = remote.getNoteById(id)
                    ?: return@withContext Result.Error("Note not found")

                val (docId, dto) = remoteResult
                val domainNote = dto.toDomain(docId)

                local.saveNote(domainNote.toEntity())

                Result.Success(domainNote)
            } catch (e: Exception) {
                val localEntity = local.getNoteById(id)
                    ?: return@withContext Result.Error("Note not found locally", e)

                Result.Success(localEntity.toDomain())
            }
        }

    override suspend fun createNote(note: Note): Result<Note> =
        withContext(dispatchers.io) {
            return@withContext try {
                val dto = note.toRemoteDto()
                val (id, savedDto) = remote.createNote(dto)
                val savedNote = savedDto.toDomain(id)

                local.saveNote(savedNote.toEntity())

                Result.Success(savedNote)
            } catch (e: Exception) {
                Result.Error("Failed to create note", e)
            }
        }

    override suspend fun updateNote(note: Note): Result<Note> =
        withContext(dispatchers.io) {
            return@withContext try {
                val dto = note.toRemoteDto()
                val (id, updatedDto) = remote.updateNote(note.id, dto)
                val updatedNote = updatedDto.toDomain(id)

                local.saveNote(updatedNote.toEntity())

                Result.Success(updatedNote)
            } catch (e: Exception) {
                Result.Error("Failed to update note", e)
            }
        }

    override suspend fun deleteNote(id: String): Result<Unit> =
        withContext(dispatchers.io) {
            return@withContext try {
                remote.deleteNote(id)
                local.deleteNote(id)
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error("Failed to delete note", e)
            }
        }
}