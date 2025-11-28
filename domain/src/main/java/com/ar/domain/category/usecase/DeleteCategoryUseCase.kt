package com.ar.domain.category.usecase


import com.ar.core.result.Result
import com.ar.core.usecase.BaseUseCase
import com.ar.domain.category.repository.CategoryRepository

class DeleteCategoryUseCase(
    private val repository: CategoryRepository
) : BaseUseCase<String, Unit>() {

    override suspend fun execute(params: String): Result<Unit> {
        if (params.isBlank()) {
            return Result.Error("Category id cannot be empty")
        }
        return repository.deleteCategory(params)
    }
}