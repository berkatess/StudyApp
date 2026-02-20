package com.ar.data.category.repository

import com.ar.core.coroutines.DispatcherProvider
import com.ar.core.data.FetchStrategy
import com.ar.core.network.NetworkMonitor
import com.ar.core.result.Result
import com.ar.core.sync.CategorySyncScheduler
import com.ar.data.category.local.CategoryLocalDataSource
import com.ar.data.category.mapper.toDomain
import com.ar.data.category.mapper.toEntity
import com.ar.data.category.remote.CategoryRemoteDataSource
import com.ar.data.sync.SyncState
import com.ar.domain.auth.repository.AuthRepository
import com.ar.domain.category.model.Category
import com.ar.domain.category.repository.CategoryRepository
import com.google.firebase.firestore.FirebaseFirestoreException
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
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CategoryRepository implementation following:
 * - Single Source of Truth (Room)
 * - Offline-first approach
 * - Optimistic updates
 * - Explicit sync strategies
 *
 * UI always observes local database.
 * Remote is used only to refresh local cache or to perform one-shot sync.
 */
@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val local: CategoryLocalDataSource,
    private val remote: CategoryRemoteDataSource,
    private val dispatchers: DispatcherProvider,
    private val categorySyncScheduler: CategorySyncScheduler,
    private val authRepository: AuthRepository,
    private val networkMonitor: NetworkMonitor
) : CategoryRepository {

    // ------------------------------------------------
    // Public API (observe + refresh dual design)
    // ------------------------------------------------

    /**
     * Observes categories using the given fetch strategy.
     * This is the main entry point for UI layers.
     */
    override fun observeCategories(
        strategy: FetchStrategy
    ): Flow<Result<List<Category>>> = when (strategy) {
        FetchStrategy.FAST -> observeCategoriesFast()
        FetchStrategy.CACHED -> observeCategoriesCached()
        FetchStrategy.FRESH -> observeCategoriesFresh(saveToLocal = true)
        FetchStrategy.FALLBACK -> observeCategoriesFallback(saveToLocal = true)
        FetchStrategy.SYNCED -> observeCategoriesSynced()
    }

    /**
     * One-shot synchronization.
     * Fetches categories from remote and updates local cache.
     * UI updates automatically via observeCategories().
     */
    override suspend fun refreshCategories(): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            if (!networkMonitor.isOnlineNow()) return@runCatching

            val remoteDomain = fetchCategoriesRemoteOnce()
            cacheRemoteCategoriesAsSynced(remoteDomain)
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { e -> Result.Error("Failed to refresh categories", e) }
        )
    }

    override suspend fun getCategoryById(id: String): Result<Category> =
        withContext(dispatchers.io) {
            try {
                // 1) Local-first (Single Source of Truth)
                val localEntity = local.getCategoryById(id)
                if (localEntity != null) {
                    return@withContext Result.Success(localEntity.toDomain())
                }

                // 2) If offline, we cannot fetch remote
                if (!networkMonitor.isOnlineNow()) {
                    return@withContext Result.Error("Category not found locally")
                }

                // 3) Remote one-shot fetch (only if needed)
                val uid = authRepository.ensureSignedIn()

                val remotePair = remote.getCategoryById(uid, id)
                    ?: return@withContext Result.Error("Category not found")

                val (docId, dto) = remotePair
                val domain = dto.toDomain(docId)

                // 4) Do not overwrite locally pending changes
                val pendingIds = local.getPendingCreates()
                    .mapTo(mutableSetOf()) { it.id }
                    .apply { addAll(local.getPendingDeletes().map { it.id }) }

                // If this id is pending delete, don't resurrect it from remote
                if (docId in pendingIds) {
                    return@withContext Result.Success(domain)
                }

                // 5) Cache as synced
                local.saveCategory(domain.toEntity(syncState = SyncState.SYNCED))

                Result.Success(domain)
            } catch (e: Exception) {
                // Fallback attempt to local (in case of race)
                val localEntity = local.getCategoryById(id)
                if (localEntity != null) Result.Success(localEntity.toDomain())
                else Result.Error("Failed to load category", e)
            }
        }

    /**
     * One-shot getter kept for backward compatibility.
     * Prefer observeCategories() + refreshCategories() for UI.
     */
    override suspend fun getCategories(): Result<List<Category>> = withContext(dispatchers.io) {
        try {
            if (networkMonitor.isOnlineNow()) {
                val remoteDomain = fetchCategoriesRemoteOnce()
                cacheRemoteCategoriesAsSynced(remoteDomain)

                return@withContext Result.Success(remoteDomain)
            }

            Result.Success(getCategoriesLocalOnce())
        } catch (e: Exception) {
            val localDomain = getCategoriesLocalOnce()
            if (localDomain.isNotEmpty()) Result.Success(localDomain)
            else Result.Error("Failed to load categories", e)
        }
    }

    // ------------------------------------------------
    // Create / Update / Delete (optimistic updates)
    // ------------------------------------------------

    override suspend fun createCategory(
        name: String,
        imageUrl: String?,
        colorHex: String?,
        order: Int
    ): Result<Category> = withContext(dispatchers.io) {
        try {
            val category = Category(
                id = UUID.randomUUID().toString(),
                name = name,
                imageUrl = imageUrl,
                colorHex = colorHex,
                order = order
            )

            // Save locally first for immediate UI update
            local.saveCategory(category.toEntity(syncState = SyncState.PENDING))

            // Schedule remote synchronization
            categorySyncScheduler.schedule()

            Result.Success(category)
        } catch (e: Exception) {
            Result.Error("Category could not be created", e)
        }
    }

    override suspend fun updateCategory(
        id: String,
        name: String,
        imageUrl: String?,
        colorHex: String?,
        order: Int
    ): Result<Category> = withContext(dispatchers.io) {
        try {
            val category = Category(
                id = id,
                name = name,
                imageUrl = imageUrl,
                colorHex = colorHex,
                order = order
            )

            // Optimistic local update
            local.saveCategory(category.toEntity(syncState = SyncState.PENDING))

            // Schedule remote synchronization
            categorySyncScheduler.schedule()

            Result.Success(category)
        } catch (e: Exception) {
            Result.Error("Category could not be updated", e)
        }
    }

    override suspend fun deleteCategory(id: String): Result<Unit> = withContext(dispatchers.io) {
        try {
            // Mark as pending delete locally
            local.markDeleted(id)

            // Schedule remote synchronization
            categorySyncScheduler.schedule()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to delete category", e)
        }
    }

    // ------------------------------------------------
    // Single Source of Truth helpers
    // ------------------------------------------------

    /**
     * Local database stream mapped to domain models.
     */
    private fun getCategoriesLocal(): Flow<List<Category>> =
        local.observeCategories()
            .map { entities -> entities.map { it.toDomain() } }

    /**
     * One-shot local read.
     */
    private suspend fun getCategoriesLocalOnce(): List<Category> =
        local.getCategories().map { it.toDomain() }

    /**
     * Remote snapshot stream mapped to domain models.
     * Used only to refresh local cache.
     */
    private fun getCategoriesRemote(): Flow<List<Category>> = flow {
        val uid = authRepository.ensureSignedIn()
        emitAll(
            remote.observeCategories(uid)
                .map { pairs -> pairs.map { (id, dto) -> dto.toDomain(id) } }
        )
    }

    /**
     * One-shot remote fetch.
     */
    private suspend fun fetchCategoriesRemoteOnce(): List<Category> {
        val uid = authRepository.ensureSignedIn()
        return remote.fetchCategoriesOnce(uid)
            .map { (id, dto) -> dto.toDomain(id) }
    }

    /**
     * Writes remote data into local cache as SYNCED.
     * Pending local changes (PENDING / PENDING_DELETE) are never overwritten.
     *
     * This prevents "revert to old state after update" issues.
     */
    private suspend fun cacheRemoteCategoriesAsSynced(categories: List<Category>) {
        val pendingIds = local.getPendingCreates()
            .mapTo(mutableSetOf()) { it.id }
            .apply {
                addAll(local.getPendingDeletes().map { it.id })
            }

        val toUpsert = categories
            .filterNot { it.id in pendingIds }
            .map { it.toEntity(syncState = SyncState.SYNCED) }

        if (toUpsert.isNotEmpty()) {
            local.saveCategories(toUpsert)
        }
    }

    // ------------------------------------------------
    // Strategy implementations
    // ------------------------------------------------

    /**
     * CACHED:
     * Emits data only from local database.
     */
    private fun observeCategoriesCached(): Flow<Result<List<Category>>> =
        getCategoriesLocal()
            .map<List<Category>, Result<List<Category>>> { Result.Success(it) }
            .onStart { emit(Result.Loading) }
            .catch { e -> emit(Result.Error("Failed to load categories locally", e)) }
            .flowOn(dispatchers.io)

    /**
     * FAST:
     * UI observes local database.
     * Remote is used only to refresh local cache in background.
     */
    private fun observeCategoriesFast(): Flow<Result<List<Category>>> = channelFlow {
        send(Result.Loading)

        val localJob = launch(dispatchers.io) {
            getCategoriesLocal().collect { domain ->
                send(Result.Success(domain))
            }
        }

        val remoteJob = launch(dispatchers.io) {
            runCatching {
                if (!networkMonitor.isOnlineNow()) return@runCatching

                getCategoriesRemote().collect { domain ->
                    cacheRemoteCategoriesAsSynced(domain)
                }
            }.onFailure { e ->
                // Avoid UI noise on permission errors
                if (e !is FirebaseFirestoreException ||
                    e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED
                ) {
                    send(Result.Error("Failed to observe categories from remote", e))
                }
            }
        }

        awaitClose {
            localJob.cancel()
            remoteJob.cancel()
        }
    }.flowOn(dispatchers.io)

    /**
     * FRESH:
     * Emits data directly from remote stream.
     * Optionally updates local cache.
     */
    private fun observeCategoriesFresh(saveToLocal: Boolean): Flow<Result<List<Category>>> =
        getCategoriesRemote()
            .map<List<Category>, Result<List<Category>>> { domain ->
                if (saveToLocal) runCatching { cacheRemoteCategoriesAsSynced(domain) }
                Result.Success(domain)
            }
            .onStart { emit(Result.Loading) }
            .catch { e -> emit(Result.Error("Failed to load categories from remote", e)) }
            .flowOn(dispatchers.io)

    /**
     * FALLBACK:
     * Tries remote stream first.
     * Falls back to local stream on failure.
     */
    private fun observeCategoriesFallback(saveToLocal: Boolean): Flow<Result<List<Category>>> =
        channelFlow {
            send(Result.Loading)

            val remoteOk = runCatching {
                if (!networkMonitor.isOnlineNow()) throw IllegalStateException("Offline")

                getCategoriesRemote().collect { domain ->
                    if (saveToLocal) runCatching { cacheRemoteCategoriesAsSynced(domain) }
                    send(Result.Success(domain))
                }
            }.isSuccess

            if (!remoteOk) {
                getCategoriesLocal().collect { domain ->
                    send(Result.Success(domain))
                }
            }
        }.catch { e ->
            emit(Result.Error("Failed to load categories (remote + local fallback)", e))
        }.flowOn(dispatchers.io)

    /**
     * SYNCED:
     * Performs one-shot remote sync, then emits local stream.
     */
    private fun observeCategoriesSynced(): Flow<Result<List<Category>>> = flow {
        emit(Result.Loading)

        if (networkMonitor.isOnlineNow()) {
            val remoteDomain = fetchCategoriesRemoteOnce()
            cacheRemoteCategoriesAsSynced(remoteDomain)
        }

        // Local database is the single source of truth
        getCategoriesLocal().collect { domain ->
            emit(Result.Success(domain))
        }
    }.catch { e ->
        emit(Result.Error("Failed to sync categories", e))
    }.flowOn(dispatchers.io)
}
