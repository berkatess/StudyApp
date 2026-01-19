package com.ar.studyapp.anim

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SwipeRevealItem(
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionWidth: Dp = 72.dp,
    cornerRadius: Dp = 16.dp,
    content: @Composable (shape: Shape) -> Unit
) {
    val density = LocalDensity.current
    val actionWidthPx = with(density) { actionWidth.toPx() }

    var offsetX by remember { mutableFloatStateOf(0f) } // 0 .. -actionWidthPx
    val settleAnim = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun clamp(v: Float) = v.coerceIn(-actionWidthPx, 0f)

    val revealPx by remember {
        derivedStateOf { (-offsetX).coerceIn(0f, actionWidthPx) }
    }

    val cs = MaterialTheme.colorScheme
    val parentShape = RoundedCornerShape(cornerRadius)

    // corner edge while swiping
    val contentShape: Shape by remember {
        derivedStateOf {
            if (revealPx > 0.5f) {
                RoundedCornerShape(
                    topStart = cornerRadius,
                    bottomStart = cornerRadius,
                    topEnd = 0.dp,
                    bottomEnd = 0.dp
                )
            } else parentShape
        }
    }


    val seamPx = with(density) { 1.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(parentShape)
    ) {
        // background
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    if (revealPx > 0f) {
                        val left = (size.width - revealPx - seamPx).coerceAtLeast(0f)
                        val width = revealPx + seamPx
                        drawRect(
                            color = cs.errorContainer,
                            topLeft = Offset(left, 0f),
                            size = Size(width, size.height)
                        )
                    }
                }
        )

        // delete button
        IconButton(
            onClick = onDeleteClick,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(actionWidth)
                .offset { IntOffset((actionWidthPx - revealPx).roundToInt(), 0) }
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = cs.onErrorContainer
            )
        }

        // foreground (content)
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            scope.launch { if (settleAnim.isRunning) settleAnim.stop() }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            offsetX = clamp(offsetX + dragAmount)
                        },
                        onDragEnd = {
                            val shouldOpen = offsetX < -actionWidthPx * 0.3f
                            val target = if (shouldOpen) -actionWidthPx else 0f
                            scope.launch {
                                settleAnim.snapTo(offsetX)
                                settleAnim.animateTo(target, tween(180))
                                offsetX = settleAnim.value
                            }
                        }
                    )
                }
        ) {
            // give shape to item
            content(contentShape)
        }
    }
}
