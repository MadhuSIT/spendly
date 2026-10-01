package com.madhusit.spendly.domain.ledger

import java.util.UUID

class CreateManualTransaction(private val repository: LedgerRepository) {
    suspend operator fun invoke(
        sourceEntityId: String,
        type: TransactionType,
        amountMinor: Long,
        currency: String,
        transactionTimestamp: Long,
        destinationEntityId: String? = null,
        merchantName: String? = null,
        description: String? = null,
        referenceNumber: String? = null,
        upiReference: String? = null,
        reviewRequired: Boolean = false,
        nowEpochMillis: Long
    ): LedgerTransaction {
        require(type != TransactionType.REVERSAL) {
            "Create the reversal transaction and link it explicitly to the original."
        }
        val transaction = LedgerTransaction(
            id = UUID.randomUUID().toString(),
            sourceEntityId = sourceEntityId,
            destinationEntityId = destinationEntityId,
            type = type,
            amountMinor = amountMinor,
            currency = currency,
            merchantName = merchantName,
            description = description,
            transactionTimestamp = transactionTimestamp,
            status = TransactionStatus.CONFIRMED,
            referenceNumber = referenceNumber,
            upiReference = upiReference,
            rawEventReference = null,
            parserSource = null,
            parserVersion = null,
            confidence = 1.0,
            reviewRequired = reviewRequired,
            createdAtEpochMillis = nowEpochMillis,
            updatedAtEpochMillis = nowEpochMillis
        )
        repository.createTransaction(transaction)
        return transaction
    }
}