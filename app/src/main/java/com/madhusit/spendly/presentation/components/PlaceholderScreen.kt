package com.madhusit.spendly.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun PlaceholderScreen(title: String, padding: PaddingValues) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp)
            .testTag("screen_${title.lowercase()}")
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text("Foundation shell ready for the next product slice.", style = MaterialTheme.typography.bodyLarge)
    }
}