package com.madhusit.spendly

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import com.madhusit.spendly.domain.ledger.*
import com.madhusit.spendly.presentation.components.LedgerTransactions
import com.madhusit.spendly.presentation.components.TransactionDetailScreen

class LedgerScreensTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun transactions_emptyState_isVisible() {
        rule.setContent {
            MaterialTheme {
                LedgerTransactions(
                    padding = PaddingValues(),
                    transactions = emptyList(),
                    onOpenTransaction = {},
                    onAddTransaction = {}
                )
            }
        }

        rule.onNodeWithTag("screen_transactions").assertIsDisplayed()
        rule.onNodeWithText("No transactions yet. Add a transaction to start your ledger.").assertIsDisplayed()
    }

    @Test
    fun transactionDetail_displaysLedgerProvenanceAndAccounts() {
        val transaction = LedgerTransaction(
            id = "tx-1",
            sourceEntityId = "cash",
            destinationEntityId = null,
            type = TransactionType.EXPENSE,
            amountMinor = 12550L,
            currency = "INR",
            merchantName = "Test Merchant",
            description = null,
            transactionTimestamp = 1_000L,
            status = TransactionStatus.CONFIRMED,
            referenceNumber = "REF-123",
            upiReference = null,
            rawEventReference = null,
            parserSource = "manual",
            parserVersion = null,
            confidence = null,
            reviewRequired = false,
            createdAtEpochMillis = 1_000L,
            updatedAtEpochMillis = 1_000L
        )
        val entity = FinancialEntity(
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

        rule.setContent {
            MaterialTheme {
                TransactionDetailScreen(
                    padding = PaddingValues(),
                    transaction = transaction,
                    entities = listOf(entity),
                    onBack = {}
                )
            }
        }

        rule.onNodeWithTag("screen_transaction_detail").assertIsDisplayed()
        rule.onNodeWithText("Test Merchant").assertIsDisplayed()
        rule.onNodeWithText("₹125.50").assertIsDisplayed()
        rule.onNodeWithText("Cash Wallet").assertIsDisplayed()
        rule.onNodeWithText("REF-123").assertIsDisplayed()
        rule.onNodeWithText("Added manually through Spendly.").assertIsDisplayed()
    }
}
