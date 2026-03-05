package com.ar.domain.category.usecase

import com.ar.core.result.Result
import com.ar.core.usecase.BaseUseCase
import com.ar.domain.category.model.Category
import com.ar.domain.category.repository.CategoryRepository
import com.ar.domain.error.CategoryError

class GetCategoryByIdUseCase(
    private val repository: CategoryRepository
) : BaseUseCase<String, Category>() {

    override suspend fun execute(params: String): Result<Category> {
        if (params.isBlank()) {
           return Result.Error(error = CategoryError.EmptyCategoryName)
        }
        return repository.getCategoryById(params)
    }
}