package com.madhusit.spendly.domain.ledger

class CalculateLedgerTotals(private val repository: LedgerRepository) {
    suspend operator fun invoke(): LedgerTotals = repository.calculateTotals()
}