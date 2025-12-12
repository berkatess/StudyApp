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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ar.domain.category.model.Category
import com.ar.studyapp.theme.CategoryColors
import com.ar.studyapp.theme.toHexString

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
        onSubmitCategory = viewModel::onSubmitCategory
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
    onSubmitCategory: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Categories") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {

            // 🔹 FORM BÖLÜMÜ
            Text(
                text = "Create new category",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = formState.name,
                onValueChange = onNameChange,
                label = { Text("Category name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = formState.imageUrl,
                onValueChange = onImageUrlChange,
                label = { Text("Image URL (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

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

            Button(
                onClick = onSubmitCategory,
                enabled = !formState.isSubmitting,
                modifier = Modifier.align(Alignment.End)
            ) {
                if (formState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving...")
                } else {
                    Text("Create")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 🔹 VAR OLAN KATEGORİLER LİSTESİ
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
                        style = MaterialTheme.typography.bodyMedium
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
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.categories, key = { it.id }) { category ->
                                CategoryListItem(category = category)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renk seçeneklerini gösteren yatay satır.
 * Kullanıcı bir renge tıklayınca seçili hale gelir.
 */
@Composable
fun ColorPickerRow(
    selectedColorHex: String?,
    onColorSelectedHex: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
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
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
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

/**
 * Basit kategori satırı:
 * - Sol tarafta renk indikatörü
 * - Sağda kategori adı ve varsa imageUrl
 */
@Composable
fun CategoryListItem(
    category: Category
) {
    val color = category.colorHex?.let {
        try {
            Color(android.graphics.Color.parseColor(it))
        } catch (e: Exception) {
            MaterialTheme.colorScheme.secondary
        }
    } ?: MaterialTheme.colorScheme.secondary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(color)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!category.imageUrl.isNullOrBlank()) {
                Text(
                    text = category.imageUrl!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
