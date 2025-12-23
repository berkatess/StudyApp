package com.ar.studyapp.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ar.core.result.Result
import com.ar.domain.category.model.Category
import com.ar.domain.category.usecase.CreateCategoryUseCase
import com.ar.domain.category.usecase.DeleteCategoryUseCase
import com.ar.domain.category.usecase.ObserveCategoriesUseCase
import com.ar.domain.category.usecase.UpdateCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * UI'daki kategori yönetim ekranı için ViewModel.
 * - Kategorileri realtime dinler
 * - Yeni kategori oluşturur
 */
sealed class CategoryUiState {
    object Loading : CategoryUiState()
    data class Success(val categories: List<Category>) : CategoryUiState()
    data class Error(val message: String) : CategoryUiState()
}

/**
 * Form state: kullanıcı yeni kategori oluştururken girdiği değerler.
 */
data class CategoryFormState(
    val editingCategoryId: String? = null,
    val name: String = "",
    val imageUrl: String = "",
    val selectedColorHex: String? = null,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)


@HiltViewModel
class CategoryManagementViewModel @Inject constructor(
    private val observeCategoriesUseCase: ObserveCategoriesUseCase,
    private val createCategoryUseCase: CreateCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase,
    private val updateCategoryUseCase: UpdateCategoryUseCase

) : ViewModel() {

    private val _uiState = MutableStateFlow<CategoryUiState>(CategoryUiState.Loading)
    val uiState: StateFlow<CategoryUiState> = _uiState

    private val _formState = MutableStateFlow(CategoryFormState())
    val formState: StateFlow<CategoryFormState> = _formState

    init {
        observeCategories()
    }

    private fun observeCategories() {
        viewModelScope.launch {
            observeCategoriesUseCase().collectLatest { result ->
                _uiState.value = when (result) {
                    is Result.Loading -> CategoryUiState.Loading
                    is Result.Success -> CategoryUiState.Success(result.data)
                    is Result.Error -> CategoryUiState.Error(result.message ?: "Unknown error")
                }
            }
        }
    }

    fun onNameChange(newName: String) {
        _formState.value = _formState.value.copy(
            name = newName,
            errorMessage = null
        )
    }

    fun onImageUrlChange(newUrl: String) {
        _formState.value = _formState.value.copy(
            imageUrl = newUrl,
            errorMessage = null
        )
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            val result = deleteCategoryUseCase(categoryId)
            if (result is Result.Error) {
                _formState.value = _formState.value.copy(
                    errorMessage = result.message ?: "Failed to delete category"
                )
            }
        }
    }


    fun onEditCategory(category: Category) {
        _formState.value = CategoryFormState(
            editingCategoryId = category.id,
            name = category.name,
            imageUrl = category.imageUrl.orEmpty(),
            selectedColorHex = category.colorHex,
            isSubmitting = false,
            errorMessage = null
        )
    }

    fun onCancelEdit() {
        _formState.value = CategoryFormState()
    }


    /**
     * UI'dan gelen hex string (örn "#FF9800") burada tutulur.
     * ViewModel Compose Color'ı hiç bilmez → daha temiz katman ayrımı.
     */
    fun onColorSelected(colorHex: String) {
        _formState.value = _formState.value.copy(
            selectedColorHex = colorHex,
            errorMessage = null
        )
    }

    fun onSubmitCategory() {
        val current = _formState.value

        if (current.name.isBlank()) {
            _formState.value = current.copy(
                errorMessage = "Category name cannot be empty"
            )
            return
        }

        viewModelScope.launch {
            _formState.value = current.copy(
                isSubmitting = true,
                errorMessage = null
            )

            val name = current.name
            val imageUrl = current.imageUrl.ifBlank { null }
            val colorHex = current.selectedColorHex
            val order = 0

            val result = if (current.editingCategoryId.isNullOrBlank()) {
                createCategoryUseCase(
                    name = name,
                    imageUrl = imageUrl,
                    colorHex = colorHex,
                    order = order
                )
            } else {
                updateCategoryUseCase(
                    id = current.editingCategoryId!!,
                    name = name,
                    imageUrl = imageUrl,
                    colorHex = colorHex,
                    order = order
                )
            }

            _formState.value = _formState.value.copy(isSubmitting = false)

            when (result) {
                is Result.Success -> _formState.value = CategoryFormState()
                is Result.Error -> _formState.value = _formState.value.copy(
                    errorMessage = result.message ?: "Failed to save category"
                )
                is Result.Loading -> Unit
            }
        }
    }

}
