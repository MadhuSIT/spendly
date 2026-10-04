package com.madhusit.spendly.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val tealPrimary = Color(0xFF1A6B4A)
private val tealOnPrimary = Color(0xFFFFFFFF)
private val tealPrimaryContainer = Color(0xFFB7F0D4)
private val tealOnPrimaryContainer = Color(0xFF002114)
private val tealSecondary = Color(0xFF4E6358)
private val tealBackground = Color(0xFFF5FAF6)
private val tealSurface = Color(0xFFF5FAF6)

private val LightColors = lightColorScheme(
    primary = tealPrimary,
    onPrimary = tealOnPrimary,
    primaryContainer = tealPrimaryContainer,
    onPrimaryContainer = tealOnPrimaryContainer,
    secondary = tealSecondary,
    background = tealBackground,
    surface = tealSurface,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CD4B8),
    onPrimary = Color(0xFF003826),
    primaryContainer = Color(0xFF005138),
    onPrimaryContainer = Color(0xFFB7F0D4),
    secondary = Color(0xFFB3CCBE),
)

@Composable
fun SpendlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
