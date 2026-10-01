package com.madhusit.spendly.domain.ledger

data class FinancialEntity(
    val id: String,
    val type: FinancialEntityType,
    val provider: String?,
    val name: String,
    val maskedIdentifier: String?,
    val lastFour: String?,
    val currency: String,
    val active: Boolean = true,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)

data class LedgerTransaction(
    val id: String,
    val sourceEntityId: String,
    val destinationEntityId: String?,
    val type: TransactionType,
    val amountMinor: Long,
    val currency: String,
    val merchantName: String?,
    val description: String?,
    val transactionTimestamp: Long,
    val status: TransactionStatus,
    val referenceNumber: String?,
    val upiReference: String?,
    val rawEventReference: String?,
    val parserSource: String?,
    val parserVersion: String?,
    val confidence: Double?,
    val reviewRequired: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)

data class TransactionRelationship(
    val id: String,
    val fromTransactionId: String,
    val toTransactionId: String,
    val relationshipType: String,
    val createdAtEpochMillis: Long
)

data class LedgerTotals(
    val incomeMinor: Long,
    val grossSpendingMinor: Long,
    val refundsMinor: Long,
    val netSpendingMinor: Long
)