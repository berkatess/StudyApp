package com.ar.data.note.repository

import com.ar.core.coroutines.DispatcherProvider
import com.ar.core.data.FetchStrategy
import com.ar.core.network.NetworkMonitor
import com.ar.core.result.Result
import com.ar.core.sync.NoteSyncScheduler
import com.ar.data.note.local.NoteLocalDataSource
import com.ar.data.note.mapper.toDomain
import com.ar.data.note.mapper.toEntity
import com.ar.data.note.mapper.toRemoteDto
import com.ar.data.note.remote.NoteRemoteDataSource
import com.ar.data.sync.SyncState
import com.ar.domain.auth.repository.AuthRepository
import com.ar.domain.note.model.Note
import com.ar.domain.note.repository.NoteRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val remote: NoteRemoteDataSource,
    private val local: NoteLocalDataSource,
    private val dispatchers: DispatcherProvider,
    private val noteSyncScheduler: NoteSyncScheduler,
    private val authRepository: AuthRepository,
    private val networkMonitor: NetworkMonitor
) : NoteRepository {

    private fun signedInUserIdOrNull(): String? = authRepository.currentUserIdOrNull()

    override fun getNotesByCategory(
        categoryId: String,
        strategy: FetchStrategy
    ): Flow<Result<List<Note>>> {
        val uid = signedInUserIdOrNull()

        // Local-only mode: remote strategies fall back to cached local behavior.
        if (uid == null && strategy in setOf(FetchStrategy.FRESH, FetchStrategy.FALLBACK, FetchStrategy.SYNCED)) {
            return getNotesByCategoryCached(categoryId)
        }

        return when (strategy) {
            FetchStrategy.FAST     -> getNotesByCategoryFast(categoryId)
            FetchStrategy.CACHED   -> getNotesByCategoryCached(categoryId)
            FetchStrategy.FRESH    -> getNotesByCategoryFresh(categoryId, saveToLocal = true)
            FetchStrategy.FALLBACK -> getNotesByCategoryFallback(categoryId, saveToLocal = true)
            FetchStrategy.SYNCED   -> getNotesByCategorySynced(categoryId)
        }
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
            val uid = signedInUserIdOrNull() ?: return@launch
            if (!networkMonitor.isOnlineNow()) return@launch

            runCatching {
                val notes = fetchNotesRemoteByCategoryOnce(uid, categoryId)
                cacheRemoteNotesAsSynced(notes)
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
    ): Flow<Result<List<Note>>> = flow {
        val uid = signedInUserIdOrNull()
            ?: throw IllegalStateException("Sign-in required for remote fetch")

        emitAll(
            getNotesRemoteByCategory(uid, categoryId)
                .map<List<Note>, Result<List<Note>>> { domain ->
                    if (saveToLocal) runCatching { cacheRemoteNotesAsSynced(domain) }
                    Result.Success(domain)
                }
                .onStart { emit(Result.Loading) }
        )
    }.catch { e ->
        emit(Result.Error("Failed to load notes from remote", e))
    }.flowOn(dispatchers.io)

    private fun getNotesByCategoryFallback(
        categoryId: String,
        saveToLocal: Boolean
    ): Flow<Result<List<Note>>> = channelFlow {
        send(Result.Loading)

        val uid = signedInUserIdOrNull()
        val remoteOk = if (uid != null && networkMonitor.isOnlineNow()) {
            runCatching {
                getNotesRemoteByCategory(uid, categoryId).collect { domain ->
                    if (saveToLocal) runCatching { cacheRemoteNotesAsSynced(domain) }
                    send(Result.Success(domain))
                }
            }.isSuccess
        } else {
            false
        }

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

        val uid = signedInUserIdOrNull()
            ?: run {
                // Local-only mode: behave like cached.
                emitAll(getNotesByCategoryCached(categoryId))
                return@flow
            }

        if (!networkMonitor.isOnlineNow()) {
            emit(Result.Error("No internet connection"))
            return@flow
        }

        val domainNotes = fetchNotesRemoteByCategoryOnce(uid, categoryId)
        cacheRemoteNotesAsSynced(domainNotes)

        emit(Result.Success(domainNotes))
    }.catch { e ->
        emit(Result.Error("Failed to sync notes", e))
    }.flowOn(dispatchers.io)

    override fun getNoteById(id: String): Flow<Result<Note>> = channelFlow {
        send(Result.Loading)

        // Single Source of Truth: UI observes local database.
        val localJob = launch(dispatchers.io) {
            local.observeNoteById(id).collect { entity ->
                if (entity == null) {
                    send(Result.Error("Note not found"))
                } else {
                    send(Result.Success(entity.toDomain()))
                }
            }
        }

        // Best-effort remote refresh: update local cache if signed in.
        val remoteJob = launch(dispatchers.io) {
            val uid = signedInUserIdOrNull() ?: return@launch
            if (!networkMonitor.isOnlineNow()) return@launch

            runCatching {
                remote.observeNoteById(uid, id).collect { remoteResult ->
                    val pair = remoteResult ?: return@collect
                    val (docId, dto) = pair

                    // Do not overwrite local pending changes.
                    val current = local.getNoteById(docId)
                    if (current?.syncState == SyncState.PENDING ||
                        current?.syncState == SyncState.PENDING_DELETE
                    ) {
                        return@collect
                    }

                    val domain = dto.toDomain(docId)
                    local.saveNote(domain.toEntity(syncState = SyncState.SYNCED))
                }
            }
        }

        awaitClose {
            localJob.cancel()
            remoteJob.cancel()
        }
    }.flowOn(dispatchers.io)

    override fun getNotes(strategy: FetchStrategy): Flow<Result<List<Note>>> {
        val uid = signedInUserIdOrNull()

        // Local-only mode: remote strategies fall back to cached local behavior.
        if (uid == null && strategy in setOf(FetchStrategy.FRESH, FetchStrategy.FALLBACK, FetchStrategy.SYNCED)) {
            return getNotesCached()
        }

        return when (strategy) {
            FetchStrategy.FAST     -> getNotesFast()
            FetchStrategy.CACHED   -> getNotesCached()
            FetchStrategy.FRESH    -> getNotesFresh(saveToLocal = true)
            FetchStrategy.FALLBACK -> getNotesFallback(saveToLocal = true)
            FetchStrategy.SYNCED   -> getNotesSynced()
        }
    }

    private fun getNotesFast(): Flow<Result<List<Note>>> = channelFlow {
        send(Result.Loading)

        val localJob = launch(dispatchers.io) {
            getNotesLocal().collect { notes ->
                send(Result.Success(notes))
            }
        }

        val remoteJob = launch(dispatchers.io) {
            val uid = signedInUserIdOrNull() ?: return@launch
            if (!networkMonitor.isOnlineNow()) return@launch

            runCatching {
                val domainNotes = fetchNotesRemoteOnce(uid)
                cacheRemoteNotesAsSynced(domainNotes)
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

    private fun getNotesFresh(saveToLocal: Boolean): Flow<Result<List<Note>>> = flow {
        val uid = signedInUserIdOrNull()
            ?: throw IllegalStateException("Sign-in required for remote fetch")

        emitAll(
            getNotesRemote(uid)
                .map<List<Note>, Result<List<Note>>> { domain ->
                    if (saveToLocal) runCatching { cacheRemoteNotesAsSynced(domain) }
                    Result.Success(domain)
                }
                .onStart { emit(Result.Loading) }
        )
    }.catch { e ->
        emit(Result.Error("Failed to load notes from remote", e))
    }.flowOn(dispatchers.io)

    private fun getNotesFallback(saveToLocal: Boolean): Flow<Result<List<Note>>> = channelFlow {
        send(Result.Loading)

        val uid = signedInUserIdOrNull()
        val remoteOk = if (uid != null && networkMonitor.isOnlineNow()) {
            runCatching {
                getNotesRemote(uid).collect { domain ->
                    if (saveToLocal) runCatching { cacheRemoteNotesAsSynced(domain) }
                    send(Result.Success(domain))
                }
            }.isSuccess
        } else {
            false
        }

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
                val uid = authRepository.currentNonAnonymousUserIdOrNull()
                if (uid != null) {
                    runCatching {
                        remote.updateNote(uid, toSave.id, toSave.toRemoteDto())
                        local.markAsSynced(toSave.id)
                    }.getOrElse {
                        noteSyncScheduler.schedule()
                    }
                } else {
                    // Not signed in with a real account yet; keep it pending and let it sync after sign-in.
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

    override suspend fun updateNote(note: Note): Result<Note> {
        if (note.id.isBlank()) return Result.Error("Note id cannot be empty")

        return try {
            // Local: optimistic update (PENDING)
            local.updateNote(note.toEntity(syncState = SyncState.PENDING))

            if (networkMonitor.isOnlineNow()) {
                val uid = authRepository.currentNonAnonymousUserIdOrNull()
                if (uid != null) {
                    runCatching {
                        remote.updateNote(uid, note.id, note.toRemoteDto())
                        local.markAsSynced(note.id)
                    }.getOrElse {
                        noteSyncScheduler.schedule()
                    }
                } else {
                    // Not signed in with a real account yet; keep it pending and let it sync after sign-in.
                    noteSyncScheduler.schedule()
                }
            } else {
                noteSyncScheduler.schedule()
            }

            Result.Success(note)
        } catch (e: Exception) {
            Result.Error("Failed to update note", e)
        }
    }

    override suspend fun deleteNote(id: String): Result<Unit> {
        return try {
            val uid = authRepository.currentNonAnonymousUserIdOrNull()

            if (uid == null) {
                // Signed-out mode: treat delete as local-only.
                // The cloud copy is considered a backup, so it should not be removed.
                local.hardDelete(id)
                return Result.Success(Unit)
            }

            // Signed-in mode: delete locally first, then sync the deletion to cloud.
            local.markDeleted(id)

            if (networkMonitor.isOnlineNow()) {
                runCatching {
                    remote.deleteNote(uid, id)
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

    private fun getNotesSynced(): Flow<Result<List<Note>>> = flow {
        emit(Result.Loading)

        val uid = signedInUserIdOrNull()
            ?: run {
                // Local-only mode: behave like cached.
                emitAll(getNotesCached())
                return@flow
            }

        if (!networkMonitor.isOnlineNow()) {
            emit(Result.Error("No internet connection"))
            return@flow
        }

        val domainNotes = fetchNotesRemoteOnce(uid)
        cacheRemoteNotesAsSynced(domainNotes)

        emit(Result.Success(domainNotes))
    }.catch { e ->
        emit(Result.Error("Failed to sync notes", e))
    }.flowOn(dispatchers.io)


    override suspend fun hasAnyNotesLocally(): Boolean {
        return local.hasAnyNotes()
    }

    override suspend fun refreshNotes(): Result<Unit> {
        return try {
            val uid = signedInUserIdOrNull()
            if (uid == null) return Result.Success(Unit)
            if (!networkMonitor.isOnlineNow()) return Result.Success(Unit)

            val domainNotes = fetchNotesRemoteOnce(uid)
            cacheRemoteNotesAsSynced(domainNotes)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to refresh notes", e)
        }
    }

    private fun getNotesLocal(): Flow<List<Note>> =
        local.observeNotes()
            .map { entities -> entities.map { it.toDomain() } }

    private fun getNotesRemote(uid: String): Flow<List<Note>> = flow {
        emitAll(
            remote.getNotes(uid)
                .map { pairs -> pairs.map { (id, dto) -> dto.toDomain(id) } }
        )
    }

    private suspend fun fetchNotesRemoteOnce(uid: String): List<Note> {
        return remote.fetchNotesOnce(uid).map { (id, dto) -> dto.toDomain(id) }
    }

    private fun getNotesRemoteByCategory(uid: String, categoryId: String): Flow<List<Note>> = flow {
        emitAll(
            remote.observeNotesByCategory(uid, categoryId)
                .map { pairs -> pairs.map { (id, dto) -> dto.toDomain(id) } }
        )
    }

    private fun getNotesLocalByCategory(categoryId: String): Flow<List<Note>> =
        local.observeNotesByCategory(categoryId)
            .map { entities -> entities.map { it.toDomain() } }

    private suspend fun fetchNotesRemoteByCategoryOnce(
        uid: String,
        categoryId: String
    ): List<Note> {
        return remote.fetchNotesByCategoryOnce(uid, categoryId)
            .map { (id, dto) -> dto.toDomain(id) }
    }

    /**
     * Writes remote data into local cache as SYNCED.
     * Pending local changes (PENDING / PENDING_DELETE) are never overwritten.
     */
    private suspend fun cacheRemoteNotesAsSynced(notes: List<Note>) {
        val pendingIds = local.getPendingNotes()
            .mapTo(mutableSetOf()) { it.id }
            .apply { addAll(local.getPendingDeletes().map { it.id }) }

        val toUpsert = notes
            .filterNot { it.id in pendingIds }
            .map { it.toEntity(syncState = SyncState.SYNCED) }

        if (toUpsert.isNotEmpty()) {
            local.saveNotes(toUpsert)
        }
    }
}
