package com.madhusit.spendly.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madhusit.spendly.domain.ledger.*
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal val SEED_MERCHANTS = listOf(
    // Food delivery
    "Swiggy", "Zomato", "Blinkit", "Zepto", "BigBasket", "Dunzo", "Instamart",
    // E-commerce
    "Amazon", "Flipkart", "Meesho", "Myntra", "Nykaa", "Snapdeal", "JioMart",
    // Transport
    "Ola", "Uber", "Rapido", "Ola Electric", "IRCTC", "RedBus", "MakeMyTrip",
    // Food & dining
    "McDonald's", "KFC", "Domino's", "Pizza Hut", "Subway", "Starbucks",
    "Haldiram's", "Café Coffee Day", "Burger King",
    // Grocery & retail
    "DMart", "Reliance Fresh", "Big Bazaar", "More Supermarket", "Lulu Hypermarket",
    // Entertainment
    "BookMyShow", "Netflix", "Hotstar", "Spotify", "Amazon Prime", "YouTube Premium",
    // Telecom & utilities
    "Jio", "Airtel", "Vi", "BSNL", "Tata Power", "Adani Electricity", "BESCOM",
    // Payments & finance
    "PhonePe", "Paytm", "Google Pay", "LIC", "Star Health", "Bajaj Finance",
    // Fuel
    "Indian Oil", "HP Petrol", "BPCL", "Shell"
)

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
    LazyColumn(
        Modifier.fillMaxSize().padding(padding).testTag("screen_home"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Greeting header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    greeting(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("Spendly", style = MaterialTheme.typography.headlineMedium)
            }
        }

        // Review banner
        if (reviewQueueCount > 0) {
            item {
                Card(
                    onClick = onOpenQueue,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().testTag("queue-attention-card")
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                "$reviewQueueCount transaction${if (reviewQueueCount == 1) "" else "s"} need review",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "Tap to resolve →",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        // Net spending hero card
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "NET SPENDING",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            formatInr(totals.netSpendingMinor),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.W300,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = (-0.5).sp
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        SpendingStat(
                            label = "Income",
                            value = formatInr(totals.incomeMinor),
                            valueColor = incomeColor(),
                            modifier = Modifier.weight(1f)
                        )
                        SpendingStat(
                            label = "Spending",
                            value = formatInr(totals.grossSpendingMinor),
                            valueColor = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        SpendingStat(
                            label = "Refunds",
                            value = formatInr(totals.refundsMinor),
                            valueColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Section header + add button
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent", style = MaterialTheme.typography.titleMedium)
                FilledTonalButton(
                    onClick = onAddTransaction,
                    modifier = Modifier.testTag("add-transaction")
                ) { Text("Add") }
            }
        }

        if (recentTransactions.isEmpty()) {
            item {
                Text(
                    "No transactions yet. Add your first to start your ledger.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("home-empty")
                )
            }
        } else {
            items(recentTransactions, key = { it.id }) { transaction ->
                TransactionRow(transaction) { onOpenTransaction(transaction.id) }
            }
        }
    }
}

@Composable
private fun SpendingStat(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, color = valueColor, fontWeight = FontWeight.W600)
    }
}

@Composable
private fun incomeColor(): Color = Color(0xFF2A7D4F)

private fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}

private fun dateLabel(epochMillis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = epochMillis }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    return when {
        cal.isSameDay(today) -> "Today"
        cal.isSameDay(yesterday) -> "Yesterday"
        cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) ->
            SimpleDateFormat("d MMMM", Locale.getDefault()).format(Date(epochMillis))
        else -> SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(epochMillis))
    }
}

private fun Calendar.isSameDay(other: Calendar) =
    get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
    get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LedgerTransactions(
    padding: PaddingValues,
    transactions: List<LedgerTransaction>,
    reviewQueueCount: Int = 0,
    onOpenTransaction: (String) -> Unit,
    onAddTransaction: () -> Unit,
    onOpenQueue: () -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf<TransactionType?>(null) }

    val displayed = remember(transactions, query, activeFilter) {
        transactions
            .let { list -> if (activeFilter != null) list.filter { it.type == activeFilter } else list }
            .let { list ->
                if (query.isBlank()) list
                else list.filter {
                    (it.merchantName?.contains(query, ignoreCase = true) == true) ||
                    it.type.displayName().contains(query, ignoreCase = true)
                }
            }
    }

    Column(Modifier.fillMaxSize().padding(padding).testTag("screen_transactions")) {
        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Transactions", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (reviewQueueCount > 0) {
                    FilledTonalButton(onClick = onOpenQueue) {
                        Text("Review ($reviewQueueCount)")
                    }
                }
                IconButton(onClick = { searchActive = !searchActive; if (!searchActive) query = "" }) {
                    Icon(Icons.Default.Search, contentDescription = "Search",
                        tint = if (searchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onAddTransaction) {
                    Icon(Icons.Default.Add, contentDescription = "Add transaction",
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Search bar + merchant autocomplete
        if (searchActive) {
            val allMerchants = remember(transactions) {
                transactions.mapNotNull { it.merchantName }.distinct().sorted()
            }
            val suggestions = remember(query, allMerchants) {
                if (query.isBlank()) allMerchants.take(6)
                else allMerchants.filter { it.contains(query, ignoreCase = true) }.take(6)
            }
            val showSuggestions = suggestions.isNotEmpty() && query.length < 20

            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 4.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search merchants, type…") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().testTag("transactions-search")
                )
                if (showSuggestions) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 3.dp,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            suggestions.forEachIndexed { i, merchant ->
                                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { query = merchant }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp))
                                    Text(merchant, style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        // Filter chips
        val filterTypes = listOf(
            TransactionType.EXPENSE, TransactionType.INCOME,
            TransactionType.TRANSFER, TransactionType.CARD_PAYMENT
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterTypes.forEach { type ->
                FilterChip(
                    selected = activeFilter == type,
                    onClick = { activeFilter = if (activeFilter == type) null else type },
                    label = { Text(type.displayName(), style = MaterialTheme.typography.labelMedium) }
                )
            }
        }

        if (displayed.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(20.dp)) {
                Text(
                    if (transactions.isEmpty()) "No transactions yet. Add a transaction to start your ledger."
                    else "No results for your search.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("transactions-empty")
                )
            }
        } else {
            val grouped = remember(displayed) {
                displayed.groupBy { dateLabel(it.transactionTimestamp) }.entries.toList()
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            ) {
                grouped.forEach { (label, txns) ->
                    stickyHeader(key = "header_$label") {
                        DateHeader(label)
                    }
                    items(txns, key = { it.id }) { transaction ->
                        Spacer(Modifier.height(10.dp))
                        TransactionRow(transaction) { onOpenTransaction(transaction.id) }
                    }
                }
                item { Spacer(Modifier.height(10.dp)) }
            }
        }
    }
}

@Composable
private fun DateHeader(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun TransactionRow(transaction: LedgerTransaction, onClick: () -> Unit) {
    val timestamp = SimpleDateFormat("d MMM · HH:mm", Locale.getDefault())
        .format(Date(transaction.transactionTimestamp))
    val amountColor = when (transaction.type) {
        TransactionType.INCOME -> Color(0xFF2A7D4F)
        TransactionType.EXPENSE, TransactionType.CARD_PAYMENT, TransactionType.FEE -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    val amountPrefix = when (transaction.type) {
        TransactionType.INCOME, TransactionType.REFUND -> "+"
        TransactionType.EXPENSE, TransactionType.CARD_PAYMENT,
        TransactionType.FEE, TransactionType.CASH_WITHDRAWAL -> "−"
        else -> ""
    }
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag("transaction-${transaction.id}")
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    transaction.merchantName ?: transaction.type.displayName(),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    "${transaction.type.displayName()} · $timestamp",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (transaction.status != TransactionStatus.CONFIRMED) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            transaction.status.displayName(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                amountPrefix + formatInr(transaction.amountMinor),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W600,
                color = amountColor
            )
        }
    }
}

@Composable
fun TransactionDetailScreen(
    padding: PaddingValues,
    transaction: LedgerTransaction?,
    entities: List<FinancialEntity>,
    onBack: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    if (transaction == null) {
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Transaction not found", style = MaterialTheme.typography.headlineSmall)
            Text("This transaction may have been removed or is no longer available.")
        }
        return
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete transaction?") },
            text = { Text("This will permanently remove the transaction from your ledger.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete?.invoke() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        transaction.merchantName ?: transaction.type.displayName(),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        transaction.type.displayName(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (onEdit != null) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
                if (onDelete != null) {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                formatInr(transaction.amountMinor),
                fontSize = 36.sp,
                fontWeight = FontWeight.W300,
                letterSpacing = (-0.5).sp,
                color = when (transaction.type) {
                    TransactionType.INCOME, TransactionType.REFUND -> Color(0xFF2A7D4F)
                    TransactionType.EXPENSE, TransactionType.CARD_PAYMENT,
                    TransactionType.FEE, TransactionType.CASH_WITHDRAWAL -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        item {
            DetailSection("Transaction") {
                DetailRow("Type", transaction.type.displayName())
                DetailRow("Status", transaction.status.displayName())
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    padding: PaddingValues,
    entities: List<FinancialEntity>,
    transactions: List<LedgerTransaction> = emptyList(),
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

    // Seed merchants + user's own past merchants, deduped, sorted alphabetically
    val knownMerchants = remember(transactions) {
        val fromTxns = transactions.mapNotNull { it.merchantName }
        (fromTxns + SEED_MERCHANTS)
            .groupBy { it.lowercase() }
            .map { (_, names) -> names.first() }
            .sortedBy { it.lowercase() }
    }

    LaunchedEffect(entities) {
        if (source.isBlank()) source = entities.firstOrNull()?.id.orEmpty()
    }

    Column(Modifier.fillMaxSize().padding(padding)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary)
            }
            Text("Add transaction", style = MaterialTheme.typography.titleMedium)
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("TYPE", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
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
                Text("AMOUNT", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
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
                Text("MERCHANT / TITLE", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                MerchantField(
                    value = title,
                    onValueChange = { title = it; error = null },
                    label = if (type == TransactionType.INCOME) "Income source" else "Merchant / title",
                    suggestions = knownMerchants,
                    enabled = !saving
                )
            }
            item {
                Text("SOURCE ACCOUNT", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
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
                    Text("DESTINATION ACCOUNT", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                    Spacer(Modifier.height(4.dp))
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
                    modifier = Modifier.fillMaxWidth().testTag("save-transaction")
                ) { Text(if (saving) "Saving…" else "Save transaction") }
            }
        }
    }
}

@Composable
fun EditTransactionScreen(
    padding: PaddingValues,
    transaction: LedgerTransaction?,
    entities: List<FinancialEntity>,
    transactions: List<LedgerTransaction> = emptyList(),
    onSave: (TransactionType, String, String, String, String?, (String?) -> Unit) -> Unit,
    onCancel: () -> Unit
) {
    if (transaction == null) {
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            IconButton(onClick = onCancel) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Transaction not found.", style = MaterialTheme.typography.headlineSmall)
        }
        return
    }

    var type by remember { mutableStateOf(transaction.type) }
    var amount by remember {
        mutableStateOf(
            BigDecimal(transaction.amountMinor).movePointLeft(2).stripTrailingZeros().toPlainString()
        )
    }
    var title by remember { mutableStateOf(transaction.merchantName.orEmpty()) }
    var source by remember { mutableStateOf(transaction.sourceEntityId) }
    var destination by remember { mutableStateOf(transaction.destinationEntityId) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    val knownMerchants = remember(transactions) {
        val fromTxns = transactions.mapNotNull { it.merchantName }
        (fromTxns + SEED_MERCHANTS)
            .groupBy { it.lowercase() }
            .map { (_, names) -> names.first() }
            .sortedBy { it.lowercase() }
    }

    Column(Modifier.fillMaxSize().padding(padding)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary)
            }
            Text("Edit transaction", style = MaterialTheme.typography.titleMedium)
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("TYPE", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
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
                Text("AMOUNT", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    amount,
                    { amount = it; error = null },
                    label = { Text("Amount (₹)") },
                    singleLine = true,
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Text("MERCHANT / TITLE", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                MerchantField(
                    value = title,
                    onValueChange = { title = it; error = null },
                    label = if (type == TransactionType.INCOME) "Income source" else "Merchant / title",
                    suggestions = knownMerchants,
                    enabled = !saving
                )
            }
            item {
                Text("SOURCE ACCOUNT", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
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
                    Text("DESTINATION ACCOUNT", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                    Spacer(Modifier.height(4.dp))
                    EntityDropdown(
                        label = "Destination",
                        entities = entities,
                        selectedId = destination,
                        onSelected = { destination = it; error = null },
                        enabled = !saving,
                        excludedId = source
                    )
                }
            }
            error?.let { msg ->
                item { Text(msg, color = MaterialTheme.colorScheme.error) }
            }
            item {
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
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (saving) "Saving…" else "Save changes") }
            }
        }
    }
}

@Composable
internal fun MerchantField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suggestions: List<String>,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var showPanel by remember { mutableStateOf(false) }

    val filtered = remember(value, suggestions) {
        if (value.isBlank()) emptyList()
        else suggestions
            .filter { it.contains(value, ignoreCase = true) }
            .sortedWith(compareByDescending<String> { it.startsWith(value, ignoreCase = true) }
                .thenBy { it.lowercase() })
            .take(6)
    }

    Column(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                showPanel = it.isNotBlank()
                onValueChange(it)
            },
            label = { Text(label) },
            singleLine = true,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("transaction-title")
                .onFocusChanged { state ->
                    if (!state.isFocused) {
                        // Delay so a tap on a suggestion row fires before panel hides
                        scope.launch {
                            delay(150)
                            showPanel = false
                        }
                    }
                }
        )
        if (showPanel && filtered.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 2.dp,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    filtered.forEachIndexed { i, merchant ->
                        if (i > 0) HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 0.5.dp
                        )
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showPanel = false
                                    onValueChange(merchant)
                                }
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                merchant,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
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

private fun TransactionStatus.displayName(): String = when (this) {
    TransactionStatus.PENDING -> "Pending"
    TransactionStatus.CONFIRMED -> "Confirmed"
    TransactionStatus.FAILED -> "Failed"
    TransactionStatus.CANCELLED -> "Cancelled"
}

private fun formatInr(amountMinor: Long): String =
    "₹" + BigDecimal(amountMinor).movePointLeft(2).setScale(2).toPlainString()
