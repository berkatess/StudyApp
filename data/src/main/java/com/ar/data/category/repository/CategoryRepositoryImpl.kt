package com.ar.data.category.repository

import com.ar.core.coroutines.DispatcherProvider
import com.ar.core.network.NetworkMonitor
import com.ar.core.result.Result
import com.ar.core.sync.CategorySyncScheduler
import com.ar.data.category.local.CategoryLocalDataSource
import com.ar.data.category.mapper.toDomain
import com.ar.data.category.mapper.toEntity
import com.ar.data.category.mapper.toRemoteDto
import com.ar.data.category.remote.CategoryRemoteDataSource
import com.ar.data.sync.SyncState
import com.ar.domain.category.model.Category
import com.ar.domain.category.repository.CategoryRepository
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

    override fun observeCategories(): Flow<Result<List<Category>>> = flow {
        emit(Result.Loading)
        local.observeCategories().collect { entities ->
            emit(Result.Success(entities.map { it.toDomain() }))
        }
    }

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