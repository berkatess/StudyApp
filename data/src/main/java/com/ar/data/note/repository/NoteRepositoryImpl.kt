package com.ar.data.note.repository

import com.ar.core.result.Result
import com.ar.core.coroutines.DispatcherProvider
import com.ar.core.sync.NoteSyncScheduler
import com.ar.data.note.local.NoteLocalDataSource
import com.ar.data.note.local.SyncState
import com.ar.data.note.mapper.toDomain
import com.ar.data.note.mapper.toEntity
import com.ar.data.note.mapper.toRemoteDto
import com.ar.data.note.remote.NoteRemoteDataSource
import com.ar.domain.note.model.Note
import com.ar.domain.note.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val remote: NoteRemoteDataSource,
    private val local: NoteLocalDataSource,
    private val dispatchers: DispatcherProvider,
    private val noteSyncScheduler: NoteSyncScheduler
) : NoteRepository {

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


    override fun getNoteById(id: String): Flow<Result<Note>> =
        remote.observeNoteById(id)
            .map { remoteResult ->
                if (remoteResult == null) {
                    Result.Error("Note not found")
                } else {
                    val (docId, dto) = remoteResult
                    val domainNote = dto.toDomain(docId)

                    local.saveNote(domainNote.toEntity())

                    Result.Success(domainNote)
                }
            }
            .onStart {
                emit(Result.Loading)
            }
            .catch { e ->
                val localEntity = local.getNoteById(id)
                if (localEntity != null) {
                    emit(Result.Success(localEntity.toDomain()))
                } else {
                    emit(Result.Error("Failed to observe note", e))
                }
            }
            .flowOn(dispatchers.io)

//    override fun getNoteById(id: String): Flow<Result<Note>> = flow {
//        emit(Result.Loading)
//
//        try {
//            val remoteResult = remote.getNoteById(id)
//            if (remoteResult == null) {
//                emit(Result.Error("Note not found"))
//                return@flow
//            }
//
//            val (docId, dto) = remoteResult
//            val domainNote = dto.toDomain(docId)
//
//            local.saveNote(domainNote.toEntity())
//
//            emit(Result.Success(domainNote))
//        } catch (e: Exception) {
//            val localEntity = local.getNoteById(id)
//            if (localEntity == null) {
//                emit(Result.Error("Note not found locally", e))
//            } else {
//                emit(Result.Success(localEntity.toDomain()))
//            }
//        }
//    }.flowOn(dispatchers.io)

    override fun getNotes(): Flow<Result<List<Note>>> =
        remote.getNotes()
            .map { remoteList ->
                val domainNotes = remoteList.map { (id, dto) -> dto.toDomain(id) }

                // update local
                local.saveNotes(domainNotes.map { it.toEntity() })

                Result.Success(domainNotes) as Result<List<Note>>
            }
            .onStart { emit(Result.Loading) }  // ilk state
            .catch { e ->
                // if remote failed try local
                val localEntities = local.getNotes()
                val localDomain = localEntities.map { it.toDomain() }

                if (localDomain.isNotEmpty()) {
                    emit(Result.Success(localDomain))
                } else {
                    emit(Result.Error("Failed to observe notes", e))
                }
            }
            .flowOn(dispatchers.io)

    override suspend fun createNote(note: Note): Result<Note> {
        val id = note.id.ifBlank { UUID.randomUUID().toString() }
        val toSave = note.copy(id = id)

        return try {
            local.saveNote(
                toSave.toEntity(syncState = SyncState.PENDING)
            )

            noteSyncScheduler.schedule()

            Result.Success(toSave)
        } catch (e: Exception) {
            Result.Error("Failed to save note locally", e)
        }
    }


    override suspend fun updateNote(note: Note): Result<Note> {
        //save local
        local.saveNote(note.toEntity())

        //try remote
        return try {
            val dto = note.toRemoteDto()
            remote.updateNote(note.id, dto)
            Result.Success(note)
        } catch (e: Exception) {
            // Offline → local already saved
            Result.Success(note)
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
