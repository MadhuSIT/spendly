package com.madhusit.spendly.domain.sms

import com.madhusit.spendly.domain.ledger.FinancialEntityType
import com.madhusit.spendly.domain.ledger.TransactionCategory
import com.madhusit.spendly.domain.ledger.TransactionType

enum class SmsClassification { FINANCIAL, NON_FINANCIAL, AMBIGUOUS }

enum class SmsFailureReason {
    UNSUPPORTED_PROVIDER, UNPARSEABLE, AMBIGUOUS_ENTITY, AMBIGUOUS_TRANSACTION, DUPLICATE, INVALID_NORMALIZED_RESULT
}

data class SmsMessage(
    val sender: String,
    val body: String,
    val receivedAtEpochMillis: Long
)

data class NormalizedSmsTransaction(
    val type: TransactionType,
    val amountMinor: Long,
    val currency: String,
    val merchantName: String?,
    val provider: String?,
    val maskedIdentifier: String?,
    val lastFour: String?,
    val transactionTimestamp: Long,
    val referenceNumber: String?,
    val upiReference: String?,
    val parserSource: String,
    val parserVersion: String,
    val confidence: Double,
    val category: TransactionCategory,
    val entityType: FinancialEntityType,
    val reviewRequired: Boolean = false
)

data class SmsParseResult(
    val classification: SmsClassification,
    val normalized: NormalizedSmsTransaction? = null,
    val failureReason: SmsFailureReason? = null
)
