package com.madhusit.spendly.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Seed: deep forest teal — legible on both light/dark, reads "money + trust"
private val tealPrimary       = Color(0xFF1A6B4A)
private val tealOnPrimary     = Color(0xFFFFFFFF)
private val tealPrimaryContainer = Color(0xFFB7F0D4)
private val tealOnPrimaryContainer = Color(0xFF002114)

private val tealSecondary     = Color(0xFF4D6357)
private val tealOnSecondary   = Color(0xFFFFFFFF)
private val tealSecondaryContainer = Color(0xFFCFE9D8)
private val tealOnSecondaryContainer = Color(0xFF0B1F17)

private val tealTertiary      = Color(0xFF3A6374)
private val tealOnTertiary    = Color(0xFFFFFFFF)
private val tealTertiaryContainer = Color(0xFFBEE8FB)
private val tealOnTertiaryContainer = Color(0xFF001F2A)

private val tealError         = Color(0xFFBA1A1A)
private val tealOnError       = Color(0xFFFFFFFF)
private val tealErrorContainer = Color(0xFFFFDAD6)
private val tealOnErrorContainer = Color(0xFF410002)

private val tealBackground    = Color(0xFFF5FBF6)
private val tealOnBackground  = Color(0xFF171D1A)
private val tealSurface       = Color(0xFFF5FBF6)
private val tealOnSurface     = Color(0xFF171D1A)
private val tealSurfaceVariant = Color(0xFFDBE5DE)
private val tealOnSurfaceVariant = Color(0xFF404944)
private val tealOutline       = Color(0xFF707973)

private val LightColors = lightColorScheme(
    primary = tealPrimary,
    onPrimary = tealOnPrimary,
    primaryContainer = tealPrimaryContainer,
    onPrimaryContainer = tealOnPrimaryContainer,
    secondary = tealSecondary,
    onSecondary = tealOnSecondary,
    secondaryContainer = tealSecondaryContainer,
    onSecondaryContainer = tealOnSecondaryContainer,
    tertiary = tealTertiary,
    onTertiary = tealOnTertiary,
    tertiaryContainer = tealTertiaryContainer,
    onTertiaryContainer = tealOnTertiaryContainer,
    error = tealError,
    onError = tealOnError,
    errorContainer = tealErrorContainer,
    onErrorContainer = tealOnErrorContainer,
    background = tealBackground,
    onBackground = tealOnBackground,
    surface = tealSurface,
    onSurface = tealOnSurface,
    surfaceVariant = tealSurfaceVariant,
    onSurfaceVariant = tealOnSurfaceVariant,
    outline = tealOutline,
)

// Dark palette — derived from the same teal seed
private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CD4B8),
    onPrimary = Color(0xFF003824),
    primaryContainer = Color(0xFF005236),
    onPrimaryContainer = Color(0xFFB7F0D4),
    secondary = Color(0xFFB4CCBC),
    onSecondary = Color(0xFF20352B),
    secondaryContainer = Color(0xFF364B40),
    onSecondaryContainer = Color(0xFFCFE9D8),
    tertiary = Color(0xFFA2CCDE),
    onTertiary = Color(0xFF033544),
    tertiaryContainer = Color(0xFF1F4C5C),
    onTertiaryContainer = Color(0xFFBEE8FB),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F1511),
    onBackground = Color(0xFFDEE4DF),
    surface = Color(0xFF0F1511),
    onSurface = Color(0xFFDEE4DF),
    surfaceVariant = Color(0xFF404944),
    onSurfaceVariant = Color(0xFFBFC9C2),
    outline = Color(0xFF8A938D),
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
