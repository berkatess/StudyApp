package com.ar.domain.Category.repository

import com.ar.domain.Category.model.Category
import com.ar.core.result.Result

interface CategoryRepository {

    suspend fun getCategories(): Result<List<Category>>

    suspend fun getCategoryById(id: String): Result<Category>

    suspend fun createCategory(category: Category): Result<Category>

    suspend fun updateCategory(category: Category): Result<Category>

    suspend fun deleteCategory(id: String): Result<Unit>
}