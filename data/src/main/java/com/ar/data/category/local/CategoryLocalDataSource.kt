package com.ar.data.category.local

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryLocalDataSource @Inject constructor(
    private val categoryDao: CategoryDao
) {

    suspend fun getCategories(): List<CategoryEntity> = categoryDao.getCategories()

    suspend fun getCategoryById(id: String): CategoryEntity? = categoryDao.getCategoryById(id)

    suspend fun saveCategories(categories: List<CategoryEntity>) =
        categoryDao.insertCategories(categories)

    suspend fun saveCategory(category: CategoryEntity) =
        categoryDao.insertCategory(category)

    suspend fun deleteCategory(id: String) =
        categoryDao.deleteCategory(id)
}