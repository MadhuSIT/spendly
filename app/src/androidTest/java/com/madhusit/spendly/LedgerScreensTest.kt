package com.madhusit.spendly

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import com.madhusit.spendly.domain.ledger.*
import com.madhusit.spendly.presentation.components.AddTransactionScreen
import com.madhusit.spendly.presentation.components.LedgerHome
import com.madhusit.spendly.presentation.components.LedgerTransactions
import com.madhusit.spendly.presentation.components.TransactionDetailScreen

class LedgerScreensTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun home_emptyState_isVisible() {
        rule.setContent {
            MaterialTheme {
                LedgerHome(
                    padding = PaddingValues(),
                    totals = LedgerTotals(0L, 0L, 0L, 0L),
                    recentTransactions = emptyList(),
                    onAddTransaction = {},
                    onOpenTransaction = {}
                )
            }
        }

        rule.onNodeWithTag("screen_home").assertIsDisplayed()
        rule.onNodeWithTag("home-empty").assertIsDisplayed()
        rule.onNodeWithText("No transactions yet. Add your first transaction to start your ledger.").assertIsDisplayed()
    }

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
    fun transactionDetail_missingTransaction_displaysRecoveryState() {
        rule.setContent {
            MaterialTheme {
                TransactionDetailScreen(
                    padding = PaddingValues(),
                    transaction = null,
                    entities = emptyList(),
                    onBack = {}
                )
            }
        }

        rule.onNodeWithText("Transaction not found").assertIsDisplayed()
        rule.onNodeWithText("This transaction may have been removed or is no longer available.").assertIsDisplayed()
        rule.onNodeWithText("Back").assertIsDisplayed()
    }

    @Test
    fun transactionDetail_automatedProvenance_displaysParserExplanation() {
        val transaction = LedgerTransaction(
            id = "tx-automated",
            sourceEntityId = "cash",
            destinationEntityId = null,
            type = TransactionType.EXPENSE,
            amountMinor = 12550L,
            currency = "INR",
            merchantName = "Parsed Merchant",
            description = null,
            transactionTimestamp = 1_000L,
            status = TransactionStatus.CONFIRMED,
            referenceNumber = null,
            upiReference = null,
            rawEventReference = "sms-1",
            parserSource = "bank-sms",
            parserVersion = "v1",
            confidence = 0.93,
            reviewRequired = false,
            createdAtEpochMillis = 1_000L,
            updatedAtEpochMillis = 1_000L
        )

        rule.setContent {
            MaterialTheme {
                TransactionDetailScreen(
                    padding = PaddingValues(),
                    transaction = transaction,
                    entities = emptyList(),
                    onBack = {}
                )
            }
        }

        rule.onNodeWithTag("screen_transaction_detail").assertIsDisplayed()
        rule.onNodeWithText(
            "Financial activity was parsed from an automated source, validated and persisted in the ledger."
        ).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("bank-sms").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("v1").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("93%").performScrollTo().assertIsDisplayed()
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
        rule.onNodeWithText("Cash Wallet").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("REF-123").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Added manually through Spendly.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun addTransaction_displaysSaveError() {
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
                AddTransactionScreen(
                    padding = PaddingValues(),
                    entities = listOf(entity),
                    onSave = { _, _, _, _, _, onComplete ->
                        onComplete("Enter an amount greater than zero.")
                    },
                    onCancel = {}
                )
            }
        }

        rule.onNodeWithTag("transaction-amount").performTextInput("0")
        rule.onNodeWithTag("transaction-title").performTextInput("Test Merchant")
        rule.onNodeWithTag("save-transaction").performClick()

        rule.onNodeWithTag("transaction-error").assertIsDisplayed()
        rule.onNodeWithText("Enter an amount greater than zero.").assertIsDisplayed()
    }

    @Test
    fun addTransaction_displaysSavingStateUntilCompletion() {
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
        var complete: ((String?) -> Unit)? = null

        rule.setContent {
            MaterialTheme {
                AddTransactionScreen(
                    padding = PaddingValues(),
                    entities = listOf(entity),
                    onSave = { _, _, _, _, _, onComplete -> complete = onComplete },
                    onCancel = {}
                )
            }
        }

        rule.onNodeWithTag("transaction-amount").performTextInput("100")
        rule.onNodeWithTag("save-transaction").performClick()

        rule.onNodeWithText("Saving…").assertIsDisplayed()
        complete?.invoke(null)

        rule.onNodeWithText("Save transaction").assertIsDisplayed()
    }

    @Test
    fun addTransaction_showsTransferRequirementWithSingleEntity() {
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
                AddTransactionScreen(
                    padding = PaddingValues(),
                    entities = listOf(entity),
                    onSave = { _, _, _, _, _, _ -> },
                    onCancel = {}
                )
            }
        }

        rule.onNodeWithText("Transfer").performClick()

        rule.onNodeWithText(
            "Transfers need two different financial entities. Add another account in the Accounts flow."
        ).assertIsDisplayed()
        rule.onNodeWithTag("save-transaction").assertIsNotEnabled()
    }
}
