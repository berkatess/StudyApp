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
import kotlinx.coroutines.flow.flow
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


    override fun getNotesByCategory(
        categoryId: String,
        strategy: FetchStrategy
    ): Flow<Result<List<Note>>> = when (strategy) {
        FetchStrategy.FAST     -> getNotesByCategoryFast(categoryId)
        FetchStrategy.CACHED   -> getNotesByCategoryCached(categoryId)
        FetchStrategy.FRESH    -> getNotesByCategoryFresh(categoryId, saveToLocal = true)
        FetchStrategy.FALLBACK -> getNotesByCategoryFallback(categoryId, saveToLocal = true)
        FetchStrategy.SYNCED   -> getNotesByCategorySynced(categoryId)
    }


    private fun getNotesByCategoryFast(
        categoryId: String
    ): Flow<Result<List<Note>>> = channelFlow {
        send(Result.Loading)

        val localJob = launch(dispatchers.io) {
            getNotesLocalByCategory(categoryId).collect {
                send(Result.Success(it))
            }
        }

        val remoteJob = launch(dispatchers.io) {
            if (networkMonitor.isOnlineNow()) {
                runCatching {
                    val notes = fetchNotesRemoteByCategoryOnce(categoryId)
                    cacheRemoteNotesAsSynced(notes)
                }
            }
        }

        awaitClose {
            localJob.cancel()
            remoteJob.cancel()
        }
    }

    private fun getNotesByCategoryCached(categoryId: String): Flow<Result<List<Note>>> =
        getNotesLocalByCategory(categoryId)
            .map<List<Note>, Result<List<Note>>> { Result.Success(it) }
            .onStart { emit(Result.Loading) }
            .catch { e -> emit(Result.Error("Failed to load notes locally", e)) }
            .flowOn(dispatchers.io)


    private fun getNotesByCategoryFresh(
        categoryId: String,
        saveToLocal: Boolean
    ): Flow<Result<List<Note>>> =
        getNotesRemoteByCategory(categoryId)
            .map<List<Note>, Result<List<Note>>> { domain ->
                if (saveToLocal) runCatching { cacheRemoteNotesAsSynced(domain) }
                Result.Success(domain)
            }
            .onStart { emit(Result.Loading) }
            .catch { e -> emit(Result.Error("Failed to load notes from remote", e)) }
            .flowOn(dispatchers.io)


    private fun getNotesByCategoryFallback(
        categoryId: String,
        saveToLocal: Boolean
    ): Flow<Result<List<Note>>> = channelFlow {
        send(Result.Loading)

        val remoteOk = runCatching {
            getNotesRemoteByCategory(categoryId).collect { domain ->
                if (saveToLocal) runCatching { cacheRemoteNotesAsSynced(domain) }
                send(Result.Success(domain))
            }
        }.isSuccess

        if (!remoteOk) {
            getNotesLocalByCategory(categoryId).collect { notes ->
                send(Result.Success(notes))
            }
        }
    }.catch { e ->
        emit(Result.Error("Failed to load notes (remote + local fallback)", e))
    }.flowOn(dispatchers.io)


    private fun getNotesByCategorySynced(categoryId: String): Flow<Result<List<Note>>> = flow {
        emit(Result.Loading)

        if (!networkMonitor.isOnlineNow()) {
            emit(Result.Error("No internet connection"))
            return@flow
        }

        val domainNotes = fetchNotesRemoteByCategoryOnce(categoryId)
        cacheRemoteNotesAsSynced(domainNotes)

        emit(Result.Success(domainNotes))
    }.catch { e ->
        emit(Result.Error("Failed to sync notes", e))
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
            getNotesLocal().collect { notes ->
                send(Result.Success(notes))
            }
        }

        val remoteJob = launch(dispatchers.io) {
            if (networkMonitor.isOnlineNow()) {
                runCatching {
                    val domainNotes = fetchNotesRemoteOnce()
                    cacheRemoteNotesAsSynced(domainNotes)
                }
            }
        }

        awaitClose {
            localJob.cancel()
            remoteJob.cancel()
        }
    }.flowOn(dispatchers.io)


    private fun getNotesCached(): Flow<Result<List<Note>>> =
        getNotesLocal()
            .map<List<Note>, Result<List<Note>>> { notes -> Result.Success(notes) }
            .onStart { emit(Result.Loading) }
            .catch { e -> emit(Result.Error("Failed to load notes locally", e)) }
            .flowOn(dispatchers.io)



    private fun getNotesFresh(saveToLocal: Boolean): Flow<Result<List<Note>>> =
        getNotesRemote()
            .map<List<Note>, Result<List<Note>>> { domain ->
                if (saveToLocal) runCatching { cacheRemoteNotesAsSynced(domain) }
                Result.Success(domain)
            }
            .onStart { emit(Result.Loading) }
            .catch { e -> emit(Result.Error("Failed to load notes from remote", e)) }
            .flowOn(dispatchers.io)



    private fun getNotesFallback(saveToLocal: Boolean): Flow<Result<List<Note>>> = channelFlow {
        send(Result.Loading)

        val remoteOk = runCatching {
            getNotesRemote().collect { domain ->
                if (saveToLocal) runCatching { cacheRemoteNotesAsSynced(domain) }
                send(Result.Success(domain))
            }
        }.isSuccess

        if (!remoteOk) {
            getNotesLocal().collect { notes ->
                send(Result.Success(notes))
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

        val domainNotes = fetchNotesRemoteOnce()
        cacheRemoteNotesAsSynced(domainNotes)

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

            val domainNotes = fetchNotesRemoteOnce()
            cacheRemoteNotesAsSynced(domainNotes)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to refresh notes", e)
        }
    }



    /**
     * Single-source-of-truth helpers used by fetch strategies.
     *
     * Keep these focused on *data retrieval* + *mapping*.
     * Side-effects like caching are done explicitly by the strategy that needs it.
     */
    private fun getNotesLocal(): Flow<List<Note>> =
        local.observeNotes()
            .map { entities -> entities.map { it.toDomain() } }

    private fun getNotesRemote(): Flow<List<Note>> =
        remote.getNotes()
            .map { pairs -> pairs.map { (id, dto) -> dto.toDomain(id) } }

    private suspend fun fetchNotesRemoteOnce(): List<Note> =
        remote.fetchNotesOnce().map { (id, dto) -> dto.toDomain(id) }

    private fun getNotesRemoteByCategory(categoryId: String): Flow<List<Note>> =
        remote.observeNotesByCategory(categoryId)
            .map { pairs -> pairs.map { (id, dto) -> dto.toDomain(id) } }


    private fun getNotesLocalByCategory(categoryId: String): Flow<List<Note>> =
        local.observeNotesByCategory(categoryId)
            .map { entities -> entities.map { it.toDomain() } }

    private suspend fun fetchNotesRemoteByCategoryOnce(
        categoryId: String
    ): List<Note> =
        remote.fetchNotesByCategoryOnce(categoryId)
            .map { (id, dto) -> dto.toDomain(id) }

    private suspend fun cacheRemoteNotesAsSynced(notes: List<Note>) {
        local.saveNotes(
            notes.map { it.toEntity(syncState = SyncState.SYNCED) }
        )
    }


}
