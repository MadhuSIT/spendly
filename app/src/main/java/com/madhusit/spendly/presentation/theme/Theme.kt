package com.madhusit.spendly.presentation.theme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
@Composable fun SpendlyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(), typography = androidx.compose.material3.Typography(), content = content)
}
