package com.madhusit.spendly

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.madhusit.spendly.data.local.SpendlyDatabase
import com.madhusit.spendly.data.repository.LedgerRepositoryImpl
import com.madhusit.spendly.domain.ledger.FinancialEntity
import com.madhusit.spendly.domain.ledger.FinancialEntityType
import com.madhusit.spendly.domain.ledger.LedgerRepository
import com.madhusit.spendly.domain.ledger.LedgerTransaction
import com.madhusit.spendly.domain.ledger.TransactionRelationship
import com.madhusit.spendly.domain.ledger.TransactionStatus
import com.madhusit.spendly.domain.ledger.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class LedgerRepositoryInvariantTest {
    private lateinit var context: Context
    private lateinit var database: SpendlyDatabase
    private lateinit var repository: LedgerRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, SpendlyDatabase::class.java).build()
        repository = LedgerRepositoryImpl(
            database,
            database.financialEntityDao(),
            database.ledgerTransactionDao(),
            database.transactionRelationshipDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun transfersAndCardPaymentsNeverIncreaseSpending() = runBlocking {
        createEntity("bank-a", FinancialEntityType.BANK_ACCOUNT)
        createEntity("bank-b", FinancialEntityType.BANK_ACCOUNT)
        createEntity("card", FinancialEntityType.CREDIT_CARD)

        repository.createTransaction(transaction("expense", "bank-a", TransactionType.EXPENSE, 5_000))
        repository.createTransaction(transaction("transfer", "bank-a", TransactionType.TRANSFER, 50_000, "bank-b"))
        repository.createTransaction(transaction("card-payment", "bank-a", TransactionType.CARD_PAYMENT, 5_000, "card"))

        val totals = repository.calculateTotals()
        assertEquals(5_000, totals.grossSpendingMinor)
        assertEquals(5_000, totals.netSpendingMinor)
    }

    @Test
    fun feesCountAsSpendingWhileCashWithdrawalsDoNot() = runBlocking {
        createEntity("bank", FinancialEntityType.BANK_ACCOUNT)

        repository.createTransaction(transaction("fee", "bank", TransactionType.FEE, 250))
        repository.createTransaction(transaction("atm", "bank", TransactionType.CASH_WITHDRAWAL, 10_000))

        val totals = repository.calculateTotals()
        assertEquals(250, totals.grossSpendingMinor)
        assertEquals(250, totals.netSpendingMinor)
    }

    @Test
    fun refundsReduceNetSpendingWithoutChangingGrossSpending() = runBlocking {
        createEntity("bank", FinancialEntityType.BANK_ACCOUNT)

        repository.createTransaction(transaction("expense", "bank", TransactionType.EXPENSE, 10_000))
        repository.createTransaction(transaction("refund", "bank", TransactionType.REFUND, 4_000))

        val totals = repository.calculateTotals()
        assertEquals(10_000, totals.grossSpendingMinor)
        assertEquals(4_000, totals.refundsMinor)
        assertEquals(6_000, totals.netSpendingMinor)
    }

    @Test
    fun pendingAndFailedTransactionsHaveNoFinancialImpact() = runBlocking {
        createEntity("bank", FinancialEntityType.BANK_ACCOUNT)

        repository.createTransaction(
            transaction("pending", "bank", TransactionType.EXPENSE, 8_000, status = TransactionStatus.PENDING)
        )
        repository.createTransaction(
            transaction("failed", "bank", TransactionType.EXPENSE, 9_000, status = TransactionStatus.FAILED)
        )

        val totals = repository.calculateTotals()
        assertEquals(0, totals.grossSpendingMinor)
        assertEquals(0, totals.netSpendingMinor)
        assertNotNull(repository.findTransaction("failed"))
    }

    @Test
    fun statusTransitionToFailedRemovesPreviouslyConfirmedFinancialImpact() = runBlocking {
        createEntity("bank", FinancialEntityType.BANK_ACCOUNT)
        val confirmed = transaction("tx", "bank", TransactionType.EXPENSE, 7_500)
        repository.createTransaction(confirmed)
        assertEquals(7_500, repository.calculateTotals().netSpendingMinor)

        repository.updateTransaction(
            confirmed.copy(
                status = TransactionStatus.FAILED,
                updatedAtEpochMillis = 2
            )
        )

        assertEquals(0, repository.calculateTotals().netSpendingMinor)
        assertEquals(TransactionStatus.FAILED, repository.findTransaction("tx")?.status)
    }

    @Test
    fun reversalRelationshipNeutralizesOriginalTransaction() = runBlocking {
        createEntity("bank", FinancialEntityType.BANK_ACCOUNT)
        repository.createTransaction(transaction("original", "bank", TransactionType.EXPENSE, 12_000))
        repository.createTransaction(transaction("reversal", "bank", TransactionType.REVERSAL, 12_000))
        repository.createRelationship(
            TransactionRelationship(
                id = "relationship",
                fromTransactionId = "reversal",
                toTransactionId = "original",
                relationshipType = "REVERSAL",
                createdAtEpochMillis = 3
            )
        )

        val totals = repository.calculateTotals()
        assertEquals(0, totals.grossSpendingMinor)
        assertEquals(0, totals.netSpendingMinor)
    }

    @Test
    fun reversalRelationshipNeutralizesIncomeToo() = runBlocking {
        createEntity("bank", FinancialEntityType.BANK_ACCOUNT)
        repository.createTransaction(transaction("income", "bank", TransactionType.INCOME, 20_000))
        repository.createTransaction(transaction("reversal", "bank", TransactionType.REVERSAL, 20_000))
        repository.createRelationship(
            TransactionRelationship(
                id = "relationship",
                fromTransactionId = "reversal",
                toTransactionId = "income",
                relationshipType = "REVERSAL",
                createdAtEpochMillis = 3
            )
        )

        assertEquals(0, repository.calculateTotals().incomeMinor)
    }

    @Test
    fun rebuildingDerivedTotalsNeverMutatesLedger() = runBlocking {
        createEntity("bank", FinancialEntityType.BANK_ACCOUNT)
        repository.createTransaction(transaction("expense", "bank", TransactionType.EXPENSE, 3_000))
        repository.createTransaction(transaction("income", "bank", TransactionType.INCOME, 9_000))

        val before = repository.observeTransactions().first()
        val first = repository.calculateTotals()
        val second = repository.calculateTotals()
        val after = repository.observeTransactions().first()

        assertEquals(first, second)
        assertEquals(before, after)
        assertEquals(2, after.size)
    }

    @Test
    fun repositoryRejectsTransactionsWhoseFinancialEntityDoesNotExist() {
        runBlocking {
            var thrown: IllegalArgumentException? = null
            try {
                repository.createTransaction(transaction("orphan", "missing", TransactionType.EXPENSE, 1_000))
            } catch (error: IllegalArgumentException) {
                thrown = error
            }
            assertNotNull(thrown)
        }
    }

    private suspend fun createEntity(id: String, type: FinancialEntityType) {
        repository.createEntity(
            FinancialEntity(
                id = id,
                type = type,
                provider = "TEST",
                name = id,
                maskedIdentifier = null,
                lastFour = null,
                currency = "INR",
                active = true,
                createdAtEpochMillis = 1,
                updatedAtEpochMillis = 1
            )
        )
    }

    private fun transaction(
        id: String,
        source: String,
        type: TransactionType,
        amount: Long,
        destination: String? = null,
        status: TransactionStatus = TransactionStatus.CONFIRMED
    ) = LedgerTransaction(
        id = id,
        sourceEntityId = source,
        destinationEntityId = destination,
        type = type,
        amountMinor = amount,
        currency = "INR",
        merchantName = "Test",
        description = null,
        transactionTimestamp = 1,
        status = status,
        referenceNumber = id,
        upiReference = null,
        rawEventReference = null,
        parserSource = null,
        parserVersion = null,
        confidence = 1.0,
        reviewRequired = false,
        createdAtEpochMillis = 1,
        updatedAtEpochMillis = 1
    )
}
