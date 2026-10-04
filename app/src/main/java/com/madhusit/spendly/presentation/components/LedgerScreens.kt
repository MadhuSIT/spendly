package com.madhusit.spendly.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.madhusit.spendly.domain.ledger.*
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LedgerHome(
    padding: PaddingValues,
    totals: LedgerTotals,
    recentTransactions: List<LedgerTransaction>,
    reviewQueueCount: Int = 0,
    onAddTransaction: () -> Unit,
    onOpenTransaction: (String) -> Unit,
    onOpenQueue: () -> Unit = {}
) {
    Column(
        Modifier.fillMaxSize().padding(padding).padding(20.dp).testTag("screen_home"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Spendly", style = MaterialTheme.typography.headlineMedium)
        if (reviewQueueCount > 0) {
            Card(
                onClick = onOpenQueue,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth().testTag("queue-attention-card")
            ) {
                Row(
                    Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "$reviewQueueCount transaction${if (reviewQueueCount == 1) "" else "s"} need review",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            "Tap to resolve",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Text(
                        "→",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Net spending", style = MaterialTheme.typography.labelLarge)
                Text(formatInr(totals.netSpendingMinor), style = MaterialTheme.typography.headlineMedium)
                Text("Income " + formatInr(totals.incomeMinor))
                Text("Spending " + formatInr(totals.grossSpendingMinor))
                Text("Refunds " + formatInr(totals.refundsMinor))
            }
        }
        Text("Recent transactions", style = MaterialTheme.typography.titleLarge)
        if (recentTransactions.isEmpty()) {
            Text(
                "No transactions yet. Add your first transaction to start your ledger.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag("home-empty")
            )
        } else {
            recentTransactions.forEach { transaction ->
                TransactionRow(transaction) { onOpenTransaction(transaction.id) }
            }
        }
        FilledTonalButton(
            onClick = onAddTransaction,
            Modifier.fillMaxWidth().testTag("add-transaction")
        ) {
            Text("Add transaction")
        }
    }
}

@Composable
fun LedgerTransactions(
    padding: PaddingValues,
    transactions: List<LedgerTransaction>,
    reviewQueueCount: Int = 0,
    onOpenTransaction: (String) -> Unit,
    onAddTransaction: () -> Unit,
    onOpenQueue: () -> Unit = {}
) {
    Column(Modifier.fillMaxSize().padding(padding).testTag("screen_transactions")) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Transactions", style = MaterialTheme.typography.headlineSmall)
            Row {
                TextButton(onClick = onOpenQueue) {
                    Text(if (reviewQueueCount > 0) "Review ($reviewQueueCount)" else "Review queue")
                }
                TextButton(onClick = onAddTransaction) { Text("Add") }
            }
        }
        if (transactions.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(20.dp)) {
                Text(
                    "No transactions yet. Add a transaction to start your ledger.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag("transactions-empty")
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(transactions, key = { it.id }) { transaction ->
                    TransactionRow(transaction) { onOpenTransaction(transaction.id) }
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(transaction: LedgerTransaction, onClick: () -> Unit) {
    val timestamp = SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault())
        .format(Date(transaction.transactionTimestamp))
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag("transaction-${transaction.id}")
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    transaction.merchantName ?: transaction.type.displayName(),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    transaction.type.name + " · " + timestamp,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (transaction.status != TransactionStatus.CONFIRMED) {
                    Text(transaction.status.name, style = MaterialTheme.typography.labelMedium)
                }
            }
            Text(formatInr(transaction.amountMinor), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun TransactionDetailScreen(
    padding: PaddingValues,
    transaction: LedgerTransaction?,
    entities: List<FinancialEntity>,
    onBack: () -> Unit
) {
    if (transaction == null) {
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Transaction not found", style = MaterialTheme.typography.headlineSmall)
            Text("This transaction may have been removed or is no longer available.")
            TextButton(onClick = onBack) { Text("Back") }
        }
        return
    }

    val source = entities.firstOrNull { it.id == transaction.sourceEntityId }?.name ?: "Unknown account"
    val destination = transaction.destinationEntityId?.let { id ->
        entities.firstOrNull { it.id == id }?.name ?: "Unknown destination"
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(padding).testTag("screen_transaction_detail"),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TextButton(onClick = onBack) { Text("Back to transactions") }
            Text(transaction.merchantName ?: transaction.type.displayName(), style = MaterialTheme.typography.headlineSmall)
            Text(formatInr(transaction.amountMinor), style = MaterialTheme.typography.headlineMedium)
        }
        item {
            DetailSection("Transaction") {
                DetailRow("Type", transaction.type.displayName())
                DetailRow("Status", transaction.status.name)
                DetailRow("Currency", transaction.currency)
                DetailRow("Time", SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault()).format(Date(transaction.transactionTimestamp)))
            }
        }
        item {
            DetailSection("Accounts") {
                DetailRow("Source", source)
                destination?.let { DetailRow("Destination", it) }
            }
        }
        item {
            DetailSection("References") {
                transaction.referenceNumber?.let { DetailRow("Reference", it) }
                transaction.upiReference?.let { DetailRow("UPI reference", it) }
                if (transaction.referenceNumber == null && transaction.upiReference == null) {
                    Text("No reference available.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            DetailSection("Why Spendly added this") {
                if (transaction.parserSource != null && transaction.parserSource != "manual") {
                    Text(
                        "Financial activity was parsed from an automated source, validated and persisted in the ledger.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    transaction.parserSource?.let { DetailRow("Source", it) }
                    transaction.parserVersion?.let { DetailRow("Parser version", it) }
                    transaction.confidence?.let { DetailRow("Confidence", "%.0f%%".format(Locale.US, it * 100)) }
                } else {
                    Text("Added manually through Spendly.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun AddTransactionScreen(
    padding: PaddingValues,
    entities: List<FinancialEntity>,
    onSave: (
        TransactionType,
        String,
        String,
        String,
        String?,
        (String?) -> Unit
    ) -> Unit,
    onCancel: () -> Unit
) {
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amount by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var source by remember { mutableStateOf(entities.firstOrNull()?.id.orEmpty()) }
    var destination by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(entities) {
        if (source.isBlank()) source = entities.firstOrNull()?.id.orEmpty()
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Add transaction", style = MaterialTheme.typography.headlineSmall) }
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER).forEachIndexed { index, item ->
                    SegmentedButton(
                        selected = type == item,
                        onClick = { type = item; error = null },
                        shape = SegmentedButtonDefaults.itemShape(index, 3),
                        modifier = Modifier.weight(1f)
                    ) { Text(item.displayName()) }
                }
            }
        }
        item {
            OutlinedTextField(
                amount,
                { amount = it; error = null },
                label = { Text("Amount (₹)") },
                singleLine = true,
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().testTag("transaction-amount")
            )
        }
        item {
            OutlinedTextField(
                title,
                { title = it; error = null },
                label = { Text(if (type == TransactionType.INCOME) "Income source" else "Merchant / title") },
                singleLine = true,
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().testTag("transaction-title")
            )
        }
        item {
            EntityDropdown(
                label = "Source",
                entities = entities,
                selectedId = source,
                onSelected = { source = it; error = null },
                enabled = !saving
            )
        }
        if (type == TransactionType.TRANSFER) {
            item {
                EntityDropdown(
                    label = "Destination",
                    entities = entities,
                    selectedId = destination,
                    onSelected = { destination = it; error = null },
                    enabled = !saving,
                    excludedId = source
                )
            }
            if (entities.size < 2) {
                item {
                    Text(
                        "Transfers need two different financial entities. Add another account in the Accounts flow.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        error?.let { message ->
            item {
                Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("transaction-error"))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onCancel, enabled = !saving) { Text("Cancel") }
                FilledTonalButton(
                    onClick = {
                        saving = true
                        error = null
                        onSave(type, amount, title, source, destination) { result ->
                            saving = false
                            if (result != null) error = result
                        }
                    },
                    enabled = !saving && source.isNotBlank() && (type != TransactionType.TRANSFER || entities.size >= 2),
                    modifier = Modifier.testTag("save-transaction")
                ) { Text(if (saving) "Saving…" else "Save transaction") }
            }
        }
    }
}

@Composable
private fun EntityDropdown(
    label: String,
    entities: List<FinancialEntity>,
    selectedId: String?,
    onSelected: (String) -> Unit,
    enabled: Boolean,
    excludedId: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val options = entities.filter { it.id != excludedId }
    val selected = options.firstOrNull { it.id == selectedId }

    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled && options.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(selected?.name ?: "Choose $label")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { entity ->
                DropdownMenuItem(
                    text = { Text(entity.name) },
                    onClick = {
                        onSelected(entity.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun TransactionType.displayName(): String = when (this) {
    TransactionType.EXPENSE -> "Expense"
    TransactionType.INCOME -> "Income"
    TransactionType.TRANSFER -> "Transfer"
    TransactionType.CARD_PAYMENT -> "Card payment"
    TransactionType.REFUND -> "Refund"
    TransactionType.FEE -> "Fee"
    TransactionType.CASH_WITHDRAWAL -> "Cash withdrawal"
    TransactionType.REVERSAL -> "Reversal"
}

private fun formatInr(amountMinor: Long): String =
    "₹" + BigDecimal(amountMinor).movePointLeft(2).setScale(2).toPlainString()
