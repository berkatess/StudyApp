package com.ar.domain.category.usecase

import com.ar.core.result.Result
import com.ar.domain.category.model.Category
import com.ar.domain.category.repository.CategoryRepository
import javax.inject.Inject

class UpdateCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(
        id: String,
        name: String,
        imageUrl: String? = null,
        colorHex: String? = null,
        order: Int = 0
    ): Result<Category> {

        if (id.isBlank()) {
            return Result.Error("Category id cannot be empty")
        }

        if (name.isBlank()) {
            return Result.Error("Category name cannot be empty")
        }

        return repository.updateCategory(
            id = id,
            name = name.trim(),
            imageUrl = imageUrl,
            colorHex = colorHex,
            order = order
        )
    }
}
