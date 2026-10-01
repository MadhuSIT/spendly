package com.madhusit.spendly.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.madhusit.spendly.domain.FoundationRepository
import com.madhusit.spendly.presentation.components.FoundationHome
import com.madhusit.spendly.presentation.components.PlaceholderScreen

private const val HOME = "home"
private const val TRANSACTIONS = "transactions"
private const val ACCOUNTS = "accounts"
private const val INSIGHTS = "insights"

@androidx.compose.runtime.Composable
fun SpendlyApp(repository: FoundationRepository) {
    val navController = rememberNavController()
    val vm: FoundationViewModel = viewModel(factory = FoundationViewModel.factory(repository))
    val state by vm.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.initialize() }

    Scaffold(
        bottomBar = {
            com.madhusit.spendly.presentation.components.BottomNav(navController)
        },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        NavHost(navController, startDestination = HOME, modifier = Modifier) {
            composable(HOME) { FoundationHome(padding, state, vm::initialize) }
            composable(TRANSACTIONS) { PlaceholderScreen("Transactions", padding) }
            composable(ACCOUNTS) { PlaceholderScreen("Accounts", padding) }
            composable(INSIGHTS) { PlaceholderScreen("Insights", padding) }
        }
    }
}
