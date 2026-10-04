package com.madhusit.spendly.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madhusit.spendly.domain.ledger.*
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

private enum class InsightsPeriod(val label: String) {
    THIS_MONTH("This month"),
    LAST_MONTH("Last month"),
    ALL_TIME("All time"),
    CUSTOM("Custom")
}

private val chartColors = listOf(
    Color(0xFFE53935),
    Color(0xFFE91E63),
    Color(0xFFFF7043),
    Color(0xFFFF9800),
    Color(0xFF9C27B0),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    padding: PaddingValues,
    transactions: List<LedgerTransaction>,
    entities: List<FinancialEntity>
) {
    var period by remember { mutableStateOf(InsightsPeriod.THIS_MONTH) }
    var showDatePicker by remember { mutableStateOf(false) }
    var customStart by remember { mutableStateOf<Long?>(null) }
    var customEnd by remember { mutableStateOf<Long?>(null) }
    val dateRangeState = rememberDateRangePickerState()

    val filtered = remember(transactions, period, customStart, customEnd) {
        val confirmed = transactions.filter { it.status == TransactionStatus.CONFIRMED && !it.reviewRequired }
        when (period) {
            InsightsPeriod.THIS_MONTH -> {
                val cal = Calendar.getInstance()
                val y = cal.get(Calendar.YEAR); val m = cal.get(Calendar.MONTH)
                confirmed.filter {
                    Calendar.getInstance().also { c -> c.timeInMillis = it.transactionTimestamp }
                        .let { c -> c.get(Calendar.YEAR) == y && c.get(Calendar.MONTH) == m }
                }
            }
            InsightsPeriod.LAST_MONTH -> {
                val cal = Calendar.getInstance().also { it.add(Calendar.MONTH, -1) }
                val y = cal.get(Calendar.YEAR); val m = cal.get(Calendar.MONTH)
                confirmed.filter {
                    Calendar.getInstance().also { c -> c.timeInMillis = it.transactionTimestamp }
                        .let { c -> c.get(Calendar.YEAR) == y && c.get(Calendar.MONTH) == m }
                }
            }
            InsightsPeriod.ALL_TIME -> confirmed
            InsightsPeriod.CUSTOM -> {
                val s = customStart; val e = customEnd
                if (s != null && e != null) confirmed.filter { it.transactionTimestamp in s..(e + 86_400_000L) }
                else confirmed
            }
        }
    }

    val expenseTypes = setOf(
        TransactionType.EXPENSE, TransactionType.CARD_PAYMENT,
        TransactionType.FEE, TransactionType.CASH_WITHDRAWAL
    )
    val totalExpenses = remember(filtered) { filtered.filter { it.type in expenseTypes }.sumOf { it.amountMinor } }
    val totalIncome = remember(filtered) { filtered.filter { it.type == TransactionType.INCOME || it.type == TransactionType.REFUND }.sumOf { it.amountMinor } }
    val spendingByType = remember(filtered) {
        filtered.filter { it.type in expenseTypes }
            .groupBy { it.type }
            .mapValues { (_, t) -> t.sumOf { it.amountMinor } }
            .entries.sortedByDescending { it.value }
    }
    val topMerchants = remember(filtered) {
        filtered.filter { it.merchantName != null && it.type in expenseTypes }
            .groupBy { it.merchantName!! }
            .mapValues { (_, t) -> t.sumOf { it.amountMinor } }
            .entries.sortedByDescending { it.value }.take(5)
    }
    val byAccount = remember(filtered, entities) {
        entities.map { entity ->
            val spent = filtered.filter { it.sourceEntityId == entity.id && it.type in expenseTypes }.sumOf { it.amountMinor }
            entity to spent
        }.filter { (_, v) -> v > 0 }.sortedByDescending { (_, v) -> v }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
                if (customStart == null) period = InsightsPeriod.THIS_MONTH
            },
            confirmButton = {
                TextButton(onClick = {
                    customStart = dateRangeState.selectedStartDateMillis
                    customEnd = dateRangeState.selectedEndDateMillis ?: dateRangeState.selectedStartDateMillis
                    showDatePicker = false
                }) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    if (customStart == null) period = InsightsPeriod.THIS_MONTH
                }) { Text("Cancel") }
            }
        ) {
            DateRangePicker(state = dateRangeState, modifier = Modifier.height(500.dp))
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { Text("Insights", style = MaterialTheme.typography.headlineMedium) }

        item {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InsightsPeriod.entries.forEach { p ->
                    FilterChip(
                        selected = period == p,
                        onClick = {
                            period = p
                            if (p == InsightsPeriod.CUSTOM) showDatePicker = true
                        },
                        label = {
                            if (p == InsightsPeriod.CUSTOM && customStart != null) {
                                val fmt = SimpleDateFormat("d MMM", Locale.getDefault())
                                val start = customStart ?: 0L
                                Text(
                                    "${fmt.format(Date(start))}${customEnd?.let { " – ${fmt.format(Date(it))}" } ?: ""}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            } else {
                                Text(p.label, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    )
                }
            }
        }

        if (period == InsightsPeriod.CUSTOM && customStart != null) {
            item {
                TextButton(
                    onClick = { showDatePicker = true },
                    contentPadding = PaddingValues(0.dp)
                ) { Text("Change date range", style = MaterialTheme.typography.labelMedium) }
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("SPENT", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                        Text(insightsFmt(totalExpenses), fontSize = 28.sp, fontWeight = FontWeight.W300,
                            color = MaterialTheme.colorScheme.error, letterSpacing = (-0.5).sp)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.End) {
                        Text("EARNED", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                        Text(insightsFmt(totalIncome), fontSize = 28.sp, fontWeight = FontWeight.W300,
                            color = Color(0xFF2A7D4F), letterSpacing = (-0.5).sp)
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("No data for this period", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Add or confirm transactions to see insights.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            if (spendingByType.isNotEmpty()) {
                item {
                    Text("SPENDING BREAKDOWN", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                }
                item {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            DonutChart(
                                segments = spendingByType.mapIndexed { i, (_, v) -> v to chartColors[i % chartColors.size] },
                                centerLabel = insightsFmt(totalExpenses),
                                modifier = Modifier.fillMaxWidth().height(200.dp)
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                spendingByType.forEachIndexed { i, (type, amount) ->
                                    SpendingBar(
                                        label = type.insightsLabel(),
                                        amount = amount,
                                        total = totalExpenses,
                                        color = chartColors[i % chartColors.size]
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (topMerchants.isNotEmpty()) {
                item {
                    Text("TOP MERCHANTS", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                }
                item {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            topMerchants.forEachIndexed { i, (merchant, amount) ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = MaterialTheme.shapes.small,
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text("${i + 1}", style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                        }
                                        Text(merchant, style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f, fill = false))
                                    }
                                    Text(insightsFmt(amount), style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.W600, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            if (byAccount.isNotEmpty()) {
                item {
                    Text("BY ACCOUNT", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                }
                item {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            val maxAmount = byAccount.maxOf { (_, v) -> v }
                            byAccount.forEach { (entity, amount) ->
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                            Text(entity.name, style = MaterialTheme.typography.bodyMedium)
                                            Text(entity.type.insightsLabel(), style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text(insightsFmt(amount), style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.W600, color = MaterialTheme.colorScheme.error)
                                    }
                                    ThinBar(fraction = if (maxAmount > 0) amount.toFloat() / maxAmount.toFloat() else 0f)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DonutChart(
    segments: List<Pair<Long, Color>>,
    centerLabel: String,
    modifier: Modifier = Modifier
) {
    val total = segments.sumOf { it.first }.toFloat().coerceAtLeast(1f)
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokeWidth = 38.dp.toPx()
            val diameter = minOf(size.width, size.height) - strokeWidth - 4.dp.toPx()
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            // Track
            drawArc(color = Color(0xFFE8EDF5), startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(width = strokeWidth, cap = StrokeCap.Butt))
            var startAngle = -90f
            segments.forEach { (value, color) ->
                val sweep = (value / total) * 360f
                val gap = if (segments.size > 1) 2f else 0f
                drawArc(color = color, startAngle = startAngle + gap / 2f, sweepAngle = sweep - gap,
                    useCenter = false, topLeft = topLeft, size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt))
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("TOTAL", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.8.sp)
            Text(centerLabel, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.W600, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun SpendingBar(label: String, amount: Long, total: Long, color: Color) {
    val fraction = if (total > 0) amount.toFloat() / total.toFloat() else 0f
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(8.dp)) { drawCircle(color = color) }
                Text(label, style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("%.0f%%".format(Locale.US, fraction * 100),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(insightsFmt(amount), style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.W600, color = color)
            }
        }
        ThinBar(fraction = fraction, color = color.copy(alpha = 0.7f))
    }
}

@Composable
private fun ThinBar(fraction: Float, color: Color = Color(0xFFE53935).copy(alpha = 0.7f)) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Canvas(Modifier.fillMaxWidth().height(8.dp)) {
        drawRoundRect(color = trackColor, size = size, cornerRadius = CornerRadius(4.dp.toPx()))
        if (fraction > 0f) {
            drawRoundRect(color = color, size = Size(size.width * fraction.coerceIn(0f, 1f), size.height),
                cornerRadius = CornerRadius(4.dp.toPx()))
        }
    }
}

private fun insightsFmt(amountMinor: Long): String =
    "₹" + BigDecimal(amountMinor).movePointLeft(2).setScale(2).toPlainString()

private fun TransactionType.insightsLabel(): String = when (this) {
    TransactionType.EXPENSE -> "Expense"
    TransactionType.INCOME -> "Income"
    TransactionType.TRANSFER -> "Transfer"
    TransactionType.CARD_PAYMENT -> "Card payment"
    TransactionType.REFUND -> "Refund"
    TransactionType.FEE -> "Fee"
    TransactionType.CASH_WITHDRAWAL -> "Cash withdrawal"
    TransactionType.REVERSAL -> "Reversal"
}

private fun FinancialEntityType.insightsLabel(): String = when (this) {
    FinancialEntityType.BANK_ACCOUNT -> "Bank account"
    FinancialEntityType.CREDIT_CARD -> "Credit card"
    FinancialEntityType.DEBIT_CARD -> "Debit card"
    FinancialEntityType.CASH -> "Cash"
    FinancialEntityType.OTHER -> "Other"
}
