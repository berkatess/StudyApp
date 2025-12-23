package com.ar.data.category.local

import com.ar.data.sync.SyncState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class CategoryLocalDataSource @Inject constructor(
    private val categoryDao: CategoryDao
) {
    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeCategories()

    suspend fun getCategories(): List<CategoryEntity> = categoryDao.getCategories()
    suspend fun getCategoryById(id: String): CategoryEntity? = categoryDao.getCategoryById(id)

    suspend fun saveCategories(categories: List<CategoryEntity>) =
        categoryDao.insertCategories(categories)

    suspend fun saveCategory(category: CategoryEntity) =
        categoryDao.insertCategory(category)

    suspend fun markDeleted(id: String) =
        categoryDao.markDeleted(id, SyncState.PENDING_DELETE)

    suspend fun getPendingCreates(): List<CategoryEntity> =
        categoryDao.getBySyncState(SyncState.PENDING)

    suspend fun getPendingDeletes(): List<CategoryEntity> =
        categoryDao.getBySyncState(SyncState.PENDING_DELETE)

    suspend fun markAsSynced(id: String) {
        val entity = categoryDao.getCategoryById(id) ?: return
        categoryDao.insertCategory(entity.copy(syncState = SyncState.SYNCED))
    }

    suspend fun hardDelete(id: String) = categoryDao.hardDelete(id)
}
