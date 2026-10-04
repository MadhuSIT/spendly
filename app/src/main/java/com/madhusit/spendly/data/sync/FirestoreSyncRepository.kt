package com.madhusit.spendly.data.sync

import com.google.firebase.firestore.FirebaseFirestore
import com.madhusit.spendly.domain.ledger.FinancialEntity
import com.madhusit.spendly.domain.ledger.FinancialEntityType
import com.madhusit.spendly.domain.ledger.LedgerTransaction
import com.madhusit.spendly.domain.ledger.TransactionStatus
import com.madhusit.spendly.domain.ledger.TransactionType
import com.madhusit.spendly.domain.sync.SyncRepository
import com.madhusit.spendly.domain.sync.SyncResult
import kotlinx.coroutines.tasks.await

class FirestoreSyncRepository : SyncRepository {

    private val db = FirebaseFirestore.getInstance()

    private fun txnCol(uid: String) = db.collection("users").document(uid).collection("transactions")
    private fun entCol(uid: String) = db.collection("users").document(uid).collection("entities")

    override suspend fun pushTransaction(uid: String, transaction: LedgerTransaction) {
        txnCol(uid).document(transaction.id).set(transaction.toMap()).await()
    }

    override suspend fun pushEntity(uid: String, entity: FinancialEntity) {
        entCol(uid).document(entity.id).set(entity.toMap()).await()
    }

    override suspend fun pullAll(uid: String): SyncResult {
        val txnDocs = txnCol(uid).get().await()
        val entDocs = entCol(uid).get().await()
        return SyncResult(
            transactions = txnDocs.documents.mapNotNull { it.toTransaction() },
            entities = entDocs.documents.mapNotNull { it.toEntity() }
        )
    }

    private fun LedgerTransaction.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "sourceEntityId" to sourceEntityId,
        "destinationEntityId" to destinationEntityId,
        "type" to type.name,
        "amountMinor" to amountMinor,
        "currency" to currency,
        "merchantName" to merchantName,
        "description" to description,
        "transactionTimestamp" to transactionTimestamp,
        "status" to status.name,
        "referenceNumber" to referenceNumber,
        "upiReference" to upiReference,
        "rawEventReference" to rawEventReference,
        "parserSource" to parserSource,
        "parserVersion" to parserVersion,
        "confidence" to confidence,
        "reviewRequired" to reviewRequired,
        "category" to category?.name,
        "createdAtEpochMillis" to createdAtEpochMillis,
        "updatedAtEpochMillis" to updatedAtEpochMillis
    )

    private fun FinancialEntity.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "type" to type.name,
        "provider" to provider,
        "name" to name,
        "maskedIdentifier" to maskedIdentifier,
        "lastFour" to lastFour,
        "currency" to currency,
        "active" to active,
        "createdAtEpochMillis" to createdAtEpochMillis,
        "updatedAtEpochMillis" to updatedAtEpochMillis
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toTransaction(): LedgerTransaction? = runCatching {
        LedgerTransaction(
            id = getString("id") ?: id,
            sourceEntityId = getString("sourceEntityId") ?: return@runCatching null,
            destinationEntityId = getString("destinationEntityId"),
            type = TransactionType.valueOf(getString("type") ?: return@runCatching null),
            amountMinor = getLong("amountMinor") ?: return@runCatching null,
            currency = getString("currency") ?: "INR",
            merchantName = getString("merchantName"),
            description = getString("description"),
            transactionTimestamp = getLong("transactionTimestamp") ?: return@runCatching null,
            status = TransactionStatus.valueOf(getString("status") ?: "COMPLETED"),
            referenceNumber = getString("referenceNumber"),
            upiReference = getString("upiReference"),
            rawEventReference = getString("rawEventReference"),
            parserSource = getString("parserSource"),
            parserVersion = getString("parserVersion"),
            confidence = getDouble("confidence"),
            reviewRequired = getBoolean("reviewRequired") ?: false,
            category = getString("category")?.let {
                try { com.madhusit.spendly.domain.ledger.TransactionCategory.valueOf(it) } catch (_: Exception) { null }
            },
            createdAtEpochMillis = getLong("createdAtEpochMillis") ?: 0L,
            updatedAtEpochMillis = getLong("updatedAtEpochMillis") ?: 0L
        )
    }.getOrNull()

    private fun com.google.firebase.firestore.DocumentSnapshot.toEntity(): FinancialEntity? = runCatching {
        FinancialEntity(
            id = getString("id") ?: id,
            type = FinancialEntityType.valueOf(getString("type") ?: return@runCatching null),
            provider = getString("provider"),
            name = getString("name") ?: return@runCatching null,
            maskedIdentifier = getString("maskedIdentifier"),
            lastFour = getString("lastFour"),
            currency = getString("currency") ?: "INR",
            active = getBoolean("active") ?: true,
            createdAtEpochMillis = getLong("createdAtEpochMillis") ?: 0L,
            updatedAtEpochMillis = getLong("updatedAtEpochMillis") ?: 0L
        )
    }.getOrNull()
}
