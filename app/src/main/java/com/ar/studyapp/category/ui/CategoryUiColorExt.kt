package com.ar.studyapp.category.ui

import androidx.compose.ui.graphics.Color
import com.ar.domain.category.model.Category

/**
 * Maps a category hex color (domain data) to a Compose Color for UI usage.
 * This stays in the presentation layer to keep domain/data layers UI-agnostic.
 */
fun Category.toComposeColorOrNull(): Color? {
    val hex = colorHex?.takeIf { it.isNotBlank() } ?: return null

    return runCatching {
        Color(android.graphics.Color.parseColor(hex))
    }.getOrNull()
}

/**
 * Same as [toComposeColorOrNull] but returns the provided fallback when parsing fails.
 */
fun Category.toComposeColorOrFallback(fallback: Color): Color {
    return toComposeColorOrNull() ?: fallback
}