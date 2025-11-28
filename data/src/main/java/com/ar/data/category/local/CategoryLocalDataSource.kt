package com.ar.data.category.local

class CategoryLocalDataSource(
    private val dao: CategoryDao
) {

    suspend fun getCategories(): List<CategoryEntity> = dao.getCategories()

    suspend fun getCategoryById(id: String): CategoryEntity? = dao.getCategoryById(id)

    suspend fun saveCategories(categories: List<CategoryEntity>) =
        dao.insertCategories(categories)

    suspend fun saveCategory(category: CategoryEntity) =
        dao.insertCategory(category)

    suspend fun deleteCategory(id: String) =
        dao.deleteCategory(id)
}