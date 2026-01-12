package com.ar.domain.category.repository

import com.ar.core.data.FetchStrategy
import com.ar.domain.category.model.Category
import com.ar.core.result.Result
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {

    fun observeCategories(
        strategy: FetchStrategy = FetchStrategy.FAST
    ): Flow<Result<List<Category>>>


    suspend fun getCategories(): Result<List<Category>>

    suspend fun getCategoryById(id: String): Result<Category>

    suspend fun createCategory(
        name: String,
        imageUrl: String? = null,
        colorHex: String? = null,
        order: Int = 0
    ): Result<Category>

    suspend fun updateCategory(
        id: String,
        name: String,
        imageUrl: String?,
        colorHex: String?,
        order: Int
    ): Result<Category>

    suspend fun refreshCategories(): Result<Unit>

    suspend fun deleteCategory(id: String): Result<Unit>
}