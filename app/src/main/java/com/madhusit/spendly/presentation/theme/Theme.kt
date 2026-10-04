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
    background = Color(0xFFEBF0FA),        // cool blue-gray canvas
    onBackground = Color(0xFF0F1C35),
    surface = Color(0xFFFFFFFF),           // white cards lift off bg
    onSurface = Color(0xFF0F1C35),
    surfaceVariant = Color(0xFFDDE5F4),    // same family as button
    onSurfaceVariant = Color(0xFF3A4A6A),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF0F4FB),
    surfaceContainerHighest = Color(0xFFE6EDF8),
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
