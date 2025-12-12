package com.ar.data.category.repository

import com.ar.core.coroutines.DispatcherProvider
import com.ar.core.result.Result
import com.ar.data.category.local.CategoryLocalDataSource
import com.ar.data.category.mapper.toDomain
import com.ar.data.category.mapper.toEntity
import com.ar.data.category.mapper.toRemoteDto
import com.ar.data.category.remote.CategoryRemoteDataSource
import com.ar.domain.category.model.Category
import com.ar.domain.category.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val remote: CategoryRemoteDataSource,
    private val local: CategoryLocalDataSource,
    private val dispatchers: DispatcherProvider
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

    override fun observeCategories(): Flow<Result<List<Category>>> =
        remote.observeCategories()
            .map { list ->
                val domain = list.map { (id, dto) -> dto.toDomain(id) }
                Result.Success(domain) as Result<List<Category>>
            }
            .onStart {
                emit(Result.Loading)
            }
            .catch { e ->
                emit(Result.Error("Failed to load categories", e))
            }
            .flowOn(dispatchers.io)


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
    ): Result<Category> =
        try {
            val tempDomain = Category(
                id = "", // Firestore
                name = name,
                imageUrl = imageUrl,
                colorHex = colorHex,
                order = order
            )

            val dto = tempDomain.toRemoteDto()
            val (id, savedDto) = remote.createCategory(dto)
            Result.Success(savedDto.toDomain(id))
        } catch (e: Exception) {
            Result.Error("Failed to create category", e)
        }

    override suspend fun updateCategory(category: Category): Result<Category> =
        withContext(dispatchers.io) {
            return@withContext try {
                val dto = category.toRemoteDto()
                val (id, updatedDto) = remote.updateCategory(category.id, dto)
                val updatedCategory = updatedDto.toDomain(id)

                local.saveCategory(updatedCategory.toEntity())

                Result.Success(updatedCategory)
            } catch (e: Exception) {
                Result.Error("Failed to update category", e)
            }
        }

    override suspend fun deleteCategory(id: String): Result<Unit> =
        withContext(dispatchers.io) {
            return@withContext try {
                remote.deleteCategory(id)
                local.deleteCategory(id)
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error("Failed to delete category", e)
            }
        }
}