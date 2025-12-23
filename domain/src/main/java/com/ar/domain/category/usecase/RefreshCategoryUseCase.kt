package com.ar.domain.category.usecase

import com.ar.core.result.Result
import com.ar.domain.category.repository.CategoryRepository
import javax.inject.Inject

class RefreshCategoriesUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.refreshCategories()
}
