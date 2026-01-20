package com.ar.studyapp.category.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
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

@Composable
fun CategoryFabMenu(
    categories: List<Category>,
    selectedCategoryId: String?,
    onSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    // IMPORTANT: DropdownMenu must be in the same layout scope as the FAB,
    // so it anchors to the FAB and opens upward when placed at the bottom.
    androidx.compose.foundation.layout.Box(modifier = modifier) {
        ExtendedFloatingActionButton(
            onClick = { expanded = true },
            text = { Text("Category") },
            icon = {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Category"
                )
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
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

            categories.forEach { c ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ColorDot(color = c.colorHexToColorSafe(MaterialTheme.colorScheme.outline))
                            Spacer(Modifier.width(10.dp))
                            Text(c.name)
                        }
                    },
                    onClick = {
                        onSelected(c.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

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

private fun Category.colorHexToColorSafe(fallback: Color): Color {
    return try {
        Color(android.graphics.Color.parseColor(this.colorHex))
    } catch (e: IllegalArgumentException) {
        fallback
    }
}
