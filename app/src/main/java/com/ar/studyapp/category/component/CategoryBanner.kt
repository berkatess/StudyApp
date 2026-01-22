package com.ar.studyapp.category.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ar.domain.category.model.Category
import com.ar.studyapp.note.list.components.CategoryChip

@Composable
fun CategoryBanner(
    categories: List<Category>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isSelected = category.id == selectedCategoryId
            CategoryChip(
                category = category,
                isSelected = isSelected,
                onClick = { onCategorySelected(category.id) }
            )
        }
    }
}
