package com.ar.domain.category.usecase

import com.ar.core.result.Result
import com.ar.core.usecase.BaseUseCase
import com.ar.domain.category.model.Category
import com.ar.domain.category.repository.CategoryRepository

class GetCategoriesUseCase(
    private val repository: CategoryRepository
) : BaseUseCase<Unit, List<Category>>() {

    override suspend fun execute(params: Unit): Result<List<Category>> {
        return repository.getCategories()
    }
}