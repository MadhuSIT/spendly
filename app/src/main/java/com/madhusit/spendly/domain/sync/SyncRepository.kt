package com.madhusit.spendly.domain.sync

import com.madhusit.spendly.domain.ledger.FinancialEntity
import com.madhusit.spendly.domain.ledger.LedgerTransaction

interface SyncRepository {
    suspend fun pushTransaction(uid: String, transaction: LedgerTransaction)
    suspend fun pushEntity(uid: String, entity: FinancialEntity)
    suspend fun pullAll(uid: String): SyncResult
}

data class SyncResult(
    val transactions: List<LedgerTransaction>,
    val entities: List<FinancialEntity>
)
