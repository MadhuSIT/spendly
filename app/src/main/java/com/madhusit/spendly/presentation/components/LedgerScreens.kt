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

@Composable
fun LedgerHome(padding: PaddingValues, totals: LedgerTotals, onAddExpense: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Spendly")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text("Net spending")
                Text("₹" + (totals.netSpendingMinor / 100))
                Text("Income ₹" + (totals.incomeMinor / 100))
                Text("Spending ₹" + (totals.grossSpendingMinor / 100))
                Text("Refunds ₹" + (totals.refundsMinor / 100))
            }
        }
        Button(onClick = onAddExpense, Modifier.fillMaxWidth().testTag("add-expense")) { Text("Add expense") }
    }
}

@Composable
fun AddExpenseScreen(padding: PaddingValues, onSave: (String, String, (String?) -> Unit) -> Unit, onCancel: () -> Unit) {
    var amount by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Add expense")
        OutlinedTextField(amount, { amount = it }, label = { Text("Amount (₹)") }, modifier = Modifier.fillMaxWidth().testTag("expense-amount"))
        OutlinedTextField(merchant, { merchant = it }, label = { Text("Merchant") }, modifier = Modifier.fillMaxWidth().testTag("expense-merchant"))
        error?.let { Text(it) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onCancel) { Text("Cancel") }
            Button(onClick = { onSave(amount, merchant) { result -> if (result == null) onCancel() else error = result } }, Modifier.testTag("save-expense")) { Text("Save expense") }
        }
    }
}

@Composable
fun LedgerTransactions(padding: PaddingValues, transactions: List<LedgerTransaction>) {
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Transactions", Modifier.testTag("transactions-title")) }
        items(transactions, key = { it.id }) { transaction ->
            Card(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(16.dp)) {
                Column(Modifier.weight(1f)) { Text(transaction.merchantName ?: transaction.type.name); Text(transaction.type.name) }
                Text("₹" + (transaction.amountMinor / 100))
            }}
        }
    }
}
