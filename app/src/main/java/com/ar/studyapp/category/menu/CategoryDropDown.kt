package com.ar.studyapp.category.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ar.domain.category.model.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(
    categories: List<Category>,
    selectedCategoryId: String?,
    onSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    // Find currently selected category
    val selectedCategory = remember(categories, selectedCategoryId) {
        categories.firstOrNull { it.id == selectedCategoryId }
    }

    val selectedName = selectedCategory?.name ?: "Select Category"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            leadingIcon = {
                // Show color dot for selected category
                selectedCategory?.let { category ->
                    ColorDot(color = category.colorHexToColor())
                }
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // "No category" option
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ColorDot(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.width(10.dp))
                        Text("No Category")
                    }
                },
                onClick = {
                    onSelected(null)
                    expanded = false
                }
            )

            // Category list
            categories.forEach { category ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ColorDot(color = category.colorHexToColor())
                            Spacer(Modifier.width(10.dp))
                            Text(category.name)
                        }
                    },
                    onClick = {
                        onSelected(category.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

/* ------------------------------------------------------- */
/* UI helpers                                              */
/* ------------------------------------------------------- */

@Composable
private fun ColorDot(
    color: Color,
    size: Dp = 10.dp
) {
    Spacer(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
    )
}

/**
 * Converts a domain-level hex color string ("#RRGGBB") into a Compose Color.
 * Domain and data layers are intentionally unaware of Compose Color.
 */
@Composable
private fun Category.colorHexToColor(): Color {
    return try {
        Color(android.graphics.Color.parseColor(this.colorHex))
    } catch (e: IllegalArgumentException) {
        // Fallback color in case of an invalid hex string
        MaterialTheme.colorScheme.outline
    }
}
