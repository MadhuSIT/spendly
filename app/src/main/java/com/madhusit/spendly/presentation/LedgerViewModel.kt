package com.madhusit.spendly.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.madhusit.spendly.domain.ledger.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LedgerViewModel(private val repository: LedgerRepository) : ViewModel() {
    val transactions: StateFlow<List<LedgerTransaction>> = repository.observeTransactions().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val totals: StateFlow<LedgerTotals> = repository.observeTransactions().map { repository.calculateTotals() }.stateIn(viewModelScope, SharingStarted.Eagerly, LedgerTotals(0, 0, 0, 0))
    private val createEntity = CreateFinancialEntity(repository)
    private val createTransaction = CreateManualTransaction(repository)
    private var initialized = false

    fun ensureDefaultEntity() {
        if (initialized) return
        initialized = true
        viewModelScope.launch {
            if (repository.observeEntities().first().isEmpty()) createEntity(type = FinancialEntityType.CASH, name = "Cash Wallet", provider = "Spendly", currency = "INR", nowEpochMillis = System.currentTimeMillis())
        }
    }

    fun addExpense(amountRupees: String, merchant: String, onComplete: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val amountMinor = amountRupees.toBigDecimal().movePointRight(2).longValueExact()
                require(amountMinor > 0) { "Enter an amount greater than zero." }
                val entity = repository.observeEntities().first().firstOrNull() ?: createEntity(FinancialEntityType.CASH, "Spendly", "Cash Wallet", null, null, "INR", System.currentTimeMillis()).let { repository.observeEntities().first().first() }
                createTransaction(sourceEntityId = entity.id, type = TransactionType.EXPENSE, amountMinor = amountMinor, currency = "INR", transactionTimestamp = System.currentTimeMillis(), merchantName = merchant.ifBlank { null }, nowEpochMillis = System.currentTimeMillis())
                onComplete(null)
            } catch (e: Exception) { onComplete(e.message ?: "Could not save transaction.") }
        }
    }

    companion object {
        fun factory(repository: LedgerRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = LedgerViewModel(repository) as T
        }
    }
}
