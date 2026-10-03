package com.madhusit.spendly.presentation

import com.madhusit.spendly.MainDispatcherRule
import com.madhusit.spendly.domain.ledger.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class LedgerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun addTransaction_persistsExpenseAndReportsSuccess() = runTest {
        val repository = FakeLedgerRepository()
        val viewModel = LedgerViewModel(repository)
        val source = repository.entities.value.single()
        var result: String? = "not-called"

        viewModel.addTransaction(
            type = TransactionType.EXPENSE,
            amountRupees = "125.50",
            title = "Test Merchant",
            sourceEntityId = source.id,
            destinationEntityId = null
        ) { result = it }

        testScheduler.advanceUntilIdle()

        assertEquals(null, result)
        val saved = repository.transactions.value.single()
        assertEquals(TransactionType.EXPENSE, saved.type)
        assertEquals(12550L, saved.amountMinor)
        assertEquals("Test Merchant", saved.merchantName)
        assertEquals(source.id, saved.sourceEntityId)
    }

    @Test
    fun observedLedgerMutation_propagatesToViewModelStateAndTotals() = runTest {
        val repository = FakeLedgerRepository()
        val viewModel = LedgerViewModel(repository)
        testScheduler.advanceUntilIdle()

        val transaction = LedgerTransaction(
            id = "tx-1",
            sourceEntityId = "cash",
            destinationEntityId = null,
            type = TransactionType.EXPENSE,
            amountMinor = 12550L,
            currency = "INR",
            merchantName = "Observed Merchant",
            description = null,
            transactionTimestamp = 1_000L,
            status = TransactionStatus.CONFIRMED,
            referenceNumber = null,
            upiReference = null,
            rawEventReference = null,
            parserSource = null,
            parserVersion = null,
            confidence = null,
            reviewRequired = false,
            createdAtEpochMillis = 1_000L,
            updatedAtEpochMillis = 1_000L
        )

        repository.transactions.value = listOf(transaction)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(transaction), viewModel.transactions.value)
        assertEquals(12550L, viewModel.totals.value.grossSpendingMinor)
        assertEquals(12550L, viewModel.totals.value.netSpendingMinor)
    }

    @Test
    fun addTransaction_rejectsZeroAmountWithoutWritingLedger() = runTest {
        val repository = FakeLedgerRepository()
        val viewModel = LedgerViewModel(repository)
        var result: String? = null

        viewModel.addTransaction(
            type = TransactionType.EXPENSE,
            amountRupees = "0",
            title = "Invalid",
            sourceEntityId = repository.entities.value.single().id,
            destinationEntityId = null
        ) { result = it }

        testScheduler.advanceUntilIdle()

        assertNotNull(result)
        assertEquals(0, repository.transactions.value.size)
    }

    @Test
    fun addTransaction_rejectsTransferToSameEntityWithoutWritingLedger() = runTest {
        val repository = FakeLedgerRepository()
        val viewModel = LedgerViewModel(repository)
        var result: String? = null
        val source = repository.entities.value.single()

        viewModel.addTransaction(
            type = TransactionType.TRANSFER,
            amountRupees = "100",
            title = "Self transfer",
            sourceEntityId = source.id,
            destinationEntityId = source.id
        ) { result = it }

        testScheduler.advanceUntilIdle()

        assertNotNull(result)
        assertEquals(0, repository.transactions.value.size)
    }

    private class FakeLedgerRepository : LedgerRepository {
        val entities = MutableStateFlow(
            listOf(
                FinancialEntity(
                    id = "cash",
                    type = FinancialEntityType.CASH,
                    provider = "Spendly",
                    name = "Cash Wallet",
                    maskedIdentifier = null,
                    lastFour = null,
                    currency = "INR",
                    createdAtEpochMillis = 1L,
                    updatedAtEpochMillis = 1L
                )
            )
        )
        val transactions = MutableStateFlow<List<LedgerTransaction>>(emptyList())

        override fun observeEntities(): Flow<List<FinancialEntity>> = entities
        override fun observeTransactions(): Flow<List<LedgerTransaction>> = transactions

        override suspend fun createEntity(entity: FinancialEntity) {
            entities.value = entities.value + entity
        }

        override suspend fun createTransaction(transaction: LedgerTransaction) {
            transactions.value = transactions.value + transaction
        }

        override suspend fun updateTransaction(transaction: LedgerTransaction) {
            transactions.value = transactions.value.map { if (it.id == transaction.id) transaction else it }
        }

        override suspend fun createRelationship(relationship: TransactionRelationship) = Unit

        override suspend fun findEntity(id: String): FinancialEntity? =
            entities.value.firstOrNull { it.id == id }

        override suspend fun findTransaction(id: String): LedgerTransaction? =
            transactions.value.firstOrNull { it.id == id }

        override suspend fun calculateTotals(): LedgerTotals {
            val confirmed = transactions.value.filter { it.status == TransactionStatus.CONFIRMED }
            val income = confirmed.filter { it.type == TransactionType.INCOME }.sumOf { it.amountMinor }
            val spending = confirmed.filter {
                it.type == TransactionType.EXPENSE || it.type == TransactionType.FEE
            }.sumOf { it.amountMinor }
            val refunds = confirmed.filter { it.type == TransactionType.REFUND }.sumOf { it.amountMinor }
            return LedgerTotals(income, spending, refunds, spending - refunds)
        }
    }
}