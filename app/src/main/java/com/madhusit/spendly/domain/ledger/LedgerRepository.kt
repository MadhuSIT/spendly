package com.madhusit.spendly.domain.ledger

import kotlinx.coroutines.flow.Flow

interface LedgerRepository {
    fun observeEntities(): Flow<List<FinancialEntity>>
    fun observeTransactions(): Flow<List<LedgerTransaction>>
    suspend fun createEntity(entity: FinancialEntity)
    suspend fun createTransaction(transaction: LedgerTransaction)
    suspend fun updateTransaction(transaction: LedgerTransaction)
    suspend fun createRelationship(relationship: TransactionRelationship)
    suspend fun findEntity(id: String): FinancialEntity?
    suspend fun findTransaction(id: String): LedgerTransaction?
    suspend fun deleteTransaction(id: String)
    suspend fun calculateTotals(): LedgerTotals
}