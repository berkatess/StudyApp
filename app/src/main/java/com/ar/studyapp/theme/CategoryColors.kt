package com.ar.studyapp.theme

import androidx.compose.ui.graphics.Color

/**
 * Kullanıcının kategori oluştururken seçebileceği hazır renkler.
 */
object CategoryColors {

    val colors: List<Color> = listOf(
        Color(0xFFFF9800), // Orange
        Color(0xFF9C27B0), // Purple
        Color(0xFF03A9F4), // Light Blue
        Color(0xFF4CAF50), // Green
        Color(0xFFF44336), // Red
        Color(0xFF607D8B), // Blue Grey
        Color(0xFF2196F3), // Blue
        Color(0xFF00BCD4), // Cyan
        Color(0xFF009688), // Teal
        Color(0xFF8BC34A), // Light Green
        Color(0xFFCDDC39), // Lime
        Color(0xFFFFC107), // Amber
        Color(0xFFFF5722), // Deep Orange
        Color(0xFF795548), // Brown
        Color(0xFF673AB7), // Deep Purple
        Color(0xFF3F51B5), // Indigo
        Color(0xFFE91E63), // Pink
        Color(0xFF9E9E9E), // Grey
        Color(0xFFB71C1C), // Dark Red
        Color(0xFF1B5E20)  // Dark Green
    )
}

/**
 * UI katmanında Color → "#RRGGBB" string'ine çeviren extension.
 * Domain/Data bu fonksiyonu hiç bilmez.
 */
fun Color.toHexString(): String {
    val intColor = android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
    return String.format("#%06X", 0xFFFFFF and intColor)
}
