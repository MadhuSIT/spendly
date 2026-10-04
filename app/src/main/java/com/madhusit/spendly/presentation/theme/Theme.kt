package com.madhusit.spendly.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B3A6B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = Color(0xFF001849),
    secondary = Color(0xFF4A6FA5),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDE5F4),
    onSecondaryContainer = Color(0xFF1B3A6B),
    background = Color(0xFFF4F7FD),        // very light blue-white
    onBackground = Color(0xFF0F1C35),
    surface = Color(0xFFF8FAFF),           // soft off-white cards, no harsh edge
    onSurface = Color(0xFF0F1C35),
    surfaceVariant = Color(0xFFDDE5F4),
    onSurfaceVariant = Color(0xFF3A4A6A),
    surfaceContainerLowest = Color(0xFFF8FAFF),
    surfaceContainerLow = Color(0xFFF8FAFF),
    surfaceContainer = Color(0xFFF8FAFF),
    surfaceContainerHigh = Color(0xFFEEF3FB),
    surfaceContainerHighest = Color(0xFFE6EDF8),
    errorContainer = Color(0xFFFDE8E6),    // softer rose, less aggressive
    onErrorContainer = Color(0xFF6B1A18),
    outline = Color(0xFFABBAD6),
    outlineVariant = Color(0xFFD0DCF0),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFADC6FF),
    onPrimary = Color(0xFF002A78),
    primaryContainer = Color(0xFF003EA8),
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = Color(0xFFB5C4FF),
    onSecondary = Color(0xFF00258B),
    secondaryContainer = Color(0xFF1B3FC3),
    onSecondaryContainer = Color(0xFFDAE2FF),
    background = Color(0xFF1A1C23),
    onBackground = Color(0xFFE3E5EE),
    surface = Color(0xFF1A1C23),
    onSurface = Color(0xFFE3E5EE),
    surfaceVariant = Color(0xFF44474F),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099),
)

@Composable
fun SpendlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
