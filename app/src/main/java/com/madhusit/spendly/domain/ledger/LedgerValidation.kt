package com.madhusit.spendly.domain.ledger

object LedgerValidation {
    fun validateEntity(entity: FinancialEntity) {
        require(entity.id.isNotBlank()) { "Entity id must not be blank." }
        require(entity.name.isNotBlank()) { "Entity name must not be blank." }
        require(entity.currency.matches(Regex("[A-Z]{3}"))) { "Currency must be a 3-letter ISO code." }
        require(entity.lastFour == null || entity.lastFour.matches(Regex("\\d{4}"))) {
            "Last four must contain exactly four digits."
        }
    }

    fun validateTransaction(transaction: LedgerTransaction) {
        require(transaction.id.isNotBlank()) { "Transaction id must not be blank." }
        require(transaction.sourceEntityId.isNotBlank()) { "Source entity is required." }
        require(transaction.amountMinor > 0) { "Transaction amount must be positive." }
        require(transaction.currency.matches(Regex("[A-Z]{3}"))) { "Currency must be a 3-letter ISO code." }
        require(transaction.transactionTimestamp >= 0) { "Transaction timestamp must be non-negative." }
        require(transaction.type != TransactionType.REVERSAL || transaction.destinationEntityId == null) {
            "Reversals are linked to transactions, not financial entities."
        }
        if (transaction.type == TransactionType.TRANSFER || transaction.type == TransactionType.CARD_PAYMENT) {
            require(!transaction.destinationEntityId.isNullOrBlank()) { "Destination entity is required." }
            require(transaction.destinationEntityId != transaction.sourceEntityId) {
                "Source and destination entities must differ."
            }
        }
        require(transaction.confidence == null || transaction.confidence in 0.0..1.0) {
            "Confidence must be between 0 and 1."
        }
    }
}