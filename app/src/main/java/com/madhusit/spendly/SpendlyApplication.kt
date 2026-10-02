package com.madhusit.spendly

import android.app.Application
import androidx.room.Room
import com.madhusit.spendly.data.local.MIGRATION_1_2
import com.madhusit.spendly.data.local.MIGRATION_2_3
import com.madhusit.spendly.data.local.MIGRATION_3_4
import com.madhusit.spendly.data.local.SpendlyDatabase
import com.madhusit.spendly.data.repository.FoundationRepositoryImpl
import com.madhusit.spendly.data.repository.LedgerRepositoryImpl
import com.madhusit.spendly.data.repository.SmsIngestionRepositoryImpl
import com.madhusit.spendly.domain.FoundationRepository
import com.madhusit.spendly.domain.ledger.LedgerRepository
import com.madhusit.spendly.domain.sms.SmsIngestionRepository

class SpendlyApplication : Application() {
    val database: SpendlyDatabase by lazy {
        Room.databaseBuilder(this, SpendlyDatabase::class.java, "spendly.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()
    }

    val repository: FoundationRepository by lazy {
        FoundationRepositoryImpl(database.foundationDao())
    }

    val ledgerRepository: LedgerRepository by lazy {
        LedgerRepositoryImpl(database, database.financialEntityDao(), database.ledgerTransactionDao(), database.transactionRelationshipDao())
    }

    val smsIngestionRepository: SmsIngestionRepository by lazy {
        SmsIngestionRepositoryImpl(database, ledgerRepository, database.processedSmsEventDao())
    }
}