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
                fingerprint, null,
                result.failureReason?.name ?: "NON_FINANCIAL"
            )
            return@withTransaction result
        }

        val sourceEntity = resolveSourceEntity(normalized)
        if (sourceEntity == null) {
            eventDao.updateResult(fingerprint, null, SmsFailureReason.AMBIGUOUS_ENTITY.name)
            return@withTransaction result.copy(failureReason = SmsFailureReason.AMBIGUOUS_ENTITY)
        }

        val destinationEntity = resolveDestinationEntity(normalized)

        val needsReview = normalized.reviewRequired ||
            normalized.type == TransactionType.TRANSFER ||
            normalized.type == TransactionType.CARD_PAYMENT ||
            normalized.type == TransactionType.REVERSAL

        if (needsReview) {
            val reviewId = UUID.randomUUID().toString()
            val reviewTxn = buildTransaction(
                id = reviewId,
                normalized = normalized,
                sourceEntityId = sourceEntity.id,
                destinationEntityId = destinationEntity?.id,
                fingerprint = fingerprint,
                status = TransactionStatus.PENDING,
                description = "Parsed from SMS – needs review",
                reviewRequired = true,
                receivedAt = message.receivedAtEpochMillis
            )
            ledgerRepository.createTransaction(reviewTxn)
            eventDao.updateResult(fingerprint, reviewId, "REVIEW_PENDING")
            return@withTransaction result
        }

        val status = if (Regex("failed|declined|rejected", RegexOption.IGNORE_CASE).containsMatchIn(message.body))
            TransactionStatus.FAILED else TransactionStatus.CONFIRMED

        val txnId = UUID.randomUUID().toString()
        val txn = buildTransaction(
            id = txnId,
            normalized = normalized,
            sourceEntityId = sourceEntity.id,
            destinationEntityId = destinationEntity?.id,
            fingerprint = fingerprint,
            status = status,
            description = "Imported from SMS",
            reviewRequired = false,
            receivedAt = message.receivedAtEpochMillis
        )
        ledgerRepository.createTransaction(txn)
        eventDao.updateResult(fingerprint, txnId, "PROCESSED")
        result
    }

    // ── Entity resolution ────────────────────────────────────────────────────

    private suspend fun resolveSourceEntity(normalized: NormalizedSmsTransaction): FinancialEntity? {
        val all = ledgerRepository.observeEntities().first().filter { it.active }
        // Filter by entity type hint (CREDIT_CARD / DEBIT_CARD / BANK_ACCOUNT) but exclude MERCHANTs
        val typed = all.filter { it.type == normalized.entityType }.takeIf { it.isNotEmpty() }
            ?: all.filter { it.type != FinancialEntityType.MERCHANT }

        // 1. Exact: provider + last-four
        typed.filter {
            normalized.provider != null &&
                it.provider.equals(normalized.provider, ignoreCase = true) &&
                normalized.lastFour != null && it.lastFour == normalized.lastFour
        }.singleOrNull()?.let { return it }

        // 2. Provider-only
        typed.filter {
            normalized.provider != null &&
                it.provider.equals(normalized.provider, ignoreCase = true)
        }.singleOrNull()?.let { return it }

        // 3. Single non-merchant entity
        val nonMerchant = all.filter { it.type != FinancialEntityType.MERCHANT }
        if (nonMerchant.size == 1) return nonMerchant.single()
        return null
    }

    private suspend fun resolveDestinationEntity(normalized: NormalizedSmsTransaction): FinancialEntity? {
        val merchant = normalized.merchantName?.takeIf { it.isNotBlank() } ?: return null
        return when (normalized.type) {
            TransactionType.EXPENSE -> findOrCreateMerchant(merchant)
            TransactionType.INCOME  -> null // source of income — not a merchant
            else -> null
        }
    }

    private suspend fun findOrCreateMerchant(name: String): FinancialEntity {
        val existing = ledgerRepository.observeEntities().first()
            .firstOrNull { it.type == FinancialEntityType.MERCHANT && it.name.equals(name, ignoreCase = true) }
        if (existing != null) return existing
        val now = System.currentTimeMillis()
        val merchant = FinancialEntity(
            id = UUID.randomUUID().toString(),
            type = FinancialEntityType.MERCHANT,
            provider = null,
            name = name,
            maskedIdentifier = null,
            lastFour = null,
            currency = "INR",
            active = true,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now
        )
        ledgerRepository.createEntity(merchant)
        Log.i(TAG, "Auto-created merchant entity: $name")
        return merchant
    }

    // ── Transaction builder ──────────────────────────────────────────────────

    private fun buildTransaction(
        id: String,
        normalized: NormalizedSmsTransaction,
        sourceEntityId: String,
        destinationEntityId: String?,
        fingerprint: String,
        status: TransactionStatus,
        description: String,
        reviewRequired: Boolean,
        receivedAt: Long
    ) = LedgerTransaction(
        id = id,
        sourceEntityId = sourceEntityId,
        destinationEntityId = destinationEntityId,
        type = normalized.type,
        amountMinor = normalized.amountMinor,
        currency = normalized.currency,
        merchantName = normalized.merchantName,
        description = description,
        transactionTimestamp = normalized.transactionTimestamp,
        status = status,
        referenceNumber = normalized.referenceNumber,
        upiReference = normalized.upiReference,
        rawEventReference = fingerprint,
        parserSource = normalized.parserSource,
        parserVersion = normalized.parserVersion,
        confidence = normalized.confidence,
        reviewRequired = reviewRequired,
        category = normalized.category,
        createdAtEpochMillis = receivedAt,
        updatedAtEpochMillis = receivedAt
    )

    // ── Inbox scan ───────────────────────────────────────────────────────────

    override suspend fun clearProcessedEvents() {
        eventDao.deleteAll()
        Log.i(TAG, "clearProcessedEvents: table cleared")
    }

    override suspend fun scanInbox(context: Context, lookbackMs: Long): SmsInboxScanSummary {
        Log.i(TAG, "scanInbox: starting, lookback=${lookbackMs / 3600000}h")
        val cutoff = System.currentTimeMillis() - lookbackMs
        val cursor = try {
            context.contentResolver.query(
                Uri.parse("content://sms/inbox"),
                arrayOf("address", "body", "date"),
                "date > ?", arrayOf(cutoff.toString()), "date DESC"
            )
        } catch (e: Exception) {
            Log.e(TAG, "scanInbox: query failed: ${e.message}", e)
            return SmsInboxScanSummary(0, 0, 0)
        }
        if (cursor == null) {
            Log.w(TAG, "scanInbox: cursor null — READ_SMS denied?")
            return SmsInboxScanSummary(0, 0, 0)
        }
        Log.i(TAG, "scanInbox: cursor rowCount=${cursor.count}")
        var total = 0; var saved = 0; var review = 0
        cursor.use {
            while (it.moveToNext()) {
                val sender = it.getString(0).orEmpty()
                val body   = it.getString(1).orEmpty()
                val date   = it.getLong(2)
                total++
                try {
                    val result = process(SmsMessage(sender, body, date))
                    Log.i(TAG, "Inbox[$total]: from=$sender cls=${result.classification} fail=${result.failureReason}")
                    if (result.failureReason == null && result.normalized != null) saved++
                    if (result.normalized?.reviewRequired == true) review++
                } catch (e: Exception) {
                    Log.e(TAG, "Inbox[$total]: process threw for $sender: ${e.message}", e)
                }
            }
        }
        Log.i(TAG, "scanInbox done: total=$total saved=$saved review=$review")
        return SmsInboxScanSummary(total, saved, review)
    }

    // ── Fingerprint ──────────────────────────────────────────────────────────

    private fun fingerprint(message: SmsMessage): String {
        val canonical = message.sender.trim().lowercase() + "|" +
            message.body.trim().replace(Regex("\\s+"), " ") + "|" +
            message.receivedAtEpochMillis
        return MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
