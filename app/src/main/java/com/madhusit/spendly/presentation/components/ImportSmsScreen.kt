package com.madhusit.spendly.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.madhusit.spendly.domain.sms.SmsInboxScanSummary
import java.text.SimpleDateFormat
import java.util.*

private val displayFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportSmsScreen(
    padding: PaddingValues,
    smsPermissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onScan: (fromMs: Long, toMs: Long, onResult: (SmsInboxScanSummary?, String?) -> Unit) -> Unit,
    onBack: () -> Unit,
    onOpenQueue: () -> Unit
) {
    val now = remember { System.currentTimeMillis() }
    var fromMs by remember { mutableLongStateOf(now - 30L * 24 * 60 * 60 * 1000) }
    var toMs by remember { mutableLongStateOf(now) }

    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    var scanning by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<SmsInboxScanSummary?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val fromPickerState = rememberDatePickerState(
        initialSelectedDateMillis = fromMs,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= toMs
        }
    )
    val toPickerState = rememberDatePickerState(
        initialSelectedDateMillis = toMs,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= fromMs && utcTimeMillis <= now
        }
    )

    if (showFromPicker) {
        DatePickerDialog(
            onDismissRequest = { showFromPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    fromPickerState.selectedDateMillis?.let { fromMs = it }
                    result = null; error = null
                    showFromPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showFromPicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = fromPickerState) }
    }

    if (showToPicker) {
        DatePickerDialog(
            onDismissRequest = { showToPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    toPickerState.selectedDateMillis?.let { toMs = it }
                    result = null; error = null
                    showToPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showToPicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = toPickerState) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary)
            }
            Text("Import from SMS", style = MaterialTheme.typography.titleMedium)
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Description
            Text(
                "Spendly will scan your SMS inbox for the selected period and automatically parse bank and payment messages into transactions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Permission warning
            if (!smsPermissionGranted) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "SMS permission required",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            "Grant Read SMS permission so Spendly can scan your inbox.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        FilledTonalButton(onClick = onRequestPermission) {
                            Text("Grant permission")
                        }
                    }
                }
            }

            // Date range section
            Text(
                "SELECT PERIOD",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = androidx.compose.ui.unit.TextUnit(1f, androidx.compose.ui.unit.TextUnitType.Sp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                FilterChip(
                    selected = false,
                    onClick = { if (!scanning) showFromPicker = true },
                    label = { Text(displayFmt.format(Date(fromMs))) }
                )
                Text("–", color = MaterialTheme.colorScheme.onSurfaceVariant)
                FilterChip(
                    selected = false,
                    onClick = { if (!scanning) showToPicker = true },
                    label = { Text(displayFmt.format(Date(toMs))) }
                )
            }

            // Quick-select chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "7 days" to 7L,
                    "30 days" to 30L,
                    "90 days" to 90L,
                    "6 months" to 180L
                ).forEach { (label, days) ->
                    val chipFrom = now - days * 24 * 60 * 60 * 1000
                    FilterChip(
                        selected = fromMs == chipFrom && toMs == now,
                        onClick = {
                            if (!scanning) {
                                fromMs = chipFrom; toMs = now
                                result = null; error = null
                            }
                        },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // Scan button
            Button(
                onClick = {
                    if (!smsPermissionGranted) { onRequestPermission(); return@Button }
                    scanning = true; result = null; error = null
                    val toEndOfDay = toMs + 24 * 60 * 60 * 1000 - 1
                    onScan(fromMs, toEndOfDay) { summary, err ->
                        scanning = false
                        result = summary
                        error = err
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !scanning
            ) {
                if (scanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Scanning…")
                } else {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Import transactions")
                }
            }

            // Error
            error?.let { err ->
                Text(err, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }

            // Results card
            AnimatedVisibility(visible = result != null, enter = fadeIn(), exit = fadeOut()) {
                result?.let { s ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("Scan complete",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold)

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                ResultStat(Modifier.weight(1f), label = "Scanned", value = "${s.total}")
                                ResultStat(Modifier.weight(1f), label = "Imported", value = "${s.saved}",
                                    color = MaterialTheme.colorScheme.primary)
                                ResultStat(Modifier.weight(1f), label = "Need review", value = "${s.review}",
                                    color = if (s.review > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            if (s.saved == 0 && s.total > 0) {
                                Text(
                                    "All ${s.total} messages were already imported or didn't match any bank/payment pattern.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (s.total == 0) {
                                Text(
                                    "No messages found in this date range. Try extending the period.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (s.review > 0) {
                                FilledTonalButton(
                                    onClick = onOpenQueue,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Review ${s.review} transaction${if (s.review == 1) "" else "s"}")
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
private fun ResultStat(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
