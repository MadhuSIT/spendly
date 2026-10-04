package com.madhusit.spendly.data.repository

import androidx.room.withTransaction
import com.madhusit.spendly.data.local.SpendlyDatabase
import com.madhusit.spendly.data.local.ledger.AuditEventEntity
import com.madhusit.spendly.data.local.ledger.FinancialEntityDao
import com.madhusit.spendly.data.local.ledger.FinancialEntityEntity
import com.madhusit.spendly.data.local.ledger.LedgerTransactionDao
import com.madhusit.spendly.data.local.ledger.LedgerTransactionEntity
import com.madhusit.spendly.data.local.ledger.TransactionRelationshipDao
import com.madhusit.spendly.data.local.ledger.TransactionRelationshipEntity
import com.madhusit.spendly.domain.ledger.FinancialEntity
import com.madhusit.spendly.domain.ledger.LedgerRepository
import com.madhusit.spendly.domain.ledger.LedgerTotals
import com.madhusit.spendly.domain.ledger.LedgerTransaction
import com.madhusit.spendly.domain.ledger.LedgerValidation
import com.madhusit.spendly.domain.ledger.TransactionRelationship
import com.madhusit.spendly.domain.ledger.TransactionStatus
import com.madhusit.spendly.domain.ledger.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LedgerRepositoryImpl(
    private val database: SpendlyDatabase,
    private val entityDao: FinancialEntityDao,
    private val transactionDao: LedgerTransactionDao,
    private val relationshipDao: TransactionRelationshipDao
) : LedgerRepository {

    private val auditDao = database.auditEventDao()

    override fun observeEntities(): Flow<List<FinancialEntity>> =
        entityDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeTransactions(): Flow<List<LedgerTransaction>> =
        transactionDao.observeAll().map { transactions -> transactions.map { it.toDomain() } }

    override suspend fun createEntity(entity: FinancialEntity) {
        LedgerValidation.validateEntity(entity)
        database.withTransaction {
            entityDao.insert(entity.toEntity())
            auditDao.insert(
                AuditEventEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    entityType = "FINANCIAL_ENTITY",
                    entityId = entity.id,
                    action = "CREATED",
                    actor = "USER",
                    details = null,
                    createdAtEpochMillis = entity.createdAtEpochMillis
                )
            )
        }
    }

    override suspend fun createTransaction(transaction: LedgerTransaction) {
        LedgerValidation.validateTransaction(transaction)
        require(entityDao.findById(transaction.sourceEntityId) != null) {
            "Source entity does not exist."
        }
        transaction.destinationEntityId?.let {
            require(entityDao.findById(it) != null) { "Destination entity does not exist." }
        }
        database.withTransaction {
            transactionDao.insert(transaction.toEntity())
            auditDao.insert(
                AuditEventEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    entityType = "TRANSACTION",
                    entityId = transaction.id,
                    action = "CREATED",
                    actor = "USER",
                    details = null,
                    createdAtEpochMillis = transaction.createdAtEpochMillis
                )
            )
        }
    }

    override suspend fun updateTransaction(transaction: LedgerTransaction) {
        LedgerValidation.validateTransaction(transaction)
database.withTransaction {
            transactionDao.update(transaction.toEntity())
            auditDao.insert(
                AuditEventEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    entityType = "TRANSACTION",
                    entityId = transaction.id,
                    action = "UPDATED",
                    actor = "USER",
                    details = null,
                    createdAtEpochMillis = transaction.updatedAtEpochMillis
                )
            )
        }
    }

    override suspend fun createRelationship(relationship: TransactionRelationship) {
        require(relationship.id.isNotBlank())
        require(relationship.fromTransactionId != relationship.toTransactionId) {
            "A transaction cannot relate to itself."
        }
        require(relationship.relationshipType.isNotBlank())
        require(transactionDao.findById(relationship.fromTransactionId) != null)
        require(transactionDao.findById(relationship.toTransactionId) != null)
        database.withTransaction {
            relationshipDao.insert(relationship.toEntity())
            auditDao.insert(
                AuditEventEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    entityType = "TRANSACTION_RELATIONSHIP",
                    entityId = relationship.id,
                    action = "CREATED",
                    actor = "USER",
                    details = relationship.relationshipType,
                    createdAtEpochMillis = relationship.createdAtEpochMillis
                )
            )
        }
    }

    override suspend fun findEntity(id: String): FinancialEntity? =
        entityDao.findById(id)?.toDomain()

    override suspend fun findTransaction(id: String): LedgerTransaction? =
        transactionDao.findById(id)?.toDomain()

    override suspend fun deleteTransaction(id: String) {
        database.withTransaction {
            transactionDao.deleteById(id)
            auditDao.insert(
                AuditEventEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    entityType = "TRANSACTION",
                    entityId = id,
                    action = "DELETED",
                    actor = "USER",
                    details = null,
                    createdAtEpochMillis = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun calculateTotals(): LedgerTotals {
        val transactions = transactionDao.findAll()
        val relationships = relationshipDao.findAll()
        val reversedTransactionIds = relationships
            .filter { it.relationshipType == "REVERSAL" }
            .flatMap { listOf(it.fromTransactionId, it.toTransactionId) }
            .toSet()

        val confirmed = transactions.filter { it.status == TransactionStatus.CONFIRMED }
        val income = confirmed
            .filter { it.type == TransactionType.INCOME && it.id !in reversedTransactionIds }
            .sumOf { it.amountMinor }

        val gross = confirmed
            .filter {
                it.id !in reversedTransactionIds &&
                    (it.type == TransactionType.EXPENSE || it.type == TransactionType.FEE)
            }
            .sumOf { it.amountMinor }

        val refunds = confirmed
            .filter { it.id !in reversedTransactionIds && it.type == TransactionType.REFUND }
            .sumOf { it.amountMinor }

        return LedgerTotals(
            incomeMinor = income,
            grossSpendingMinor = gross,
            refundsMinor = refunds,
            netSpendingMinor = gross - refunds
        )
    }

    private fun FinancialEntityEntity.toDomain() = FinancialEntity(
        id, type, provider, name, maskedIdentifier, lastFour, currency,
        active, createdAtEpochMillis, updatedAtEpochMillis
    )

    private fun FinancialEntity.toEntity() = FinancialEntityEntity(
        id, type, provider, name, maskedIdentifier, lastFour, currency,
        active, createdAtEpochMillis, updatedAtEpochMillis
    )

    private fun LedgerTransactionEntity.toDomain() = LedgerTransaction(
        id, sourceEntityId, destinationEntityId, type, amountMinor, currency,
        merchantName, description, transactionTimestamp, status, referenceNumber,
        upiReference, rawEventReference, parserSource, parserVersion, confidence,
        reviewRequired, createdAtEpochMillis, updatedAtEpochMillis
    )

    private fun LedgerTransaction.toEntity() = LedgerTransactionEntity(
        id, sourceEntityId, destinationEntityId, type, amountMinor, currency,
        merchantName, description, transactionTimestamp, status, referenceNumber,
        upiReference, rawEventReference, parserSource, parserVersion, confidence,
        reviewRequired, createdAtEpochMillis, updatedAtEpochMillis
    )

    private fun TransactionRelationship.toEntity() = TransactionRelationshipEntity(
        id, fromTransactionId, toTransactionId, relationshipType, createdAtEpochMillis
    )
}