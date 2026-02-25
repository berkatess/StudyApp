package com.ar.studyapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.ar.domain.settings.model.ThemeMode

/**
 * ----------------------------------------------------
 *  COLOR SCHEME
 * ----------------------------------------------------
 */

// LIGHT MODE
private val LightColors = lightColorScheme(
    primary = Color(0xFF2962FF),
    onPrimary = Color.White,

    secondary = Color(0xFF03DAC6),
    onSecondary = Color.Black,

    background = Color(0xFFFDFDFD),   // SABİT
    onBackground = Color(0xFF1A1A1A),

    surface = Color.White,
    onSurface = Color.Black,

    error = Color(0xFFD32F2F),
    onError = Color.White
)

// DARK MODE
private val DarkColors = darkColorScheme(
    primary = Color(0xFF82B1FF),
    onPrimary = Color.Black,

    secondary = Color(0xFF03DAC6),
    onSecondary = Color.Black,

    background = Color(0xFF121212),
    onBackground = Color(0xFFEAEAEA),

    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,

    error = Color(0xFFEF9A9A),
    onError = Color.Black
)

@Composable
fun Theme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        ThemeMode.DARK -> DarkColors
        ThemeMode.LIGHT -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}