package com.ar.data.category.repository

import com.ar.core.coroutines.DispatcherProvider
import com.ar.core.network.NetworkMonitor
import com.ar.core.result.Result
import com.ar.core.data.FetchStrategy
import com.ar.core.sync.CategorySyncScheduler
import com.ar.data.category.local.CategoryLocalDataSource
import com.ar.data.category.mapper.toDomain
import com.ar.data.category.mapper.toEntity
import com.ar.data.category.mapper.toRemoteDto
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val remote: CategoryRemoteDataSource,
    private val local: CategoryLocalDataSource,
    private val dispatchers: DispatcherProvider,
    private val categorySyncScheduler: CategorySyncScheduler,
    private val authRepository: AuthRepository,
    private val networkMonitor: NetworkMonitor
) : CategoryRepository {

    override suspend fun getCategories(): Result<List<Category>> =
        withContext(dispatchers.io) {
            try {
                //remote
                val remoteData = remote.getCategories()
                val domainCategories = remoteData.map { (id, dto) -> dto.toDomain(id) }

                //save to local cache
                local.saveCategories(domainCategories.map { it.toEntity() })

                Result.Success(domainCategories)
            } catch (e: Exception) {
                //remote failed try local
                val localEntities = local.getCategories()
                val localDomain = localEntities.map { it.toDomain() }

                if (localDomain.isNotEmpty()) {
                    Result.Success(localDomain)
                } else {
                    Result.Error("Failed to load categories", e)
                }
            }
        }

    override fun observeCategories(
        strategy: FetchStrategy
    ): Flow<Result<List<Category>>> = when (strategy) {
        FetchStrategy.CACHED   -> observeCategoriesCached()
        FetchStrategy.FRESH    -> observeCategoriesFresh(saveToLocal = true)
        FetchStrategy.FALLBACK -> observeCategoriesFallback(saveToLocal = true)
        FetchStrategy.FAST     -> observeCategoriesFast()
        FetchStrategy.SYNCED   -> observeCategoriesSynced()
    }

    private fun observeCategoriesFresh(saveToLocal: Boolean): Flow<Result<List<Category>>> =
        remote.observeCategories()
            .map<List<Pair<String, com.ar.data.category.remote.CategoryRemoteDto>>, Result<List<Category>>> { pairs ->
                val domain = pairs.map { (id, dto) -> dto.toDomain(id) }

                if (saveToLocal) {
                    runCatching {
                        local.saveCategories(domain.map { it.toEntity(syncState = SyncState.SYNCED) })
                    }
                }

                Result.Success(domain)
            }
            .onStart { emit(Result.Loading) }
            .catch { e -> emit(Result.Error("Failed to load categories from remote", e)) }
            .flowOn(dispatchers.io)


    private fun observeCategoriesFast(): Flow<Result<List<Category>>> = channelFlow {
        send(Result.Loading)

        val localJob = launch(dispatchers.io) {
            local.observeCategories().collect { entities ->
                send(Result.Success(entities.map { it.toDomain() }))
            }
        }

        val remoteJob = launch(dispatchers.io) {
            runCatching {
                authRepository.ensureSignedIn()

                remote.observeCategories().collect { pairs ->
                    val domain = pairs.map { (id, dto) -> dto.toDomain(id) }
                    local.saveCategories(
                        domain.map { it.toEntity(syncState = SyncState.SYNCED) }
                    )
                }
            }.onFailure { e ->
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


    private fun observeCategoriesFallback(saveToLocal: Boolean): Flow<Result<List<Category>>> = channelFlow {
        send(Result.Loading)

        val remoteOk = runCatching {
            remote.observeCategories().collect { pairs ->
                val domain = pairs.map { (id, dto) -> dto.toDomain(id) }

                if (saveToLocal) {
                    runCatching {
                        local.saveCategories(domain.map { it.toEntity(syncState = SyncState.SYNCED) })
                    }
                }

                send(Result.Success(domain))
            }
        }.isSuccess

        if (!remoteOk) {
            local.observeCategories().collect { entities ->
                send(Result.Success(entities.map { it.toDomain() }))
            }
        }
    }.catch { e ->
        emit(Result.Error("Failed to load categories (remote + local fallback)", e))
    }.flowOn(dispatchers.io)


    private fun observeCategoriesSynced(): Flow<Result<List<Category>>> = flow {
        emit(Result.Loading)

        if (networkMonitor.isOnlineNow()) {
            val remoteList = remote.fetchCategoriesOnce()
            val domain = remoteList.map { (id, dto) -> dto.toDomain(id) }
            local.saveCategories(domain.map { it.toEntity(syncState = SyncState.SYNCED) })
        }

        // local
        local.observeCategories().collect { entities ->
            emit(Result.Success(entities.map { it.toDomain() }))
        }
    }.catch { e ->
        emit(Result.Error("Failed to sync categories", e))
    }.flowOn(dispatchers.io)


    private fun observeCategoriesCached(): Flow<Result<List<Category>>> =
        local.observeCategories()
            .map<List<com.ar.data.category.local.CategoryEntity>, Result<List<Category>>> { entities ->
                Result.Success(entities.map { it.toDomain() })
            }
            .onStart { emit(Result.Loading) }
            .catch { e -> emit(Result.Error("Failed to load categories locally", e)) }
            .flowOn(dispatchers.io)


    override suspend fun refreshCategories(): Result<Unit> {
        return try {
            if (!networkMonitor.isOnlineNow()) return Result.Success(Unit)

            val remoteList = remote.fetchCategoriesOnce()
            val domain = remoteList.map { (id, dto) -> dto.toDomain(id) }

            local.saveCategories(domain.map { it.toEntity(syncState = SyncState.SYNCED) })

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to refresh categories", e)
        }
    }


    override suspend fun getCategoryById(id: String): Result<Category> =
        withContext(dispatchers.io) {
            try {
                val remoteResult = remote.getCategoryById(id)
                    ?: return@withContext Result.Error("Category not found")

                val (docId, dto) = remoteResult
                val domainCategory = dto.toDomain(docId)

                local.saveCategory(domainCategory.toEntity())

                Result.Success(domainCategory)
            } catch (e: Exception) {
                val localEntity = local.getCategoryById(id)
                    ?: return@withContext Result.Error("Category not found locally", e)

                Result.Success(localEntity.toDomain())
            }
        }

    override suspend fun createCategory(
        name: String,
        imageUrl: String?,
        colorHex: String?,
        order: Int
    ): Result<Category> {
        return try {
            val id = UUID.randomUUID().toString()

            val category = Category(
                id = id,
                name = name,
                imageUrl = imageUrl,
                colorHex = colorHex,
                order = order
            )

            local.saveCategory(
                category.toEntity(syncState = SyncState.PENDING)
            )

            if (networkMonitor.isOnlineNow()) {
                runCatching {
                    remote.createCategory(id, category.toRemoteDto())
                    local.markAsSynced(id)
                }.getOrElse {
                    categorySyncScheduler.schedule()
                }
            } else {
                categorySyncScheduler.schedule()
            }

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
    ): Result<Category> {
        return try {
            val category = Category(
                id = id,
                name = name,
                imageUrl = imageUrl,
                colorHex = colorHex,
                order = order
            )

            local.saveCategory(
                category.toEntity(syncState = SyncState.PENDING)
            )

            if (networkMonitor.isOnlineNow()) {
                runCatching {
                    remote.updateCategory(id, category.toRemoteDto())
                    local.markAsSynced(id)
                }.getOrElse {
                    categorySyncScheduler.schedule()
                }
            } else {
                categorySyncScheduler.schedule()
            }

            Result.Success(category)
        } catch (e: Exception) {
            Result.Error("Category could not be updated", e)
        }
    }


    override suspend fun deleteCategory(id: String): Result<Unit> = withContext(dispatchers.io) {
        try {
            local.markDeleted(id)          //UI
            categorySyncScheduler.schedule() //firebase deleted after room on
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Failed to delete category", e)
        }
    }



}