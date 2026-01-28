package com.ar.studyapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
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

// Light mode renkleri
private val LightColors = lightColorScheme(
    primary = Color(0xFF2962FF),
    onPrimary = Color.White,

    secondary = Color(0xFF03DAC6),
    onSecondary = Color.Black,

    background = Color(0xFFFDFDFD),
    onBackground = Color(0xFF1A1A1A),

    surface = Color.White,
    onSurface = Color.Black,

    error = Color(0xFFD32F2F),
    onError = Color.White
)

// Dark mode renkleri
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


/**
 * ----------------------------------------------------
 *   MAIN THEME WRAPPER
 * ----------------------------------------------------
 */

@Composable
fun Theme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val colorScheme = if (isDark) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

