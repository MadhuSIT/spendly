package com.madhusit.spendly.domain.ledger

import java.util.UUID

class CreateFinancialEntity(private val repository: LedgerRepository) {
    suspend operator fun invoke(
        type: FinancialEntityType,
        name: String,
        currency: String = "INR",
        provider: String? = null,
        maskedIdentifier: String? = null,
        lastFour: String? = null,
        nowEpochMillis: Long
    ): FinancialEntity {
        val entity = FinancialEntity(
            id = UUID.randomUUID().toString(),
            type = type,
            provider = provider,
            name = name,
            maskedIdentifier = maskedIdentifier,
            lastFour = lastFour,
            currency = currency,
            createdAtEpochMillis = nowEpochMillis,
            updatedAtEpochMillis = nowEpochMillis
        )
        repository.createEntity(entity)
        return entity
    }
}