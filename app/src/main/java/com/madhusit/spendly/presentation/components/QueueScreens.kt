package com.madhusit.spendly.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onSeedItem: () -> Unit,
    onBulkApprove: (List<String>, (String?) -> Unit) -> Unit = { _, _ -> },
    onBulkReject: (List<String>, (String?) -> Unit) -> Unit = { _, _ -> }
) {
    val selectedIds = remember { mutableStateListOf<String>() }
    LaunchedEffect(queue) { selectedIds.removeAll { id -> queue.none { it.id == id } } }

    val allSelected = queue.isNotEmpty() && selectedIds.size == queue.size

    var showApproveDialog by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }

    if (showApproveDialog) {
        AlertDialog(
            onDismissRequest = { if (!busy) showApproveDialog = false },
            title = { Text("Approve ${selectedIds.size} transaction${if (selectedIds.size == 1) "" else "s"}?") },
            text = { Text("They will be confirmed with their parsed values and move to your transaction history.") },
            confirmButton = {
                Button(
                    onClick = {
                        busy = true; actionError = null
                        val ids = selectedIds.toList()
                        onBulkApprove(ids) { err ->
                            busy = false
                            if (err == null) { selectedIds.clear(); showApproveDialog = false }
                            else actionError = err
                        }
                    },
                    enabled = !busy
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Approve")
                }
            },
            dismissButton = { TextButton(onClick = { showApproveDialog = false }, enabled = !busy) { Text("Cancel") } }
        )
    }

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { if (!busy) showRejectDialog = false },
            title = { Text("Delete ${selectedIds.size} transaction${if (selectedIds.size == 1) "" else "s"}?") },
            text = { Text("These will be permanently removed. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        busy = true; actionError = null
                        val ids = selectedIds.toList()
                        onBulkReject(ids) { err ->
                            busy = false
                            if (err == null) { selectedIds.clear(); showRejectDialog = false }
                            else actionError = err
                        }
                    },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Delete")
                }
            },
            dismissButton = { TextButton(onClick = { showRejectDialog = false }, enabled = !busy) { Text("Cancel") } }
        )
    }

    Scaffold(
        modifier = Modifier.padding(padding),
        topBar = {
            Column {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    Text("Review queue", style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f))
                    if (queue.isEmpty()) {
                        TextButton(onClick = onSeedItem) { Text("Add test item") }
                    }
                }
                if (queue.isNotEmpty()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (allSelected) selectedIds.clear()
                                else { selectedIds.clear(); selectedIds.addAll(queue.map { it.id }) }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = allSelected,
                            onCheckedChange = {
                                if (allSelected) selectedIds.clear()
                                else { selectedIds.clear(); selectedIds.addAll(queue.map { it.id }) }
                            }
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (allSelected) "Deselect all" else "Select all (${queue.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${queue.size} item${if (queue.size == 1) "" else "s"} pending",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider()
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = selectedIds.isNotEmpty(),
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                Surface(tonalElevation = 3.dp) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        actionError?.let { err ->
                            Text(err, color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 8.dp))
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showRejectDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Reject (${selectedIds.size})")
                            }
                            Button(
                                onClick = { showApproveDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Approve (${selectedIds.size})")
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (queue.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("All caught up", style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.testTag("queue-empty-title"))
                Text("Everything currently needs no attention.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                FilledTonalButton(onClick = onDone, modifier = Modifier.testTag("queue-done")) { Text("Done") }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 4.dp,
                    bottom = innerPadding.calculateBottomPadding() + 80.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("screen_review_queue")
            ) {
                items(queue, key = { it.id }) { txn ->
                    val entity = entities.firstOrNull { it.id == txn.sourceEntityId }
                    val isSelected = txn.id in selectedIds
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = {
                                if (isSelected) selectedIds.remove(txn.id) else selectedIds.add(txn.id)
                            }
                        )
                        Box(Modifier.weight(1f)) {
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
    }
}

@Composable
private fun QueueItemCard(
    txn: LedgerTransaction,
    entity: FinancialEntity?,
    entities: List<FinancialEntity>,
    onClick: () -> Unit
) {
    ElevatedCard(
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
    allTransactions: List<LedgerTransaction> = emptyList(),
    onConfirm: (merchantName: String?, type: TransactionType, sourceEntityId: String, done: (String?) -> Unit) -> Unit,
    onLater: () -> Unit,
    onBack: () -> Unit
) {
    if (transaction == null) {
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Item not found", style = MaterialTheme.typography.headlineSmall)
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
    val knownMerchants = remember(allTransactions) {
        val fromTxns = allTransactions.mapNotNull { it.merchantName }
        (fromTxns + SEED_MERCHANTS)
            .groupBy { it.lowercase() }
            .map { (_, names) -> names.first() }
            .sortedBy { it.lowercase() }
    }

    val typeOptions = listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER)

    Column(
        Modifier.fillMaxSize().padding(padding).testTag("screen_review_item")
    ) {
        // Top bar with back arrow
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                "Review transaction",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Amount hero
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        formatInr(transaction.amountMinor),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.W300,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        transaction.type.displayName(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Why review banner — subtle, not alarming
            item {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "Needs attention",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                reviewReason(transaction, entities),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Merchant field with autocomplete
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "MERCHANT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    MerchantField(
                        value = merchantName,
                        onValueChange = { merchantName = it; error = null },
                        label = "e.g. Swiggy, Amazon…",
                        suggestions = knownMerchants,
                        enabled = !saving
                    )
                }
            }

            // Type selector
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "TYPE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        typeOptions.forEachIndexed { index, option ->
                            SegmentedButton(
                                selected = selectedType == option,
                                onClick = { selectedType = option; error = null },
                                shape = SegmentedButtonDefaults.itemShape(index, typeOptions.size)
                            ) { Text(option.displayName()) }
                        }
                    }
                }
            }

            // Account picker
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "ACCOUNT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    if (entities.isEmpty()) {
                        Text(
                            "No accounts available. Add one first.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            }

            error?.let { msg ->
                item {
                    Text(msg, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag("review-error"))
                }
            }

            // Actions
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onLater, enabled = !saving) { Text("Skip for now") }
                    FilledTonalButton(
                        onClick = {
                            saving = true
                            error = null
                            onConfirm(merchantName.ifBlank { null }, selectedType, selectedEntityId) { result ->
                                saving = false
                                if (result != null) error = result
                            }
                        },
                        enabled = !saving && selectedEntityId.isNotBlank(),
                        modifier = Modifier.weight(1f).testTag("review-confirm")
                    ) {
                        Text(if (saving) "Confirming…" else "Confirm")
                    }
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
    FinancialEntityType.MERCHANT -> "Merchant"
    FinancialEntityType.OTHER -> "Other"
}
