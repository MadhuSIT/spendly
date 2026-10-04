package com.madhusit.spendly.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.room.withTransaction
import com.madhusit.spendly.data.local.SpendlyDatabase
import com.madhusit.spendly.data.local.sms.ProcessedSmsEventEntity
import com.madhusit.spendly.domain.ledger.*
import com.madhusit.spendly.domain.sms.*
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.flow.first

private const val TAG = "Spendly.SmsIngest"

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

    override suspend fun clearProcessedEvents() {
        eventDao.deleteAll()
        Log.i(TAG, "clearProcessedEvents: processed_sms_events cleared")
    }

    override suspend fun scanInbox(context: Context, lookbackMs: Long) {
        Log.i(TAG, "scanInbox: starting, lookbackMs=$lookbackMs")
        val cutoff = System.currentTimeMillis() - lookbackMs
        val uri = Uri.parse("content://sms/inbox")
        val cursor = try {
            context.contentResolver.query(
                uri,
                arrayOf("address", "body", "date"),
                "date > ?",
                arrayOf(cutoff.toString()),
                "date DESC"
            )
        } catch (e: Exception) {
            Log.e(TAG, "scanInbox: ContentResolver.query failed: ${e.message}", e)
            return
        }
        if (cursor == null) {
            Log.w(TAG, "scanInbox: cursor null — READ_SMS denied or provider unavailable")
            return
        }
        Log.i(TAG, "scanInbox: cursor opened, rowCount=${cursor.count}")
        var count = 0
        var processed = 0
        cursor.use {
            while (it.moveToNext()) {
                val sender = it.getString(0).orEmpty()
                val body = it.getString(1).orEmpty()
                val date = it.getLong(2)
                count++
                try {
                    val msg = SmsMessage(sender = sender, body = body, receivedAtEpochMillis = date)
                    val result = process(msg)
                    Log.i(TAG, "Inbox[$count]: from=$sender cls=${result.classification} fail=${result.failureReason}")
                    processed++
                } catch (e: Exception) {
                    Log.e(TAG, "Inbox[$count]: process() threw for sender=$sender: ${e.message}", e)
                }
            }
        }
        Log.i(TAG, "scanInbox complete: $count messages read, $processed processed without error")
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
