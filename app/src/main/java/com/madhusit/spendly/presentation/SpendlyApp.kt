package com.madhusit.spendly.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.madhusit.spendly.domain.FoundationRepository
import com.madhusit.spendly.domain.ledger.LedgerRepository
import com.madhusit.spendly.presentation.components.*

private const val HOME = "home"
private const val TRANSACTIONS = "transactions"
private const val ACCOUNTS = "accounts"
private const val INSIGHTS = "insights"
private const val ADD_TRANSACTION = "add_transaction"
private const val TRANSACTION_DETAIL = "transaction/{id}"
private const val ADD_ENTITY = "add_entity"
private const val ENTITY_DETAIL = "entity/{id}"

@Composable
fun SpendlyApp(repository: FoundationRepository, ledgerRepository: LedgerRepository) {
    val navController = rememberNavController()
    val foundationVm: FoundationViewModel = viewModel(factory = FoundationViewModel.factory(repository))
    val ledgerVm: LedgerViewModel = viewModel(factory = LedgerViewModel.factory(ledgerRepository))
    val transactions by ledgerVm.transactions.collectAsStateWithLifecycle()
    val entities by ledgerVm.entities.collectAsStateWithLifecycle()
    val totals by ledgerVm.totals.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        foundationVm.initialize()
        ledgerVm.ensureDefaultEntity()
    }

    Scaffold(
        bottomBar = { BottomNav(navController) },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        NavHost(navController, startDestination = HOME) {
            composable(HOME) {
                LedgerHome(
                    padding = padding,
                    totals = totals,
                    recentTransactions = transactions.take(8),
                    onAddTransaction = { navController.navigate(ADD_TRANSACTION) },
                    onOpenTransaction = { navController.navigate("transaction/$it") }
                )
            }
            composable(TRANSACTIONS) {
                LedgerTransactions(
                    padding = padding,
                    transactions = transactions,
                    onOpenTransaction = { navController.navigate("transaction/$it") },
                    onAddTransaction = { navController.navigate(ADD_TRANSACTION) }
                )
            }
            composable(
                TRANSACTION_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                val transaction = transactions.firstOrNull { it.id == entry.arguments?.getString("id") }
                TransactionDetailScreen(
                    padding = padding,
                    transaction = transaction,
                    entities = entities,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(ADD_TRANSACTION) {
                AddTransactionScreen(
                    padding = padding,
                    entities = entities,
                    onSave = { type, amount, title, source, destination, done ->
                        ledgerVm.addTransaction(type, amount, title, source, destination) { error ->
                            done(error)
                            if (error == null) navController.popBackStack()
                        }
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(ACCOUNTS) {
                AccountsScreen(
                    padding = padding,
                    entities = entities,
                    onAddEntity = { navController.navigate(ADD_ENTITY) },
                    onOpenEntity = { navController.navigate("entity/$it") }
                )
            }
            composable(ADD_ENTITY) {
                AddEntityScreen(
                    padding = padding,
                    onSave = { type, name, identifier, lastFour, done ->
                        ledgerVm.addEntity(type, name, identifier, lastFour) { error ->
                            done(error)
                            if (error == null) navController.popBackStack()
                        }
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(
                ENTITY_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                val entity = entities.firstOrNull { it.id == entry.arguments?.getString("id") }
                EntityDetailScreen(
                    padding = padding,
                    entity = entity,
                    transactions = transactions,
                    entities = entities,
                    onBack = { navController.popBackStack() },
                    onAddTransaction = { navController.navigate(ADD_TRANSACTION) }
                )
            }
            composable(INSIGHTS) { PlaceholderScreen("Insights", padding) }
        }
    }
}
