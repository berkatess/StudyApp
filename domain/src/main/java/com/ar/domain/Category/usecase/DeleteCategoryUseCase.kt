package com.ar.domain.Category.usecase


import com.ar.core.result.Result
import com.ar.core.usecase.BaseUseCase
import com.ar.domain.Category.repository.CategoryRepository

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