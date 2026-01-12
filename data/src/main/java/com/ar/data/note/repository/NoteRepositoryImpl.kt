package com.ar.data.note.repository

import com.ar.core.result.Result
import com.ar.core.coroutines.DispatcherProvider
import com.ar.core.data.FetchStrategy
import com.ar.core.network.NetworkMonitor
import com.ar.core.sync.NoteSyncScheduler
import com.ar.data.note.local.NoteEntity
import com.ar.data.note.local.NoteLocalDataSource
import com.ar.data.sync.SyncState
import com.ar.data.note.mapper.toDomain
import com.ar.data.note.mapper.toEntity
import com.ar.data.note.mapper.toRemoteDto
import com.ar.data.note.remote.NoteRemoteDataSource
import com.ar.data.note.remote.NoteRemoteDto
import com.ar.domain.note.model.Note
import com.ar.domain.note.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.awaitClose
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val remote: NoteRemoteDataSource,
    private val local: NoteLocalDataSource,
    private val dispatchers: DispatcherProvider,
    private val noteSyncScheduler: NoteSyncScheduler,
    private val networkMonitor: NetworkMonitor
) : NoteRepository {


    override fun getNotesByCategory(categoryId: String): Flow<Result<List<Note>>> = channelFlow {
        send(Result.Loading)

        val localJob = launch(dispatchers.io) {
            local.observeNotesByCategory(categoryId).collect { entities ->
                send(Result.Success(entities.map { it.toDomain() }))
            }
        }

        val remoteJob = launch(dispatchers.io) {
            try {
                val remoteData = remote.getNotesByCategory(categoryId)
                val domainNotes = remoteData.map { (id, dto) -> dto.toDomain(id) }
                local.saveNotes(domainNotes.map { it.toEntity(syncState = SyncState.SYNCED) })
            } catch (_: Exception) { }
        }

        awaitClose {
            localJob.cancel()
            remoteJob.cancel()
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

    override fun getNotes(strategy: FetchStrategy): Flow<Result<List<Note>>> = when (strategy) {
        FetchStrategy.FAST     -> getNotesFast()
        FetchStrategy.CACHED   -> getNotesCached()
        FetchStrategy.FRESH    -> getNotesFresh(saveToLocal = true)
        FetchStrategy.FALLBACK -> getNotesFallback(saveToLocal = true)
        FetchStrategy.SYNCED   -> getNotesSynced()
    }

    private fun getNotesFast(): Flow<Result<List<Note>>> = channelFlow {
        send(Result.Loading)

        val localJob = launch(dispatchers.io) {
            local.observeNotes().collect { entities ->
                send(Result.Success(entities.map { it.toDomain() }))
            }
        }

        val remoteJob = launch(dispatchers.io) {
            if (networkMonitor.isOnlineNow()) {
                runCatching {
                    val remoteList = remote.fetchNotesOnce()
                    val domainNotes = remoteList.map { (id, dto) -> dto.toDomain(id) }
                    local.saveNotes(domainNotes.map { it.toEntity(syncState = SyncState.SYNCED) })
                }
            }
        }

        awaitClose {
            localJob.cancel()
            remoteJob.cancel()
        }
    }.flowOn(dispatchers.io)

    private fun getNotesCached(): Flow<Result<List<Note>>> =
        local.observeNotes()
            .map<List<NoteEntity>, Result<List<Note>>> { entities ->
                Result.Success(entities.map { it.toDomain() })
            }
            .onStart { emit(Result.Loading) }
            .catch { e -> emit(Result.Error("Failed to load notes locally", e)) }
            .flowOn(dispatchers.io)


    private fun getNotesFresh(saveToLocal: Boolean): Flow<Result<List<Note>>> =
        remote.getNotes()
            .map<List<Pair<String, NoteRemoteDto>>, Result<List<Note>>> { pairs ->
                val domain = pairs.map { (id, dto) -> dto.toDomain(id) }

                if (saveToLocal) {
                    runCatching {
                        local.saveNotes(domain.map { it.toEntity(syncState = SyncState.SYNCED) })
                    }
                }

                Result.Success(domain)
            }
            .onStart { emit(Result.Loading) }
            .catch { e -> emit(Result.Error("Failed to load notes from remote", e)) }
            .flowOn(dispatchers.io)


    private fun getNotesFallback(saveToLocal: Boolean): Flow<Result<List<Note>>> = channelFlow {
        send(Result.Loading)

        val remoteOk = runCatching {
            remote.getNotes().collect { pairs ->
                val domain = pairs.map { (id, dto) -> dto.toDomain(id) }
                if (saveToLocal) {
                    runCatching {
                        local.saveNotes(domain.map { it.toEntity(syncState = SyncState.SYNCED) })
                    }
                }
                send(Result.Success(domain))
            }
        }.isSuccess

        if (!remoteOk) {
            // remote patladı -> local observe
            local.observeNotes().collect { entities ->
                send(Result.Success(entities.map { it.toDomain() }))
            }
        }
    }.catch { e ->
        emit(Result.Error("Failed to load notes (remote + local fallback)", e))
    }.flowOn(dispatchers.io)



    override suspend fun createNote(note: Note): Result<Note> {
        val id = note.id.ifBlank { UUID.randomUUID().toString() }
        val toSave = note.copy(id = id)

        return try {
            local.saveNote(toSave.toEntity(syncState = SyncState.PENDING))

            if (networkMonitor.isOnlineNow()) {
                runCatching {
                    remote.updateNote(toSave.id, toSave.toRemoteDto())
                    local.markAsSynced(toSave.id)
                }.getOrElse {
                    noteSyncScheduler.schedule()
                }
            } else {
                noteSyncScheduler.schedule()
            }

            Result.Success(toSave)
        } catch (e: Exception) {
            Result.Error("Failed to save note locally", e)
        }
    }

    private fun getNotesSynced(): Flow<Result<List<Note>>> = kotlinx.coroutines.flow.flow {
        emit(Result.Loading)

        if (!networkMonitor.isOnlineNow()) {
            emit(Result.Error("No internet connection"))
            return@flow
        }

        val remoteList = remote.fetchNotesOnce()
        val domainNotes = remoteList.map { (id, dto) -> dto.toDomain(id) }
        local.saveNotes(domainNotes.map { it.toEntity(syncState = SyncState.SYNCED) })

        emit(Result.Success(domainNotes))
    }.catch { e ->
        emit(Result.Error("Failed to sync notes", e))
    }.flowOn(dispatchers.io)




    override suspend fun updateNote(note: Note): Result<Note> {
        return try {
            local.saveNote(note.toEntity(syncState = SyncState.PENDING))

            if (networkMonitor.isOnlineNow()) {
                runCatching {
                    remote.updateNote(note.id, note.toRemoteDto())
                    local.markAsSynced(note.id)
                }.getOrElse {
                    noteSyncScheduler.schedule()
                }
            } else {
                noteSyncScheduler.schedule()
            }

            Result.Success(note)
        } catch (e: Exception) {
            Result.Error("Failed to update note locally", e)
        }
    }



    override suspend fun deleteNote(id: String): Result<Unit> {
        return try {
            local.markDeleted(id)

            if (networkMonitor.isOnlineNow()) {
                runCatching {
                    remote.deleteNote(id)
                    local.hardDelete(id)
                }.getOrElse {
                    noteSyncScheduler.schedule()
                }
            } else {
                noteSyncScheduler.schedule()
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to delete note", e)
        }
    }

    override suspend fun hasAnyNotesLocally(): Boolean {
        return local.hasAnyNotes()
    }


    override suspend fun refreshNotes(): Result<Unit> {
        return try {
            if (!networkMonitor.isOnlineNow()) return Result.Success(Unit)

            val remoteList = remote.fetchNotesOnce()
            val domainNotes = remoteList.map { (id, dto) -> dto.toDomain(id) }
            local.saveNotes(domainNotes.map { it.toEntity(syncState = SyncState.SYNCED) })

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to refresh notes", e)
        }
    }

}
