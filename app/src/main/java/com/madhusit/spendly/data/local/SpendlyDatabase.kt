package com.madhusit.spendly.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.madhusit.spendly.data.local.ledger.AuditEventDao
import com.madhusit.spendly.data.local.ledger.AuditEventEntity
import com.madhusit.spendly.data.local.ledger.FinancialEntityDao
import com.madhusit.spendly.data.local.ledger.FinancialEntityEntity
import com.madhusit.spendly.data.local.ledger.LedgerConverters
import com.madhusit.spendly.data.local.ledger.LedgerTransactionDao
import com.madhusit.spendly.data.local.ledger.LedgerTransactionEntity
import com.madhusit.spendly.data.local.ledger.TransactionRelationshipDao
import com.madhusit.spendly.data.local.ledger.TransactionRelationshipEntity

@Database(
    entities = [
        FoundationEntity::class,
        FinancialEntityEntity::class,
        LedgerTransactionEntity::class,
        TransactionRelationshipEntity::class,
        AuditEventEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(LedgerConverters::class)
abstract class SpendlyDatabase : RoomDatabase() {
    abstract fun foundationDao(): FoundationDao
    abstract fun financialEntityDao(): FinancialEntityDao
    abstract fun ledgerTransactionDao(): LedgerTransactionDao
    abstract fun transactionRelationshipDao(): TransactionRelationshipDao
    abstract fun auditEventDao(): AuditEventDao
}