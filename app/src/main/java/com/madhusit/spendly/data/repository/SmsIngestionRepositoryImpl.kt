package com.madhusit.spendly.data.repository

import androidx.room.withTransaction
import com.madhusit.spendly.data.local.SpendlyDatabase
import com.madhusit.spendly.data.local.sms.ProcessedSmsEventEntity
import com.madhusit.spendly.domain.ledger.*
import com.madhusit.spendly.domain.sms.*
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.flow.first

class SmsIngestionRepositoryImpl(
    private val database: SpendlyDatabase,
    private val ledgerRepository: LedgerRepository,
    private val eventDao: com.madhusit.spendly.data.local.sms.ProcessedSmsEventDao,
    private val parserEngine: SmsParserEngine = SmsParserEngine()
) : SmsIngestionRepository {

    override suspend fun process(message: SmsMessage): SmsParseResult = database.withTransaction {
        val fingerprint = fingerprint(message)
        val claimed = eventDao.claim(
            ProcessedSmsEventEntity(
                fingerprint = fingerprint,
                transactionId = null,
                status = "PROCESSING",
                createdAtEpochMillis = message.receivedAtEpochMillis
            )
        )
        if (claimed == -1L) {
            return@withTransaction SmsParseResult(
                SmsClassification.FINANCIAL,
                failureReason = SmsFailureReason.DUPLICATE
            )
        }

        val result = parserEngine.parse(message)
        val normalized = result.normalized
        if (normalized == null) {
            eventDao.updateResult(
                fingerprint,
                null,
                result.failureReason?.name ?: "NON_FINANCIAL"
            )
            return@withTransaction result
        }

        // Transfer/card-payment/reversal semantics require a destination or
        // relationship that this ingestion foundation does not resolve yet —
        // send to review queue rather than silently dropping.
        val needsReview = normalized.reviewRequired ||
            normalized.type == TransactionType.TRANSFER ||
            normalized.type == TransactionType.CARD_PAYMENT ||
            normalized.type == TransactionType.REVERSAL

        val entity = resolveEntity(normalized)
            ?: ledgerRepository.observeEntities().first().firstOrNull { it.active }

        if (needsReview || entity == null) {
            if (entity == null) {
                eventDao.updateResult(fingerprint, null, SmsFailureReason.AMBIGUOUS_ENTITY.name)
                return@withTransaction result.copy(failureReason = SmsFailureReason.AMBIGUOUS_ENTITY)
            }
            val reviewId = UUID.randomUUID().toString()
            val reviewTxn = LedgerTransaction(
                id = reviewId,
                sourceEntityId = entity.id,
                destinationEntityId = null,
                type = normalized.type,
                amountMinor = normalized.amountMinor,
                currency = normalized.currency,
                merchantName = normalized.merchantName,
                description = "Parsed from SMS – needs review",
                transactionTimestamp = normalized.transactionTimestamp,
                status = TransactionStatus.PENDING,
                referenceNumber = normalized.referenceNumber,
                upiReference = normalized.upiReference,
                rawEventReference = fingerprint,
                parserSource = normalized.parserSource,
                parserVersion = normalized.parserVersion,
                confidence = normalized.confidence,
                reviewRequired = true,
                createdAtEpochMillis = message.receivedAtEpochMillis,
                updatedAtEpochMillis = message.receivedAtEpochMillis
            )
            ledgerRepository.createTransaction(reviewTxn)
            eventDao.updateResult(fingerprint, reviewId, "REVIEW_PENDING")
            return@withTransaction result
        }

        val transactionId = UUID.randomUUID().toString()
        val status = if (message.body.contains(
                Regex("failed|declined|rejected", RegexOption.IGNORE_CASE)
            )
        ) {
            TransactionStatus.FAILED
        } else {
            TransactionStatus.CONFIRMED
        }

        val transaction = LedgerTransaction(
            id = transactionId,
            sourceEntityId = entity.id,
            destinationEntityId = null,
            type = normalized.type,
            amountMinor = normalized.amountMinor,
            currency = normalized.currency,
            merchantName = normalized.merchantName,
            description = "Imported from SMS",
            transactionTimestamp = normalized.transactionTimestamp,
            status = status,
            referenceNumber = normalized.referenceNumber,
            upiReference = normalized.upiReference,
            rawEventReference = fingerprint,
            parserSource = normalized.parserSource,
            parserVersion = normalized.parserVersion,
            confidence = normalized.confidence,
            reviewRequired = normalized.reviewRequired,
            createdAtEpochMillis = message.receivedAtEpochMillis,
            updatedAtEpochMillis = message.receivedAtEpochMillis
        )
        ledgerRepository.createTransaction(transaction)
        eventDao.updateResult(fingerprint, transactionId, "PROCESSED")
        result
    }

    private suspend fun resolveEntity(normalized: NormalizedSmsTransaction): FinancialEntity? {
        val entities = ledgerRepository.observeEntities().first().filter { it.active }
        val exact = entities.filter {
            normalized.provider != null &&
                it.provider.equals(normalized.provider, ignoreCase = true) &&
                normalized.lastFour != null &&
                it.lastFour == normalized.lastFour
        }
        if (exact.size == 1) return exact.single()
        val providerOnly = entities.filter {
            normalized.provider != null &&
                it.provider.equals(normalized.provider, ignoreCase = true)
        }
        if (providerOnly.size == 1) return providerOnly.single()
        if (entities.size == 1) return entities.single()
        return null
    }

    private fun fingerprint(message: SmsMessage): String {
        val canonical = message.sender.trim().lowercase() + "|" +
            message.body.trim().replace(Regex("\\s+"), " ") + "|" +
            message.receivedAtEpochMillis
        return MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
