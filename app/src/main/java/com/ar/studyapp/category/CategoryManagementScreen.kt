package com.ar.studyapp.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import com.ar.domain.category.model.Category
import com.ar.studyapp.theme.CategoryColors
import com.ar.studyapp.theme.toHexString
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.window.Dialog

/**
 * Route katmanı:
 * - ViewModel'i Hilt ile alır
 * - StateFlow'ları collect eder
 * - Saf UI fonksiyonuna state + callback'leri taşır
 */
@Composable
fun CategoryManagementRoute(
    onBackClick: () -> Unit,
    viewModel: CategoryManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val formState by viewModel.formState.collectAsState()

    CategoryManagementScreen(
        uiState = uiState,
        formState = formState,
        onBackClick = onBackClick,
        onNameChange = viewModel::onNameChange,
        onImageUrlChange = viewModel::onImageUrlChange,
        onColorSelectedHex = viewModel::onColorSelected,
        onSubmitCategory = viewModel::onSubmitCategory,
        onCancelEdit = viewModel::onCancelEdit
    )
}

/**
 * NEW: Dialog route layer:
 * - Obtains the ViewModel via Hilt
 * - Collects state flows
 * - Renders the dialog container
 *
 * English note: This avoids navigation for category management while keeping
 * the feature self-contained and testable.
 */
@Composable
fun CategoryManagementDialogRoute(
    onDismiss: () -> Unit,
    viewModel: CategoryManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val formState by viewModel.formState.collectAsState()

    CategoryManagementDialog(
        uiState = uiState,
        formState = formState,
        onDismiss = {
            // English note: Reset the form on close to prevent stale edit state.
            viewModel.onDialogDismiss()
            onDismiss()
        },
        onNameChange = viewModel::onNameChange,
        onImageUrlChange = viewModel::onImageUrlChange,
        onColorSelectedHex = viewModel::onColorSelected,
        onSubmitCategory = viewModel::onSubmitCategory,
        onCancelEdit = viewModel::onCancelEdit
    )
}

/**
 * Saf UI:
 * - Ne gösterileceğine (loading, error, liste) karar verir.
 * - Form inputlarını gösterir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    uiState: CategoryUiState,
    formState: CategoryFormState,
    onBackClick: () -> Unit,
    onNameChange: (String) -> Unit,
    onImageUrlChange: (String) -> Unit,
    onColorSelectedHex: (String) -> Unit,
    onSubmitCategory: () -> Unit,
    onCancelEdit: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        CategoryManagementContent(
            uiState = uiState,
            formState = formState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            onNameChange = onNameChange,
            onImageUrlChange = onImageUrlChange,
            onColorSelectedHex = onColorSelectedHex,
            onSubmitCategory = onSubmitCategory,
            onCancelEdit = onCancelEdit,
            header = null // screen header already handled by TopAppBar
        )
    }
}

/**
 * NEW: Dialog container composable
 * - Renders a compact popup version of the same content.
 *
 * English note: The content is shared with the full screen to avoid duplication
 * and keep UI behavior consistent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementDialog(
    uiState: CategoryUiState,
    formState: CategoryFormState,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onImageUrlChange: (String) -> Unit,
    onColorSelectedHex: (String) -> Unit,
    onSubmitCategory: () -> Unit,
    onCancelEdit: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            modifier = Modifier
                .widthIn(min = 280.dp, max = 520.dp)
                .heightIn(max = 640.dp)
                .padding(16.dp)
        ) {
            // Title row for dialog
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (formState.editingCategoryId.isNullOrBlank()) "Create new category" else "Edit category",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                Divider()

                CategoryManagementContent(
                    uiState = uiState,
                    formState = formState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onNameChange = onNameChange,
                    onImageUrlChange = onImageUrlChange,
                    onColorSelectedHex = onColorSelectedHex,
                    onSubmitCategory = {
                        onSubmitCategory()
                        // English note: Close dialog after submit attempt is handled at caller side if desired.
                        // We do not auto-close here to keep business flow predictable.
                    },
                    onCancelEdit = onCancelEdit,
                    header = null
                )
            }
        }
    }
}

/**
 * NEW: Shared content composable used by both full-screen and dialog versions.
 *
 * English note: Splitting container (Scaffold/Dialog) from content helps follow
 * SRP and makes UI reusable (dialog, bottom sheet, screen).
 */
@Composable
private fun CategoryManagementContent(
    uiState: CategoryUiState,
    formState: CategoryFormState,
    modifier: Modifier,
    onNameChange: (String) -> Unit,
    onImageUrlChange: (String) -> Unit,
    onColorSelectedHex: (String) -> Unit,
    onSubmitCategory: () -> Unit,
    onCancelEdit: () -> Unit,
    header: (@Composable (() -> Unit))?
) {
    Column(
        modifier = modifier
    ) {
        header?.invoke()

//        Text(
//            text = if (formState.editingCategoryId.isNullOrBlank()) "Create new category" else "Edit category",
//            style = MaterialTheme.typography.titleMedium
//        )
//        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = formState.name,
            onValueChange = onNameChange,
            label = { Text("Category name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

//            Spacer(modifier = Modifier.height(8.dp))
//
//            OutlinedTextField(
//                value = formState.imageUrl,
//                onValueChange = onImageUrlChange,
//                label = { Text("Image URL (optional)") },
//                modifier = Modifier.fillMaxWidth(),
//                singleLine = true
//            )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Color",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        ColorPickerRow(
            selectedColorHex = formState.selectedColorHex,
            onColorSelectedHex = onColorSelectedHex
        )

        if (formState.errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formState.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // if editMode show cancel button
            if (!formState.editingCategoryId.isNullOrBlank()) {
                TextButton(onClick = onCancelEdit) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Button(
                onClick = onSubmitCategory,
                enabled = !formState.isSubmitting
            ) {
                if (formState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving...")
                } else {
                    Text(if (formState.editingCategoryId.isNullOrBlank()) "Create" else "Update")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // CategoryList
        Text(
            text = "Existing categories",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        when (uiState) {
            is CategoryUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            is CategoryUiState.Error -> {
                Text(
                    text = uiState.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            is CategoryUiState.Success -> {
                if (uiState.categories.isEmpty()) {
                    Text(
                        text = "No categories yet",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.categories, key = { it.id }) { category ->
                            CategoryListItem(
                                category = category
                            )
                        }

                        // English note: Give dialog a bit of breathing room at the bottom.
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerRow(
    selectedColorHex: String?,
    onColorSelectedHex: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CategoryColors.colors.forEach { color ->
            val colorHex = color.toHexString()
            val isSelected = selectedColorHex == colorHex

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        shape = CircleShape
                    )
                    .clickable { onColorSelectedHex(colorHex) }
                    .padding(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}

@Composable
fun CategoryListItem(
    category: Category
) {
    val color = category.colorHex?.let {
        try {
            Color(it.toColorInt())
        } catch (e: Exception) {
            MaterialTheme.colorScheme.secondary
        }
    } ?: MaterialTheme.colorScheme.secondary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = CircleShape
                )
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
//            if (!category.imageUrl.isNullOrBlank()) {
//                Text(
//                    text = category.imageUrl!!,
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant,
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis
//                )
//            }
        }
    }
}