package com.ar.domain.category.usecase

import com.ar.core.data.FetchStrategy
import com.ar.core.result.Result
import com.ar.domain.category.model.Category
import com.ar.domain.category.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveCategoriesUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    operator fun invoke(
        strategy: FetchStrategy = FetchStrategy.FAST
    ): Flow<Result<List<Category>>> {
        return repository.observeCategories(strategy)
    }
}