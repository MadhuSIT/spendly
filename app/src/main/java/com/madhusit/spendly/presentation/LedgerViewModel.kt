package com.madhusit.spendly.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.madhusit.spendly.domain.ledger.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LedgerViewModel(private val repository: LedgerRepository) : ViewModel() {
    val transactions: StateFlow<List<LedgerTransaction>> = repository.observeTransactions()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val entities: StateFlow<List<FinancialEntity>> = repository.observeEntities()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val totals: StateFlow<LedgerTotals> = repository.observeTransactions()
        .map { repository.calculateTotals() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, LedgerTotals(0, 0, 0, 0))
    val reviewQueue: StateFlow<List<LedgerTransaction>> = repository.observeTransactions()
        .map { list -> list.filter { it.reviewRequired && it.status == TransactionStatus.PENDING } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val createEntity = CreateFinancialEntity(repository)
    private val createTransaction = CreateManualTransaction(repository)
    private val updateTransaction = UpdateLedgerTransaction(repository)
    private var initialized = false

    fun ensureDefaultEntity() {
        if (initialized) return
        initialized = true
        viewModelScope.launch { ensureCashWallet() }
    }

    private suspend fun ensureCashWallet(): FinancialEntity =
        repository.observeEntities().first().firstOrNull()
            ?: createEntity(
                type = FinancialEntityType.CASH,
                name = "Cash Wallet",
                currency = "INR",
                provider = "Spendly",
                nowEpochMillis = System.currentTimeMillis()
            )

    fun deleteTransaction(id: String, onComplete: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteTransaction(id)
                onComplete(null)
            } catch (e: Exception) {
                onComplete(e.message ?: "Could not delete transaction.")
            }
        }
    }

    fun addEntity(
        type: FinancialEntityType,
        name: String,
        maskedIdentifier: String?,
        lastFour: String?,
        onComplete: (String?, FinancialEntity?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                require(name.isNotBlank()) { "Enter a name for this account." }
                require(lastFour == null || lastFour.matches(Regex("\\d{4}"))) {
                    "Last four digits must be exactly four digits."
                }
                val entity = createEntity(
                    type = type,
                    name = name,
                    currency = "INR",
                    maskedIdentifier = maskedIdentifier?.ifBlank { null },
                    lastFour = lastFour?.ifBlank { null },
                    nowEpochMillis = System.currentTimeMillis()
                )
                onComplete(null, entity)
            } catch (e: Exception) {
                onComplete(e.message ?: "Could not save account.", null)
            }
        }
    }

    fun confirmQueueItem(
        transactionId: String,
        merchantName: String?,
        type: TransactionType,
        sourceEntityId: String,
        onComplete: (String?, LedgerTransaction?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val txn = repository.findTransaction(transactionId)
                    ?: error("Transaction not found.")
                require(sourceEntityId.isNotBlank()) { "Choose an account." }
                val now = System.currentTimeMillis()
                val updated = txn.copy(
                    merchantName = merchantName?.ifBlank { null } ?: txn.merchantName,
                    type = type,
                    sourceEntityId = sourceEntityId,
                    status = TransactionStatus.CONFIRMED,
                    reviewRequired = false,
                    updatedAtEpochMillis = now
                )
                updateTransaction(updated, nowEpochMillis = now)
                onComplete(null, updated)
            } catch (e: Exception) {
                onComplete(e.message ?: "Could not confirm transaction.", null)
            }
        }
    }

    fun seedReviewItem(sourceEntityId: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val id = java.util.UUID.randomUUID().toString()
            repository.createTransaction(
                LedgerTransaction(
                    id = id,
                    sourceEntityId = sourceEntityId,
                    destinationEntityId = null,
                    type = TransactionType.EXPENSE,
                    amountMinor = 49900L,
                    currency = "INR",
                    merchantName = null,
                    description = "Parsed from SMS",
                    transactionTimestamp = now,
                    status = TransactionStatus.PENDING,
                    referenceNumber = null,
                    upiReference = null,
                    rawEventReference = "sms:demo:$id",
                    parserSource = "GenericSmsParser",
                    parserVersion = "1",
                    confidence = 0.6,
                    reviewRequired = true,
                    category = null,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now
                )
            )
        }
    }

    fun editTransaction(
        transactionId: String,
        type: TransactionType,
        amountRupees: String,
        title: String,
        sourceEntityId: String,
        destinationEntityId: String?,
        onComplete: (String?, LedgerTransaction?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val existing = repository.findTransaction(transactionId)
                    ?: error("Transaction not found.")
                val amountMinor = amountRupees.toBigDecimal().movePointRight(2).longValueExact()
                require(amountMinor > 0) { "Enter an amount greater than zero." }
                require(sourceEntityId.isNotBlank()) { "Choose a source." }
                if (type == TransactionType.TRANSFER) {
                    require(!destinationEntityId.isNullOrBlank()) { "Choose a destination." }
                    require(destinationEntityId != sourceEntityId) { "Source and destination must be different." }
                }
                val now = System.currentTimeMillis()
                val updated = existing.copy(
                    type = type,
                    amountMinor = amountMinor,
                    merchantName = title.ifBlank { null },
                    sourceEntityId = sourceEntityId,
                    destinationEntityId = if (type == TransactionType.TRANSFER) destinationEntityId else null,
                    updatedAtEpochMillis = now
                )
                updateTransaction(updated, nowEpochMillis = now)
                onComplete(null, updated)
            } catch (e: Exception) {
                onComplete(e.message ?: "Could not update transaction.", null)
            }
        }
    }

    fun addTransaction(
        type: TransactionType,
        amountRupees: String,
        title: String,
        sourceEntityId: String,
        destinationEntityId: String?,
        onComplete: (String?, LedgerTransaction?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val amountMinor = amountRupees.toBigDecimal().movePointRight(2).longValueExact()
                require(amountMinor > 0) { "Enter an amount greater than zero." }
                require(sourceEntityId.isNotBlank()) { "Choose a source." }
                if (type == TransactionType.TRANSFER) {
                    require(!destinationEntityId.isNullOrBlank()) { "Choose a destination." }
                    require(destinationEntityId != sourceEntityId) { "Source and destination must be different." }
                }
                val now = System.currentTimeMillis()
                val txn = createTransaction(
                    sourceEntityId = sourceEntityId,
                    destinationEntityId = destinationEntityId,
                    type = type,
                    amountMinor = amountMinor,
                    currency = "INR",
                    transactionTimestamp = now,
                    merchantName = title.ifBlank { null },
                    nowEpochMillis = now
                )
                onComplete(null, txn)
            } catch (e: Exception) {
                onComplete(e.message ?: "Could not save transaction.", null)
            }
        }
    }

    fun bulkApproveQueueItems(ids: List<String>, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                ids.forEach { id ->
                    val txn = repository.findTransaction(id) ?: return@forEach
                    val updated = txn.copy(
                        status = TransactionStatus.CONFIRMED,
                        reviewRequired = false,
                        updatedAtEpochMillis = System.currentTimeMillis()
                    )
                    updateTransaction(updated, nowEpochMillis = updated.updatedAtEpochMillis)
                }
                onDone(null)
            } catch (e: Exception) {
                onDone(e.message ?: "Could not approve transactions.")
            }
        }
    }

    fun bulkDeleteTransactions(ids: List<String>, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteTransactions(ids)
                onDone(null)
            } catch (e: Exception) {
                onDone(e.message ?: "Could not delete transactions.")
            }
        }
    }

    companion object {
        fun factory(repository: LedgerRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                LedgerViewModel(repository) as T
        }
    }
}
