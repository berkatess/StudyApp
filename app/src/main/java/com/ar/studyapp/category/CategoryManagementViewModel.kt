package com.ar.studyapp.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ar.core.data.FetchStrategy
import com.ar.core.network.NetworkMonitor
import com.ar.core.result.Result
import com.ar.domain.category.model.Category
import com.ar.domain.category.usecase.CreateCategoryUseCase
import com.ar.domain.category.usecase.DeleteCategoryUseCase
import com.ar.domain.category.usecase.ObserveCategoriesUseCase
import com.ar.domain.category.usecase.RefreshCategoriesUseCase
import com.ar.domain.category.usecase.UpdateCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import com.ar.studyapp.note.navigation.NoteDestinations
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for Category Management screen.
 *
 * Responsibilities:
 * - Observes categories (local as SSOT) with a chosen FetchStrategy
 * - Triggers a one-shot refresh when the screen opens
 * - Creates / updates / deletes categories
 * - Manages form state
 */
sealed class CategoryUiState {
    object Loading : CategoryUiState()
    data class Success(val categories: List<Category>) : CategoryUiState()
    data class Error(val message: String) : CategoryUiState()
}

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
    savedStateHandle: SavedStateHandle,
    private val observeCategoriesUseCase: ObserveCategoriesUseCase,
    private val createCategoryUseCase: CreateCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase,
    private val updateCategoryUseCase: UpdateCategoryUseCase,
    private val networkMonitor: NetworkMonitor,
    private val refreshCategoriesUseCase: RefreshCategoriesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CategoryUiState>(CategoryUiState.Loading)
    val uiState: StateFlow<CategoryUiState> = _uiState

    private val _formState = MutableStateFlow(CategoryFormState())
    val formState: StateFlow<CategoryFormState> = _formState

    // User preference (optional). Keep FAST as default.
    private val userPreferredStrategy = MutableStateFlow(FetchStrategy.FAST)

    // Connectivity state.
    private val isOnline: StateFlow<Boolean> =
        networkMonitor.isOnline
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    // Effective strategy:
    // Offline -> force CACHED (local only)
    // Online  -> use preferred (typically FAST)
    private val effectiveStrategy: StateFlow<FetchStrategy> =
        combine(userPreferredStrategy, isOnline) { preferred, online ->
            if (!online) FetchStrategy.CACHED else preferred
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FetchStrategy.FAST)

    private val pendingEditCategoryId: String? =
        savedStateHandle.get<String>(NoteDestinations.ARG_CATEGORY_ID)
            ?.takeIf { it.isNotBlank() }

    private var didHandleInitialEdit: Boolean = false

    init {
        observeCategories()
        refreshOnScreenOpen()
    }

    private fun observeCategories() {
        viewModelScope.launch {
            effectiveStrategy
                .flatMapLatest { strategy -> observeCategoriesUseCase(strategy) }
                .collectLatest { result ->
                    _uiState.value = when (result) {
                        is Result.Loading -> CategoryUiState.Loading
                        is Result.Success -> {
                            // If we navigated here with a categoryId, auto-enter edit mode once.
                            if (!didHandleInitialEdit && pendingEditCategoryId != null) {
                                result.data.firstOrNull { it.id == pendingEditCategoryId }?.let { category ->
                                    onEditCategory(category)
                                }
                                didHandleInitialEdit = true
                            }
                            CategoryUiState.Success(result.data)
                        }
                        is Result.Error -> CategoryUiState.Error(result.message ?: "Unknown error")
                    }
                }
        }
    }

    /**
     * One-shot refresh when the screen opens.
     * Safe: repository treats offline as a no-op success (or can just fail gracefully).
     */
    private fun refreshOnScreenOpen() {
        viewModelScope.launch {
            refreshCategoriesUseCase()
        }
    }

    /**
     * Optional: call this from pull-to-refresh / retry button if you have one.
     */
    fun onRefresh() {
        viewModelScope.launch {
            refreshCategoriesUseCase()
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
            imageUrl = category.imageUrl ?: "",
            selectedColorHex = category.colorHex,
            isSubmitting = false,
            errorMessage = null
        )
    }

    fun onCancelEdit() {
        _formState.value = CategoryFormState()
    }

    fun onColorSelected(hex: String) {
        _formState.value = _formState.value.copy(
            selectedColorHex = hex,
            errorMessage = null
        )
    }

    fun onSubmitCategory() {
        viewModelScope.launch {
            val current = _formState.value

            if (current.name.isBlank()) {
                _formState.value = current.copy(errorMessage = "Category name is required")
                return@launch
            }

            _formState.value = current.copy(
                isSubmitting = true,
                errorMessage = null
            )

            val result = if (current.editingCategoryId == null) {
                createCategoryUseCase(
                    name = current.name.trim(),
                    imageUrl = current.imageUrl.takeIf { it.isNotBlank() },
                    colorHex = current.selectedColorHex
                )
            } else {
                updateCategoryUseCase(
                    id = current.editingCategoryId,
                    name = current.name.trim(),
                    imageUrl = current.imageUrl.takeIf { it.isNotBlank() },
                    colorHex = current.selectedColorHex
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
