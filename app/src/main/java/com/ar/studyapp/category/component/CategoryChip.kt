package com.ar.studyapp.note.list.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ar.domain.category.model.Category
import androidx.core.graphics.toColorInt
import com.ar.studyapp.theme.toComposeColor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryChip(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null
) {
    val baseColor = category.colorHex?.let { hex ->
        try {
            Color(hex.toColorInt())
        } catch (e: Exception) {
            MaterialTheme.colorScheme.secondary
        }
    } ?: MaterialTheme.colorScheme.secondary

    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = if (isSelected) 4.dp else 0.dp,
        modifier = modifier
            .padding(horizontal = 4.dp)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else category.colorHex.toComposeColor(),
                shape = RoundedCornerShape(16.dp)
            )
            // Use combinedClickable so callers can optionally provide a long-press action (e.g., overflow menu).
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Text(
            text = category.name,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = category.colorHex.toComposeColor(),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
