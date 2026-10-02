package com.madhusit.spendly

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.madhusit.spendly.data.local.SpendlyDatabase
import com.madhusit.spendly.data.local.ledger.FinancialEntityEntity
import com.madhusit.spendly.data.repository.LedgerRepositoryImpl
import com.madhusit.spendly.data.repository.SmsIngestionRepositoryImpl
import com.madhusit.spendly.domain.ledger.FinancialEntityType
import com.madhusit.spendly.domain.ledger.TransactionType
import com.madhusit.spendly.domain.sms.SmsFailureReason
import com.madhusit.spendly.domain.sms.SmsMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SmsIngestionTest {
    private lateinit var database: SpendlyDatabase
    private lateinit var ingestion: SmsIngestionRepositoryImpl

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SpendlyDatabase::class.java).build()
        val ledger = LedgerRepositoryImpl(
            database,
            database.financialEntityDao(),
            database.ledgerTransactionDao(),
            database.transactionRelationshipDao()
        )
        database.financialEntityDao().insert(
            FinancialEntityEntity(
                id = UUID.randomUUID().toString(),
                type = FinancialEntityType.BANK_ACCOUNT,
                provider = "HDFC",
                name = "HDFC Savings",
                maskedIdentifier = "XXXX1234",
                lastFour = "1234",
                currency = "INR",
                active = true,
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 1L
            )
        )
        ingestion = SmsIngestionRepositoryImpl(database, ledger, database.processedSmsEventDao())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun duplicateFinancialSmsCreatesOneLedgerImpact() {
        val message = SmsMessage(
            sender = "HDFCBK",
            body = "Rs. 125.50 debited from A/c XX1234 at Test Merchant. UPI Ref 123456789",
            receivedAtEpochMillis = 10_000L
        )
        val first = runBlocking { ingestion.process(message) }
        val second = runBlocking { ingestion.process(message) }
        val transactions = runBlocking { database.ledgerTransactionDao().findAll() }

        assertEquals(SmsFailureReason.DUPLICATE, second.failureReason)
        assertEquals(1, transactions.size)
        assertEquals(12550L, transactions.single().amountMinor)
        assertEquals(TransactionType.EXPENSE, transactions.single().type)
        assertEquals(null, first.failureReason)
    }

    @Test
    fun concurrentDuplicateSmsCreatesOnlyOneLedgerImpact() = runBlocking {
        val message = SmsMessage(
            sender = "HDFCBK",
            body = "Rs. 250.00 debited from A/c XX1234 at Concurrent Merchant. UPI Ref 223456789",
            receivedAtEpochMillis = 20_000L
        )

        val results = coroutineScope {
            listOf(
                async { ingestion.process(message) },
                async { ingestion.process(message) }
            ).awaitAll()
        }
        val transactions = database.ledgerTransactionDao().findAll()

        assertEquals(1, transactions.size)
        assertTrue(results.count { it.failureReason == SmsFailureReason.DUPLICATE } >= 1)
        assertTrue(results.count { it.failureReason == null } >= 1)
    }
}
