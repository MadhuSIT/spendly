package com.madhusit.spendly.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.madhusit.spendly.domain.ledger.*

fun entityBalance(entity: FinancialEntity, transactions: List<LedgerTransaction>): Long {
    val confirmed = transactions.filter { it.status == TransactionStatus.CONFIRMED }
    val inflow = confirmed.filter { it.destinationEntityId == entity.id || it.sourceEntityId == entity.id }.let { txns ->
        txns.filter {
            (it.type == TransactionType.INCOME && it.sourceEntityId == entity.id) ||
            (it.type == TransactionType.TRANSFER && it.destinationEntityId == entity.id)
        }.sumOf { it.amountMinor } -
        txns.filter {
            (it.type == TransactionType.EXPENSE && it.sourceEntityId == entity.id) ||
            (it.type == TransactionType.TRANSFER && it.sourceEntityId == entity.id) ||
            (it.type == TransactionType.CARD_PAYMENT && it.sourceEntityId == entity.id)
        }.sumOf { it.amountMinor }
    }
    return inflow
}

@Composable
fun AccountsScreen(
    padding: PaddingValues,
    entities: List<FinancialEntity>,
    transactions: List<LedgerTransaction>,
    onAddEntity: () -> Unit,
    onOpenEntity: (String) -> Unit
) {
    val banks = entities.filter { it.type == FinancialEntityType.BANK_ACCOUNT }
    val cards = entities.filter { it.type == FinancialEntityType.CREDIT_CARD || it.type == FinancialEntityType.DEBIT_CARD }
    val cash = entities.filter { it.type == FinancialEntityType.CASH }
    val other = entities.filter { it.type == FinancialEntityType.OTHER }

    Box(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .testTag("screen_accounts")
    ) {
        if (entities.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Accounts", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "No accounts yet. Add a bank account, credit card or cash wallet to get started.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag("accounts-empty")
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp, top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    Text(
                        "Accounts",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
                if (banks.isNotEmpty()) {
                    item { EntityGroupHeader("Bank accounts") }
                    items(banks, key = { it.id }) { EntityCard(it, entityBalance(it, transactions), onOpenEntity) }
                }
                if (cards.isNotEmpty()) {
                    item { EntityGroupHeader("Credit & debit cards") }
                    items(cards, key = { it.id }) { EntityCard(it, entityBalance(it, transactions), onOpenEntity) }
                }
                if (cash.isNotEmpty()) {
                    item { EntityGroupHeader("Cash") }
                    items(cash, key = { it.id }) { EntityCard(it, entityBalance(it, transactions), onOpenEntity) }
                }
                if (other.isNotEmpty()) {
                    item { EntityGroupHeader("Other") }
                    items(other, key = { it.id }) { EntityCard(it, entityBalance(it, transactions), onOpenEntity) }
                }
            }
        }

        FloatingActionButton(
            onClick = onAddEntity,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add-entity")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add account")
        }
    }
}

@Composable
private fun EntityGroupHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .fillMaxWidth()
    )
}

@Composable
private fun EntityCard(entity: FinancialEntity, balance: Long, onClick: (String) -> Unit) {
    Card(
        onClick = { onClick(entity.id) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .testTag("entity-${entity.id}")
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(entity.name, style = MaterialTheme.typography.titleMedium)
                val sub = buildString {
                    append(entity.type.displayLabel())
                    entity.lastFour?.let { append(" · ••••$it") }
                }
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(formatInr(balance), style = MaterialTheme.typography.titleMedium)
                if (!entity.active) {
                    Badge { Text("Inactive") }
                }
            }
        }
    }
}

@Composable
fun AddEntityScreen(
    padding: PaddingValues,
    onSave: (FinancialEntityType, String, String?, String?, (String?) -> Unit) -> Unit,
    onCancel: () -> Unit
) {
    var type by remember { mutableStateOf(FinancialEntityType.BANK_ACCOUNT) }
    var name by remember { mutableStateOf("") }
    var identifier by remember { mutableStateOf("") }
    var lastFour by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    val showIdentifier = type == FinancialEntityType.BANK_ACCOUNT ||
        type == FinancialEntityType.CREDIT_CARD ||
        type == FinancialEntityType.DEBIT_CARD

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Add account", style = MaterialTheme.typography.headlineSmall) }

        item {
            Text("Type", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            val typeOptions = listOf(
                FinancialEntityType.BANK_ACCOUNT,
                FinancialEntityType.CREDIT_CARD,
                FinancialEntityType.CASH
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                typeOptions.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = type == option,
                        onClick = { type = option; error = null },
                        shape = SegmentedButtonDefaults.itemShape(index, typeOptions.size),
                        modifier = Modifier.weight(1f)
                    ) { Text(option.displayLabel()) }
                }
            }
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = null },
                label = { Text("Account name") },
                singleLine = true,
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().testTag("entity-name")
            )
        }

        if (showIdentifier) {
            item {
                OutlinedTextField(
                    value = identifier,
                    onValueChange = { identifier = it; error = null },
                    label = { Text("Account / card number (optional)") },
                    singleLine = true,
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth().testTag("entity-identifier")
                )
            }
            item {
                OutlinedTextField(
                    value = lastFour,
                    onValueChange = { if (it.length <= 4) { lastFour = it; error = null } },
                    label = { Text("Last four digits (optional)") },
                    singleLine = true,
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth().testTag("entity-last-four")
                )
            }
        }

        error?.let { msg ->
            item {
                Text(msg, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("entity-error"))
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onCancel, enabled = !saving) { Text("Cancel") }
                Button(
                    onClick = {
                        saving = true
                        error = null
                        onSave(
                            type,
                            name,
                            identifier.ifBlank { null },
                            lastFour.ifBlank { null }
                        ) { result ->
                            saving = false
                            if (result != null) error = result
                        }
                    },
                    enabled = !saving && name.isNotBlank(),
                    modifier = Modifier.testTag("save-entity")
                ) { Text(if (saving) "Saving…" else "Save account") }
            }
        }
    }
}

@Composable
fun EntityDetailScreen(
    padding: PaddingValues,
    entity: FinancialEntity?,
    transactions: List<LedgerTransaction>,
    entities: List<FinancialEntity>,
    onBack: () -> Unit,
    onAddTransaction: () -> Unit
) {
    if (entity == null) {
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Account not found", style = MaterialTheme.typography.headlineSmall)
            Text("This account may have been removed.")
            TextButton(onClick = onBack) { Text("Back") }
        }
        return
    }

    val entityTxns = transactions.filter {
        it.sourceEntityId == entity.id || it.destinationEntityId == entity.id
    }
    val income = entityTxns
        .filter { it.type == TransactionType.INCOME && it.status == TransactionStatus.CONFIRMED }
        .sumOf { it.amountMinor }
    val spending = entityTxns
        .filter { it.type == TransactionType.EXPENSE && it.status == TransactionStatus.CONFIRMED }
        .sumOf { it.amountMinor }

    LazyColumn(
        Modifier.fillMaxSize().padding(padding).testTag("screen_entity_detail"),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TextButton(onClick = onBack) { Text("Back to accounts") }
        }
        item {
            Text(entity.name, style = MaterialTheme.typography.headlineMedium)
            val sub = buildString {
                append(entity.type.displayLabel())
                entity.lastFour?.let { append(" · ••••$it") }
            }
            Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Income", style = MaterialTheme.typography.labelMedium)
                        Text(formatInr(income), style = MaterialTheme.typography.titleMedium)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Spending", style = MaterialTheme.typography.labelMedium)
                        Text(formatInr(spending), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Transactions", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onAddTransaction) { Text("Add") }
            }
        }
        if (entityTxns.isEmpty()) {
            item {
                Text(
                    "No transactions for this account yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag("entity-detail-empty")
                )
            }
        } else {
            items(entityTxns.sortedByDescending { it.transactionTimestamp }, key = { it.id }) { txn ->
                val counterparty = if (txn.destinationEntityId == entity.id) {
                    entities.firstOrNull { it.id == txn.sourceEntityId }?.name
                } else {
                    txn.destinationEntityId?.let { id -> entities.firstOrNull { it.id == id }?.name }
                }
                EntityTransactionRow(txn, counterparty)
            }
        }
    }
}

@Composable
private fun EntityTransactionRow(transaction: LedgerTransaction, counterparty: String?) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    transaction.merchantName ?: transaction.type.displayName(),
                    style = MaterialTheme.typography.titleSmall
                )
                val sub = buildString {
                    append(transaction.type.displayName())
                    counterparty?.let { append(" · $it") }
                }
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(formatInr(transaction.amountMinor), style = MaterialTheme.typography.titleSmall)
        }
    }
}

private fun FinancialEntityType.displayLabel(): String = when (this) {
    FinancialEntityType.BANK_ACCOUNT -> "Bank account"
    FinancialEntityType.CREDIT_CARD -> "Credit card"
    FinancialEntityType.DEBIT_CARD -> "Debit card"
    FinancialEntityType.CASH -> "Cash"
    FinancialEntityType.OTHER -> "Other"
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
    "₹" + java.math.BigDecimal(amountMinor).movePointLeft(2).setScale(2).toPlainString()
