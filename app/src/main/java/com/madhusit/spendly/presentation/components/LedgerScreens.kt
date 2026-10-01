package com.madhusit.spendly.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.madhusit.spendly.domain.ledger.LedgerTotals
import com.madhusit.spendly.domain.ledger.LedgerTransaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LedgerHome(padding: PaddingValues, totals: LedgerTotals, onAddExpense: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(padding).padding(20.dp).testTag("screen_home"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Spendly", style = MaterialTheme.typography.headlineMedium)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Net spending", style = MaterialTheme.typography.labelLarge)
                Text("₹" + (totals.netSpendingMinor / 100), style = MaterialTheme.typography.headlineMedium)
                Text("Income ₹" + (totals.incomeMinor / 100))
                Text("Spending ₹" + (totals.grossSpendingMinor / 100))
                Text("Refunds ₹" + (totals.refundsMinor / 100))
            }
        }
        Button(onClick = onAddExpense, Modifier.fillMaxWidth().testTag("add-expense")) {
            Text("Add expense")
        }
    }
}

@Composable
fun AddExpenseScreen(
    padding: PaddingValues,
    onSave: (String, String, (String?) -> Unit) -> Unit,
    onCancel: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().padding(padding).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Add expense", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            amount, { amount = it; error = null },
            label = { Text("Amount (₹)") }, singleLine = true, enabled = !saving,
            modifier = Modifier.fillMaxWidth().testTag("expense-amount")
        )
        OutlinedTextField(
            merchant, { merchant = it; error = null },
            label = { Text("Merchant") }, singleLine = true, enabled = !saving,
            modifier = Modifier.fillMaxWidth().testTag("expense-merchant")
        )
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("expense-error"))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onCancel, enabled = !saving) { Text("Cancel") }
            Button(
                onClick = {
                    error = null
                    saving = true
                    onSave(amount, merchant) { result ->
                        saving = false
                        if (result == null) onCancel() else error = result
                    }
                },
                enabled = !saving,
                modifier = Modifier.testTag("save-expense")
            ) { Text(if (saving) "Saving…" else "Save expense") }
        }
    }
}

@Composable
fun LedgerTransactions(padding: PaddingValues, transactions: List<LedgerTransaction>) {
    LazyColumn(
        Modifier.fillMaxSize().padding(padding).testTag("screen_transactions"),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Transactions", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("transactions-title"))
        }
        if (transactions.isEmpty()) {
            item {
                Text(
                    "No transactions yet. Add an expense to start your ledger.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag("transactions-empty")
                )
            }
        } else {
            items(transactions, key = { it.id }) { transaction -> TransactionRow(transaction) }
        }
    }
}

@Composable
private fun TransactionRow(transaction: LedgerTransaction) {
    val timestamp = SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault())
        .format(Date(transaction.transactionTimestamp))
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    transaction.merchantName ?: transaction.type.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    transaction.type.name + " · " + timestamp,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (transaction.status != com.madhusit.spendly.domain.ledger.TransactionStatus.CONFIRMED) {
                    Text(transaction.status.name, style = MaterialTheme.typography.labelMedium)
                }
            }
            Text("₹" + (transaction.amountMinor / 100), style = MaterialTheme.typography.titleMedium)
        }
    }
}
