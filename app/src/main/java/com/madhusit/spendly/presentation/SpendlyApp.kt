package com.madhusit.spendly.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.madhusit.spendly.domain.FoundationRepository
import com.madhusit.spendly.domain.ledger.LedgerRepository
import com.madhusit.spendly.presentation.components.*

private const val HOME = "home"
private const val TRANSACTIONS = "transactions"
private const val ACCOUNTS = "accounts"
private const val INSIGHTS = "insights"
private const val ADD_EXPENSE = "add_expense"

@Composable
fun SpendlyApp(repository: FoundationRepository, ledgerRepository: LedgerRepository) {
    val navController = rememberNavController()
    val foundationVm: FoundationViewModel = viewModel(factory = FoundationViewModel.factory(repository))
    val ledgerVm: LedgerViewModel = viewModel(factory = LedgerViewModel.factory(ledgerRepository))
    val transactions by ledgerVm.transactions.collectAsStateWithLifecycle()
    val totals by ledgerVm.totals.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { foundationVm.initialize(); ledgerVm.ensureDefaultEntity() }
    Scaffold(bottomBar = { BottomNav(navController) }, modifier = Modifier.fillMaxSize()) { padding ->
        NavHost(navController, startDestination = HOME) {
            composable(HOME) { LedgerHome(padding, totals) { navController.navigate(ADD_EXPENSE) } }
            composable(ADD_EXPENSE) { AddExpenseScreen(padding, ledgerVm::addExpense) { navController.popBackStack() } }
            composable(TRANSACTIONS) { LedgerTransactions(padding, transactions) }
            composable(ACCOUNTS) { PlaceholderScreen("Accounts", padding) }
            composable(INSIGHTS) { PlaceholderScreen("Insights", padding) }
        }
    }
}
