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
fun FoundationHome(padding: PaddingValues, state: FoundationUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp)
            .testTag("screen_home"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Spendly", style = MaterialTheme.typography.headlineLarge)
        if (state.loading) CircularProgressIndicator()
        else {
            Text(state.message ?: "Ready", style = MaterialTheme.typography.bodyLarge)
            Button(onClick = {}, enabled = false) { Text("Foundation verified") }
        }
    }
}