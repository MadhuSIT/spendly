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

private fun reviewReason(txn: LedgerTransaction, entities: List<FinancialEntity>): String {
    val hasEntity = entities.any { it.id == txn.sourceEntityId }
    return when {
        !hasEntity -> "Spendly could not identify the account this came from."
        txn.merchantName == null -> "The merchant name could not be read from this message."
        txn.confidence != null && txn.confidence < 0.75 ->
            "This was parsed with low confidence (${(txn.confidence * 100).toInt()}%). Please verify the details."
        else -> "This transaction needs your attention before it is confirmed."
    }
}

@Composable
fun ReviewQueueScreen(
    padding: PaddingValues,
    queue: List<LedgerTransaction>,
    entities: List<FinancialEntity>,
    onOpenItem: (String) -> Unit,
    onDone: () -> Unit,
    onSeedItem: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .testTag("screen_review_queue")
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Review queue", style = MaterialTheme.typography.headlineSmall)
            if (queue.isEmpty()) {
                TextButton(onClick = onSeedItem) { Text("Add test item") }
            }
        }

        if (queue.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "All caught up",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.testTag("queue-empty-title")
                )
                Text(
                    "Everything currently needs no attention.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onDone, modifier = Modifier.testTag("queue-done")) {
                    Text("Done")
                }
            }
        } else {
            Text(
                "${queue.size} item${if (queue.size == 1) "" else "s"} need your attention",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp)
            )
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(queue, key = { it.id }) { txn ->
                    val entity = entities.firstOrNull { it.id == txn.sourceEntityId }
                    QueueItemCard(
                        txn = txn,
                        entity = entity,
                        entities = entities,
                        onClick = { onOpenItem(txn.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueItemCard(
    txn: LedgerTransaction,
    entity: FinancialEntity?,
    entities: List<FinancialEntity>,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("queue-item-${txn.id}")
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    txn.merchantName ?: "Unknown merchant",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(formatInr(txn.amountMinor), style = MaterialTheme.typography.titleMedium)
            }
            val sub = buildString {
                append(txn.type.displayName())
                entity?.let { append(" · ${it.name}") } ?: append(" · Unknown account")
            }
            Text(
                sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    reviewReason(txn, entities),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            Text(
                "Tap to review →",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ReviewItemScreen(
    padding: PaddingValues,
    transaction: LedgerTransaction?,
    entities: List<FinancialEntity>,
    onConfirm: (merchantName: String?, type: TransactionType, sourceEntityId: String, done: (String?) -> Unit) -> Unit,
    onLater: () -> Unit,
    onBack: () -> Unit
) {
    if (transaction == null) {
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Item not found", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Back") }
        }
        return
    }

    var merchantName by remember { mutableStateOf(transaction.merchantName ?: "") }
    var selectedType by remember { mutableStateOf(transaction.type) }
    var selectedEntityId by remember {
        mutableStateOf(
            if (entities.any { it.id == transaction.sourceEntityId }) transaction.sourceEntityId
            else entities.firstOrNull()?.id ?: ""
        )
    }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    val typeOptions = listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER)

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .testTag("screen_review_item"),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { TextButton(onClick = onBack) { Text("Back to queue") } }

        item {
            Text("Review transaction", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                formatInr(transaction.amountMinor),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Why review?",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        reviewReason(transaction, entities),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        item {
            Text("Merchant / description", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = merchantName,
                onValueChange = { merchantName = it; error = null },
                label = { Text("Merchant name (optional)") },
                singleLine = true,
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().testTag("review-merchant")
            )
        }

        item {
            Text("Type", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                typeOptions.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = selectedType == option,
                        onClick = { selectedType = option; error = null },
                        shape = SegmentedButtonDefaults.itemShape(index, typeOptions.size),
                        modifier = Modifier.weight(1f)
                    ) { Text(option.displayName()) }
                }
            }
        }

        item {
            Text("Account", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            if (entities.isEmpty()) {
                Text(
                    "No accounts available. Add an account first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    entities.forEach { entity ->
                        FilterChip(
                            selected = entity.id == selectedEntityId,
                            onClick = { selectedEntityId = entity.id; error = null },
                            label = { Text("${entity.name} · ${entity.type.displayLabel()}") },
                            modifier = Modifier.testTag("review-entity-${entity.id}")
                        )
                    }
                }
            }
        }

        error?.let { msg ->
            item {
                Text(msg, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("review-error"))
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onLater, enabled = !saving) { Text("Later") }
                Button(
                    onClick = {
                        saving = true
                        error = null
                        onConfirm(merchantName.ifBlank { null }, selectedType, selectedEntityId) { result ->
                            saving = false
                            if (result != null) error = result
                        }
                    },
                    enabled = !saving && selectedEntityId.isNotBlank(),
                    modifier = Modifier.testTag("review-confirm")
                ) {
                    Text(if (saving) "Confirming…" else "Confirm transaction")
                }
            }
        }
    }
}

private fun formatInr(amountMinor: Long): String =
    "₹" + java.math.BigDecimal(amountMinor).movePointLeft(2).setScale(2).toPlainString()

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

private fun FinancialEntityType.displayLabel(): String = when (this) {
    FinancialEntityType.BANK_ACCOUNT -> "Bank account"
    FinancialEntityType.CREDIT_CARD -> "Credit card"
    FinancialEntityType.DEBIT_CARD -> "Debit card"
    FinancialEntityType.CASH -> "Cash"
    FinancialEntityType.OTHER -> "Other"
}
