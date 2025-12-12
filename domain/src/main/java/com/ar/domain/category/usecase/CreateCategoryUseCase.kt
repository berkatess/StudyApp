package com.ar.domain.category.usecase

import com.ar.core.result.Result
import com.ar.domain.category.model.Category
import com.ar.domain.category.repository.CategoryRepository
import javax.inject.Inject

class CreateCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(
        name: String,
        imageUrl: String? = null,
        colorHex: String? = null,
        order: Int = 0
    ): Result<Category> {
        if (name.isBlank()) {
            return Result.Error("Category name cannot be empty")
        }
        return repository.createCategory(
            name = name.trim(),
            imageUrl = imageUrl,
            colorHex = colorHex,
            order = order
        )
    }
}
