package com.madhusit.spendly.domain.ledger

object LedgerValidation {
    fun validateEntity(entity: FinancialEntity) {
        require(entity.id.isNotBlank()) { "Entity id must not be blank." }
        require(entity.name.isNotBlank()) { "Entity name must not be blank." }
        require(entity.currency.matches(Regex("[A-Z]{3}"))) { "Currency must be a 3-letter ISO code." }
        require(entity.amountSafeIdentifier()) { "Masked identifier must not contain unmasked sensitive data." }
    }

    fun validateTransaction(transaction: LedgerTransaction) {
        require(transaction.id.isNotBlank()) { "Transaction id must not be blank." }
        require(transaction.sourceEntityId.isNotBlank()) { "Source entity is required." }
        require(transaction.amountMinor > 0) { "Transaction amount must be positive." }
        require(transaction.currency.matches(Regex("[A-Z]{3}"))) { "Currency must be a 3-letter ISO code." }
        require(transaction.transactionTimestamp >= 0) { "Transaction timestamp must be non-negative." }
        if (transaction.type == TransactionType.TRANSFER || transaction.type == TransactionType.CARD_PAYMENT) {
            require(!transaction.destinationEntityId.isNullOrBlank()) { "Destination entity is required." }
        }
        require(transaction.type != TransactionType.REVERSAL || !transaction.destinationEntityId.isNullOrBlank()) {
            "A reversal must identify the related transaction through a relationship or destination."
        }
        require(transaction.confidence == null || transaction.confidence in 0.0..1.0) {
            "Confidence must be between 0 and 1."
        }
    }

    private fun FinancialEntity.amountSafeIdentifier(): Boolean {
        val identifier = maskedIdentifier ?: return true
        return !identifier.matches(Regex(".*\\b\\d{8,}\\b.*"))
    }
}