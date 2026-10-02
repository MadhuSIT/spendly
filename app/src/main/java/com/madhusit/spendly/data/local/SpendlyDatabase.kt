package com.madhusit.spendly.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.madhusit.spendly.data.local.ledger.*
import com.madhusit.spendly.data.local.sms.ProcessedSmsEventDao
import com.madhusit.spendly.data.local.sms.ProcessedSmsEventEntity

@Database(
    entities = [
        FoundationEntity::class,
        FinancialEntityEntity::class,
        LedgerTransactionEntity::class,
        TransactionRelationshipEntity::class,
        AuditEventEntity::class,
        ProcessedSmsEventEntity::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(LedgerConverters::class)
abstract class SpendlyDatabase : RoomDatabase() {
    abstract fun foundationDao(): FoundationDao
    abstract fun financialEntityDao(): FinancialEntityDao
    abstract fun ledgerTransactionDao(): LedgerTransactionDao
    abstract fun transactionRelationshipDao(): TransactionRelationshipDao
    abstract fun auditEventDao(): AuditEventDao
    abstract fun processedSmsEventDao(): ProcessedSmsEventDao
}