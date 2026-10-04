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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.madhusit.spendly.domain.ledger.FinancialEntity
import com.madhusit.spendly.domain.ledger.LedgerTransaction
import com.madhusit.spendly.domain.ledger.TransactionType
import java.text.SimpleDateFormat
import java.util.*

private val dateDisplayFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
private val txnTimeFmt = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkManageScreen(
    padding: PaddingValues,
    transactions: List<LedgerTransaction>,
    entities: List<FinancialEntity>,
    onBack: () -> Unit,
    onDeleteSelected: (List<String>, (String?) -> Unit) -> Unit
) {
    // Default range: last 7 days
    val now = remember { System.currentTimeMillis() }
    var rangeStartMs by remember { mutableLongStateOf(now - 7L * 24 * 60 * 60 * 1000) }
    var rangeEndMs by remember { mutableLongStateOf(now) }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val filtered = remember(transactions, rangeStartMs, rangeEndMs) {
        val endOfDay = rangeEndMs + 24 * 60 * 60 * 1000 - 1
        transactions
            .filter { it.transactionTimestamp in rangeStartMs..endOfDay }
            .sortedByDescending { it.transactionTimestamp }
    }

    val selectedIds = remember { mutableStateListOf<String>() }

    // Keep selection in sync when filter changes
    LaunchedEffect(filtered) {
        selectedIds.removeAll { id -> filtered.none { it.id == id } }
    }

    val allSelected = filtered.isNotEmpty() && selectedIds.size == filtered.size

    var showConfirmDialog by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    // Date pickers
    val startPickerState = rememberDatePickerState(
        initialSelectedDateMillis = rangeStartMs,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= rangeEndMs
        }
    )
    val endPickerState = rememberDatePickerState(
        initialSelectedDateMillis = rangeEndMs,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= rangeStartMs && utcTimeMillis <= now
        }
    )

    if (showStartPicker) {
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startPickerState.selectedDateMillis?.let { rangeStartMs = it }
                    selectedIds.clear()
                    showStartPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = startPickerState)
        }
    }

    if (showEndPicker) {
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endPickerState.selectedDateMillis?.let { rangeEndMs = it }
                    selectedIds.clear()
                    showEndPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = endPickerState)
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!deleting) showConfirmDialog = false },
            title = { Text("Delete ${selectedIds.size} transaction${if (selectedIds.size == 1) "" else "s"}?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        deleting = true
                        deleteError = null
                        val toDelete = selectedIds.toList()
                        onDeleteSelected(toDelete) { error ->
                            deleting = false
                            if (error == null) {
                                selectedIds.clear()
                                showConfirmDialog = false
                            } else {
                                deleteError = error
                            }
                        }
                    },
                    enabled = !deleting,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    if (deleting) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }, enabled = !deleting) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        modifier = Modifier.padding(padding),
        topBar = {
            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    Text("Manage Transactions", style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f))
                }

                // Date range chips
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    FilterChip(
                        selected = false,
                        onClick = { showStartPicker = true },
                        label = { Text(dateDisplayFmt.format(Date(rangeStartMs))) }
                    )
                    Text("–", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FilterChip(
                        selected = false,
                        onClick = { showEndPicker = true },
                        label = { Text(dateDisplayFmt.format(Date(rangeEndMs))) }
                    )
                }

                HorizontalDivider()
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
                        deleteError?.let { err ->
                            Text(err, color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 8.dp))
                        }
                        Button(
                            onClick = { showConfirmDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null,
                                modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Delete (${selectedIds.size})")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("No transactions", style = MaterialTheme.typography.headlineSmall)
                    Text("in the selected date range",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 4.dp,
                    bottom = innerPadding.calculateBottomPadding() + 80.dp
                ),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Select all row
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (allSelected) selectedIds.clear()
                                else { selectedIds.clear(); selectedIds.addAll(filtered.map { it.id }) }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = allSelected,
                            onCheckedChange = {
                                if (allSelected) selectedIds.clear()
                                else { selectedIds.clear(); selectedIds.addAll(filtered.map { it.id }) }
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (allSelected) "Deselect all" else "Select all (${filtered.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    HorizontalDivider()
                }

                items(filtered, key = { it.id }) { txn ->
                    val isSelected = txn.id in selectedIds
                    val entity = entities.firstOrNull { it.id == txn.sourceEntityId }

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isSelected) selectedIds.remove(txn.id) else selectedIds.add(txn.id)
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = {
                                if (isSelected) selectedIds.remove(txn.id) else selectedIds.add(txn.id)
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                txn.merchantName ?: txn.type.bulkDisplayName(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                buildString {
                                    append(txnTimeFmt.format(Date(txn.transactionTimestamp)))
                                    entity?.let { append(" · ${it.name}") }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            formatAmountInr(txn.amountMinor),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = when (txn.type) {
                                TransactionType.INCOME -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
    }
}

private fun TransactionType.bulkDisplayName(): String = when (this) {
    TransactionType.EXPENSE -> "Expense"
    TransactionType.INCOME -> "Income"
    TransactionType.TRANSFER -> "Transfer"
    TransactionType.CARD_PAYMENT -> "Card payment"
    TransactionType.REFUND -> "Refund"
    TransactionType.FEE -> "Fee"
    TransactionType.CASH_WITHDRAWAL -> "Cash withdrawal"
    TransactionType.REVERSAL -> "Reversal"
}

private fun formatAmountInr(amountMinor: Long): String =
    "₹" + java.math.BigDecimal(amountMinor).movePointLeft(2).setScale(2).toPlainString()
