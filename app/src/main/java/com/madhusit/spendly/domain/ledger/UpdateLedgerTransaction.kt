package com.madhusit.spendly.domain.ledger

class UpdateLedgerTransaction(private val repository: LedgerRepository) {
    suspend operator fun invoke(transaction: LedgerTransaction, nowEpochMillis: Long): LedgerTransaction {
        val updated = transaction.copy(updatedAtEpochMillis = nowEpochMillis)
        repository.updateTransaction(updated)
        return updated
    }
}