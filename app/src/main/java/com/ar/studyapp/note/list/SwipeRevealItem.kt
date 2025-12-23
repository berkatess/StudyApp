package com.ar.studyapp.note.list

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SwipeRevealItem(
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val actionWidth = 72.dp
    val density = LocalDensity.current
    val actionWidthPx = with(density) { actionWidth.toPx() }

    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope() // ✅ EKLENDİ

    Box(modifier = modifier.fillMaxWidth()) {

        // Arka katman
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.CenterEnd
        ) {
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.width(actionWidth)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Ön katman
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            scope.launch {
                                val newOffset =
                                    (offsetX.value + dragAmount)
                                        .coerceIn(-actionWidthPx, 0f)
                                offsetX.snapTo(newOffset)
                            }
                        },
                        onDragEnd = {
                            scope.launch { // ✅ BURASI
                                val shouldOpen =
                                    offsetX.value < -actionWidthPx * 0.3f
                                offsetX.animateTo(
                                    if (shouldOpen) -actionWidthPx else 0f,
                                    animationSpec = tween(180)
                                )
                            }
                        }
                    )
                }
        ) {
            content()
        }
    }
}
