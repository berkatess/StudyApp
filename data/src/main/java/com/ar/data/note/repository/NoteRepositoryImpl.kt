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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val remote: NoteRemoteDataSource,
    private val local: NoteLocalDataSource,
    private val dispatchers: DispatcherProvider
) : NoteRepository {

    override fun getNotes(): Flow<Result<List<Note>>> = flow {
        emit(Result.Loading)

        try {
            // Remote
            val remoteData = remote.getNotes()
            val domainNotes = remoteData.map { (id, dto) -> dto.toDomain(id) }

            // Local’e kaydet
            local.saveNotes(domainNotes.map { it.toEntity() })

            emit(Result.Success(domainNotes))
        } catch (e: Exception) {
            // Remote fail → Local’den dene
            val localEntities = local.getNotes()
            val localDomain = localEntities.map { it.toDomain() }

            if (localDomain.isNotEmpty()) {
                emit(Result.Success(localDomain))
            } else {
                emit(Result.Error("Failed to load notes", e))
            }
        }
    }.flowOn(dispatchers.io)

    override fun getNotesByCategory(categoryId: String): Flow<Result<List<Note>>> = flow {
        emit(Result.Loading)

        try {
            val remoteData = remote.getNotesByCategory(categoryId)
            val domainNotes = remoteData.map { (id, dto) -> dto.toDomain(id) }

            local.saveNotes(domainNotes.map { it.toEntity() })

            emit(Result.Success(domainNotes))
        } catch (e: Exception) {
            val localEntities = local.getNotesByCategory(categoryId)
            val localDomain = localEntities.map { it.toDomain() }

            if (localDomain.isNotEmpty()) {
                emit(Result.Success(localDomain))
            } else {
                emit(Result.Error("Failed to load notes by category", e))
            }
        }
    }.flowOn(dispatchers.io)

    override fun getNoteById(id: String): Flow<Result<Note>> = flow {
        emit(Result.Loading)

        try {
            val remoteResult = remote.getNoteById(id)
            if (remoteResult == null) {
                emit(Result.Error("Note not found"))
                return@flow
            }

            val (docId, dto) = remoteResult
            val domainNote = dto.toDomain(docId)

            local.saveNote(domainNote.toEntity())

            emit(Result.Success(domainNote))
        } catch (e: Exception) {
            val localEntity = local.getNoteById(id)
            if (localEntity == null) {
                emit(Result.Error("Note not found locally", e))
            } else {
                emit(Result.Success(localEntity.toDomain()))
            }
        }
    }.flowOn(dispatchers.io)

    override suspend fun createNote(note: Note): Result<Note> {
        return try {
            val dto = note.toRemoteDto()
            val (id, savedDto) = remote.createNote(dto)
            val savedNote = savedDto.toDomain(id)

            local.saveNote(savedNote.toEntity())

            Result.Success(savedNote)
        } catch (e: Exception) {
            Result.Error("Failed to create note", e)
        }
    }

    override suspend fun updateNote(note: Note): Result<Note> {
        return try {
            val dto = note.toRemoteDto()
            val (id, updatedDto) = remote.updateNote(note.id, dto)
            val updatedNote = updatedDto.toDomain(id)

            local.saveNote(updatedNote.toEntity())

            Result.Success(updatedNote)
        } catch (e: Exception) {
            Result.Error("Failed to update note", e)
        }
    }

    override suspend fun deleteNote(id: String): Result<Unit> {
        return try {
            remote.deleteNote(id)
            local.deleteNote(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to delete note", e)
        }
    }
}
