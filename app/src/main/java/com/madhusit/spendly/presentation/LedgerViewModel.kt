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

    private val createEntity = CreateFinancialEntity(repository)
    private val createTransaction = CreateManualTransaction(repository)
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

    fun addEntity(
        type: FinancialEntityType,
        name: String,
        maskedIdentifier: String?,
        lastFour: String?,
        onComplete: (String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                require(name.isNotBlank()) { "Enter a name for this account." }
                require(lastFour == null || lastFour.matches(Regex("\\d{4}"))) {
                    "Last four digits must be exactly four digits."
                }
                createEntity(
                    type = type,
                    name = name,
                    currency = "INR",
                    maskedIdentifier = maskedIdentifier?.ifBlank { null },
                    lastFour = lastFour?.ifBlank { null },
                    nowEpochMillis = System.currentTimeMillis()
                )
                onComplete(null)
            } catch (e: Exception) {
                onComplete(e.message ?: "Could not save account.")
            }
        }
    }

    fun addTransaction(
        type: TransactionType,
        amountRupees: String,
        title: String,
        sourceEntityId: String,
        destinationEntityId: String?,
        onComplete: (String?) -> Unit
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
                createTransaction(
                    sourceEntityId = sourceEntityId,
                    destinationEntityId = destinationEntityId,
                    type = type,
                    amountMinor = amountMinor,
                    currency = "INR",
                    transactionTimestamp = now,
                    merchantName = title.ifBlank { null },
                    nowEpochMillis = now
                )
                onComplete(null)
            } catch (e: Exception) {
                onComplete(e.message ?: "Could not save transaction.")
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
