package com.ar.data.category.local

import androidx.room.*
import com.ar.data.sync.SyncState
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE isDeleted = 0 ORDER BY `order` ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE isDeleted = 0 ORDER BY `order` ASC")
    suspend fun getCategories(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("UPDATE categories SET isDeleted = 1, syncState = :state WHERE id = :id")
    suspend fun markDeleted(id: String, state: SyncState = SyncState.PENDING_DELETE)

    @Query("SELECT * FROM categories WHERE syncState = :state")
    suspend fun getBySyncState(state: SyncState): List<CategoryEntity>

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun hardDelete(id: String)
}
