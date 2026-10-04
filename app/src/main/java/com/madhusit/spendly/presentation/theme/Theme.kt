package com.madhusit.spendly.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B3A6B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = Color(0xFF001849),
    secondary = Color(0xFF4A6FA5),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDE5F4),
    onSecondaryContainer = Color(0xFF1B3A6B),
    background = Color(0xFFF9FBFF),        // near-white with the faintest blue
    onBackground = Color(0xFF0F1C35),
    surface = Color(0xFFFFFFFF),           // pure white cards, clear lift
    onSurface = Color(0xFF0F1C35),
    surfaceVariant = Color(0xFFDDE5F4),
    onSurfaceVariant = Color(0xFF3A4A6A),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
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

private val SpendlyTypography = Typography(
    // Headlines — tight tracking, heavier weight
    headlineLarge  = TextStyle(fontWeight = FontWeight.W700, fontSize = 32.sp, letterSpacing = (-0.5).sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.W700, fontSize = 28.sp, letterSpacing = (-0.5).sp, lineHeight = 36.sp),
    headlineSmall  = TextStyle(fontWeight = FontWeight.W600, fontSize = 24.sp, letterSpacing = (-0.25).sp, lineHeight = 32.sp),
    // Titles
    titleLarge  = TextStyle(fontWeight = FontWeight.W600, fontSize = 20.sp, letterSpacing = (-0.1).sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.W600, fontSize = 16.sp, letterSpacing = 0.sp,     lineHeight = 24.sp),
    titleSmall  = TextStyle(fontWeight = FontWeight.W500, fontSize = 14.sp, letterSpacing = 0.1.sp,   lineHeight = 20.sp),
    // Body
    bodyLarge  = TextStyle(fontWeight = FontWeight.W400, fontSize = 16.sp, letterSpacing = 0.15.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.W400, fontSize = 14.sp, letterSpacing = 0.1.sp,  lineHeight = 20.sp),
    bodySmall  = TextStyle(fontWeight = FontWeight.W400, fontSize = 12.sp, letterSpacing = 0.2.sp,  lineHeight = 16.sp),
    // Labels — used for section caps + chips
    labelLarge  = TextStyle(fontWeight = FontWeight.W500, fontSize = 14.sp, letterSpacing = 0.1.sp,  lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.W500, fontSize = 12.sp, letterSpacing = 0.4.sp,  lineHeight = 16.sp),
    labelSmall  = TextStyle(fontWeight = FontWeight.W600, fontSize = 11.sp, letterSpacing = 0.8.sp,  lineHeight = 16.sp),
)

@Composable
fun SpendlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = SpendlyTypography,
        content = content
    )
}
