package com.madhusit.spendly.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.madhusit.spendly.presentation.FoundationUiState

@Composable
fun FoundationHome(
    padding: PaddingValues,
    state: FoundationUiState,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp)
            .testTag("screen_home"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Spendly", style = MaterialTheme.typography.headlineLarge)
        when {
            state.loading -> CircularProgressIndicator()
            state.error != null -> {
                Text(
                    "We couldn't save the local Spendly state.",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    state.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = onRetry,
                    modifier = Modifier.testTag("foundation_retry")
                ) {
                    Text("Retry")
                }
            }
            else -> {
                Text(
                    state.message ?: "Ready",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    "Your local foundation is ready.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
