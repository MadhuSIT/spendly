package com.madhusit.spendly.domain.ledger

import java.util.UUID

class LinkTransactionRelationship(private val repository: LedgerRepository) {
    suspend operator fun invoke(
        fromTransactionId: String,
        toTransactionId: String,
        relationshipType: String,
        nowEpochMillis: Long
    ): TransactionRelationship {
        val relationship = TransactionRelationship(
            id = UUID.randomUUID().toString(),
            fromTransactionId = fromTransactionId,
            toTransactionId = toTransactionId,
            relationshipType = relationshipType,
            createdAtEpochMillis = nowEpochMillis
        )
        repository.createRelationship(relationship)
        return relationship
    }
}