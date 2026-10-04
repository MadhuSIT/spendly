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
    secondaryContainer = Color(0xFFD8E6FF),
    onSecondaryContainer = Color(0xFF00204D),
    background = Color(0xFFF2EDE8),       // warm sand
    onBackground = Color(0xFF1C1B1A),
    surface = Color(0xFFFFFFFF),           // pure white cards
    onSurface = Color(0xFF1C1B1A),
    surfaceVariant = Color(0xFFEDE8E3),    // warm tint for chips/inputs
    onSurfaceVariant = Color(0xFF4A4744),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF5F0EB),
    surfaceContainerHighest = Color(0xFFEDE8E3),
    outline = Color(0xFFB8B0A8),
    outlineVariant = Color(0xFFDDD8D3),
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
