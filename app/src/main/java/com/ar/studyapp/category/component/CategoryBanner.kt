package com.ar.studyapp.category.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ar.domain.category.model.Category
import com.ar.studyapp.note.list.components.CategoryChip

@Composable
fun CategoryBanner(
    categories: List<Category>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    onAddCategoryClick: () -> Unit,
    onUpdateCategory: (Category) -> Unit,
    onDeleteCategory: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    // Holds which category's overflow menu is currently open (at most one).
    var menuForCategoryId by remember { mutableStateOf<String?>(null) }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = categories,
            key = { it.id }
        ) { category ->
            val isSelected = category.id == selectedCategoryId
            val isMenuExpanded = menuForCategoryId == category.id

            Box {
                CategoryChip(
                    category = category,
                    isSelected = isSelected,
                    onClick = { onCategorySelected(category.id) },
                    // Long-press opens the contextual actions menu.
                    onLongClick = { menuForCategoryId = category.id }
                )

                DropdownMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = { menuForCategoryId = null }
                ) {
                    DropdownMenuItem(
                        text = { Text("Update") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Update"
                            )
                        },
                        onClick = {
                            menuForCategoryId = null
                            onUpdateCategory(category)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete"
                            )
                        },
                        onClick = {
                            menuForCategoryId = null
                            onDeleteCategory(category)
                        }
                    )
                }
            }
        }

        // Add Category action as the very last item.
        item {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add category",
                modifier = Modifier
                    .size(24.dp)
                    // Disable ripple/pressed indication to keep the icon "plain".
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onAddCategoryClick() },
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
