package com.madhusit.spendly.presentation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import com.madhusit.spendly.domain.FoundationRepository
import com.madhusit.spendly.presentation.notification.SpendlyNotificationHelper
import com.madhusit.spendly.domain.auth.AuthRepository
import com.madhusit.spendly.domain.ledger.LedgerRepository
import com.madhusit.spendly.domain.sms.SmsIngestionRepository
import com.madhusit.spendly.domain.sync.SyncRepository
import com.madhusit.spendly.presentation.components.*

private const val HOME = "home"
private const val TRANSACTIONS = "transactions"
private const val ACCOUNTS = "accounts"
private const val INSIGHTS = "insights"
private const val ADD_TRANSACTION = "add_transaction"
private const val TRANSACTION_DETAIL = "transaction/{id}"
private const val ADD_ENTITY = "add_entity"
private const val ENTITY_DETAIL = "entity/{id}"
private const val REVIEW_QUEUE = "review_queue"
private const val REVIEW_ITEM = "review_item/{id}"
private const val EDIT_TRANSACTION = "edit_transaction/{id}"
private const val BULK_MANAGE = "bulk_manage"

@Composable
fun SpendlyApp(
    repository: FoundationRepository,
    ledgerRepository: LedgerRepository,
    authRepository: AuthRepository,
    syncRepository: SyncRepository,
    smsIngestionRepository: SmsIngestionRepository
) {
    val authVm: AuthViewModel = viewModel(
        factory = AuthViewModel.factory(authRepository, syncRepository, ledgerRepository, smsIngestionRepository)
    )
    val user by authVm.user.collectAsStateWithLifecycle()

    if (user == null) {
        LoginScreen(
            onGoogleSignIn = { authVm.signInWithGoogle(it) },
            onSkip = { authVm.signInAsGuest() },
            error = authVm.syncError
        )
        return
    }

    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var readSmsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* RECEIVE_SMS: receiver activates on grant */ }
    val readSmsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        readSmsGranted = granted
        if (granted) {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val summary = smsIngestionRepository.scanInbox(context)
                    SpendlyNotificationHelper.notifyBatchImport(context, summary.total, summary.saved, summary.review)
                } catch (e: Exception) {
                    android.util.Log.e("Spendly.App", "scanInbox failed: ${e.message}", e)
                }
            }
        }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* channel created; permission decision is theirs */ }

    LaunchedEffect(Unit) {
        SpendlyNotificationHelper.createChannels(context)
        android.util.Log.i("Spendly.App", "LaunchedEffect: readSmsGranted=$readSmsGranted")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) smsPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
        if (!readSmsGranted) {
            readSmsLauncher.launch(Manifest.permission.READ_SMS)
        } else {
            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val summary = smsIngestionRepository.scanInbox(context)
                    SpendlyNotificationHelper.notifyBatchImport(context, summary.total, summary.saved, summary.review)
                }
            } catch (e: Exception) {
                android.util.Log.e("Spendly.App", "scanInbox failed: ${e.message}", e)
            }
        }
    }

    val navController = rememberNavController()
    val foundationVm: FoundationViewModel = viewModel(factory = FoundationViewModel.factory(repository))
    val ledgerVm: LedgerViewModel = viewModel(factory = LedgerViewModel.factory(ledgerRepository))
    val transactions by ledgerVm.transactions.collectAsStateWithLifecycle()
    val entities by ledgerVm.entities.collectAsStateWithLifecycle()
    val totals by ledgerVm.totals.collectAsStateWithLifecycle()
    val reviewQueue by ledgerVm.reviewQueue.collectAsStateWithLifecycle()

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
                    user = user,
                    onSignOut = { authVm.signOut() },
                    totals = totals,
                    recentTransactions = transactions.filter { !it.reviewRequired }.take(8),
                    reviewQueueCount = reviewQueue.size,
                    onAddTransaction = { navController.navigate(ADD_TRANSACTION) },
                    onOpenTransaction = { navController.navigate("transaction/$it") },
                    onOpenQueue = { navController.navigate(REVIEW_QUEUE) },
                    onScanSms = {
                        if (readSmsGranted) {
                            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                try {
                                    val summary = smsIngestionRepository.scanInbox(context)
                                    SpendlyNotificationHelper.notifyBatchImport(context, summary.total, summary.saved, summary.review)
                                } catch (e: Exception) {
                                    android.util.Log.e("Spendly.App", "manual scan failed: ${e.message}", e)
                                }
                            }
                        } else {
                            readSmsLauncher.launch(Manifest.permission.READ_SMS)
                        }
                    }
                )
            }
            composable(TRANSACTIONS) {
                LedgerTransactions(
                    padding = padding,
                    transactions = transactions.filter { !it.reviewRequired },
                    reviewQueueCount = reviewQueue.size,
                    onOpenTransaction = { navController.navigate("transaction/$it") },
                    onAddTransaction = { navController.navigate(ADD_TRANSACTION) },
                    onOpenQueue = { navController.navigate(REVIEW_QUEUE) },
                    onManage = { navController.navigate(BULK_MANAGE) }
                )
            }
            composable(BULK_MANAGE) {
                BulkManageScreen(
                    padding = padding,
                    transactions = transactions.filter { !it.reviewRequired },
                    entities = entities,
                    onBack = { navController.popBackStack() },
                    onDeleteSelected = { ids, done ->
                        ledgerVm.bulkDeleteTransactions(ids, done)
                    }
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
                    onBack = { navController.popBackStack() },
                    onEdit = transaction?.let { { navController.navigate("edit_transaction/${it.id}") } },
                    onDelete = transaction?.let { txn ->
                        {
                            ledgerVm.deleteTransaction(txn.id) { error ->
                                if (error == null) navController.popBackStack()
                            }
                        }
                    }
                )
            }
            composable(
                EDIT_TRANSACTION,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                val transaction = transactions.firstOrNull { it.id == entry.arguments?.getString("id") }
                EditTransactionScreen(
                    padding = padding,
                    transaction = transaction,
                    entities = entities,
                    transactions = transactions,
                    onSave = { type, amount, title, source, destination, done ->
                        ledgerVm.editTransaction(
                            transactionId = transaction?.id ?: "",
                            type = type,
                            amountRupees = amount,
                            title = title,
                            sourceEntityId = source,
                            destinationEntityId = destination
                        ) { error, updated ->
                            done(error)
                            if (error == null && updated != null) {
                                authVm.pushTransaction(updated)
                                navController.popBackStack()
                            }
                        }
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(ADD_TRANSACTION) {
                AddTransactionScreen(
                    padding = padding,
                    entities = entities,
                    transactions = transactions,
                    onSave = { type, amount, title, source, destination, done ->
                        ledgerVm.addTransaction(type, amount, title, source, destination) { error, txn ->
                            done(error)
                            if (error == null && txn != null) {
                                authVm.pushTransaction(txn)
                                navController.popBackStack()
                            }
                        }
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(ACCOUNTS) {
                AccountsScreen(
                    padding = padding,
                    entities = entities,
                    transactions = transactions,
                    onAddEntity = { navController.navigate(ADD_ENTITY) },
                    onOpenEntity = { navController.navigate("entity/$it") }
                )
            }
            composable(ADD_ENTITY) {
                AddEntityScreen(
                    padding = padding,
                    onSave = { type, name, identifier, lastFour, done ->
                        ledgerVm.addEntity(type, name, identifier, lastFour) { error, entity ->
                            done(error)
                            if (error == null && entity != null) {
                                authVm.pushEntity(entity)
                                navController.popBackStack()
                            }
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
            composable(REVIEW_QUEUE) {
                ReviewQueueScreen(
                    padding = padding,
                    queue = reviewQueue,
                    entities = entities,
                    onOpenItem = { navController.navigate("review_item/$it") },
                    onDone = { navController.popBackStack() },
                    onSeedItem = { entities.firstOrNull()?.let { e -> ledgerVm.seedReviewItem(e.id) } },
                    onBulkApprove = { ids, done -> ledgerVm.bulkApproveQueueItems(ids, done) },
                    onBulkReject = { ids, done -> ledgerVm.bulkDeleteTransactions(ids, done) }
                )
            }
            composable(
                REVIEW_ITEM,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                val txn = reviewQueue.firstOrNull { it.id == entry.arguments?.getString("id") }
                ReviewItemScreen(
                    padding = padding,
                    transaction = txn,
                    entities = entities,
                    allTransactions = transactions,
                    onConfirm = { merchantName, type, sourceEntityId, done ->
                        ledgerVm.confirmQueueItem(
                            transactionId = txn?.id ?: "",
                            merchantName = merchantName,
                            type = type,
                            sourceEntityId = sourceEntityId
                        ) { error, updated ->
                            done(error)
                            if (error == null && updated != null) {
                                authVm.pushTransaction(updated)
                                navController.popBackStack()
                            }
                        }
                    },
                    onLater = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(INSIGHTS) {
                InsightsScreen(
                    padding = padding,
                    transactions = transactions,
                    entities = entities
                )
            }
        }
    }
}
