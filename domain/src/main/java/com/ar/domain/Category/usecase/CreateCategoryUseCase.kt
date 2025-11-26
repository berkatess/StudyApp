package com.ar.domain.Category.usecase

import com.ar.core.result.Result
import com.ar.core.usecase.BaseUseCase
import com.ar.domain.Category.model.Category
import com.ar.domain.Category.repository.CategoryRepository

class CreateCategoryUseCase(
    private val repository: CategoryRepository
) : BaseUseCase<Category, Category>() {

    override suspend fun execute(params: Category): Result<Category> {
        if (params.name.isBlank()) {
            return Result.Error("Category name cannot be empty")
        }
        return repository.createCategory(params)
    }
}