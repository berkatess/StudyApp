package com.ar.studyapp.note.list.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ar.domain.category.model.Category
import androidx.core.graphics.toColorInt

@Composable
fun CategoryChip(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val baseColor = category.colorHex?.let { hex ->
        try {
            Color(hex.toColorInt())
        } catch (e: Exception) {
            MaterialTheme.colorScheme.secondary
        }
    } ?: MaterialTheme.colorScheme.secondary

    val backgroundColor =
        if (isSelected) baseColor else baseColor.copy(alpha = 0.4f)

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = if (isSelected) 4.dp else 0.dp,
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = category.name,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = Color.White,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
