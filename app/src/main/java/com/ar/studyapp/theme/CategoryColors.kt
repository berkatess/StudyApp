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
