package com.madhusit.spendly.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS financial_entities (
                id TEXT NOT NULL PRIMARY KEY,
                type TEXT NOT NULL,
                provider TEXT,
                name TEXT NOT NULL,
                maskedIdentifier TEXT,
                lastFour TEXT,
                currency TEXT NOT NULL,
                active INTEGER NOT NULL,
                createdAtEpochMillis INTEGER NOT NULL,
                updatedAtEpochMillis INTEGER NOT NULL
            )
            """.trimIndent()
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_financial_entities_provider_type_lastFour ON financial_entities(provider, type, lastFour)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_financial_entities_active ON financial_entities(active)"
        )
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ledger_transactions (
                id TEXT NOT NULL PRIMARY KEY,
                sourceEntityId TEXT NOT NULL,
                destinationEntityId TEXT,
                type TEXT NOT NULL,
                amountMinor INTEGER NOT NULL,
                currency TEXT NOT NULL,
                merchantName TEXT,
                description TEXT,
                transactionTimestamp INTEGER NOT NULL,
                status TEXT NOT NULL,
                referenceNumber TEXT,
                upiReference TEXT,
                rawEventReference TEXT,
                parserSource TEXT,
                parserVersion TEXT,
                confidence REAL,
                reviewRequired INTEGER NOT NULL,
                createdAtEpochMillis INTEGER NOT NULL,
                updatedAtEpochMillis INTEGER NOT NULL,
                FOREIGN KEY(sourceEntityId) REFERENCES financial_entities(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(destinationEntityId) REFERENCES financial_entities(id) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent()
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS index_ledger_transactions_sourceEntityId_transactionTimestamp ON ledger_transactions(sourceEntityId, transactionTimestamp)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_ledger_transactions_destinationEntityId_transactionTimestamp ON ledger_transactions(destinationEntityId, transactionTimestamp)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_ledger_transactions_type_status_transactionTimestamp ON ledger_transactions(type, status, transactionTimestamp)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_ledger_transactions_referenceNumber ON ledger_transactions(referenceNumber)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_ledger_transactions_upiReference ON ledger_transactions(upiReference)")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS transaction_relationships (
                id TEXT NOT NULL PRIMARY KEY,
                fromTransactionId TEXT NOT NULL,
                toTransactionId TEXT NOT NULL,
                relationshipType TEXT NOT NULL,
                createdAtEpochMillis INTEGER NOT NULL,
                FOREIGN KEY(fromTransactionId) REFERENCES ledger_transactions(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(toTransactionId) REFERENCES ledger_transactions(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transaction_relationships_fromTransactionId_relationshipType ON transaction_relationships(fromTransactionId, relationshipType)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transaction_relationships_toTransactionId_relationshipType ON transaction_relationships(toTransactionId, relationshipType)")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_transaction_relationships_fromTransactionId_toTransactionId_relationshipType ON transaction_relationships(fromTransactionId, toTransactionId, relationshipType)")
    }
}