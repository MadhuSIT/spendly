package com.madhusit.spendly

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.madhusit.spendly.domain.ledger.CreateFinancialEntity
import com.madhusit.spendly.domain.ledger.FinancialEntityType
import com.madhusit.spendly.domain.sms.SmsMessage
import com.madhusit.spendly.presentation.sms.SmsReceiver
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmsIngestionE2ETest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun smsReceiverHandoffCreatesPersistedLedgerTransactionAndUpdatesUi() {
        val app = rule.activity.application as SpendlyApplication
        val now = System.currentTimeMillis()
        val merchant = "SMS E2E Merchant"

        runBlocking {
            CreateFinancialEntity(app.ledgerRepository)(
                type = FinancialEntityType.BANK_ACCOUNT,
                name = "HDFC E2E Account",
                provider = "HDFC",
                maskedIdentifier = "XXXX4321",
                lastFour = "4321",
                nowEpochMillis = now
            )
        }

        val message = SmsMessage(
            sender = "HDFCBK",
            body = "Rs. 321.45 debited from A/c XX4321 at $merchant. UPI Ref 987654321",
            receivedAtEpochMillis = now
        )

        runBlocking {
            val receiver = SmsReceiver()
            receiver.processMessage(app, message)
            receiver.processMessage(app, message)
        }

        val matchingTransactions = runBlocking {
            app.database.ledgerTransactionDao().findAll().filter {
                it.merchantName == merchant
            }
        }
        assertEquals("Duplicate SMS must create one ledger transaction", 1, matchingTransactions.size)
        assertEquals(32145L, matchingTransactions.single().amountMinor)

        rule.onNodeWithTag("bottom_nav_transactions").performClick()
        rule.waitUntil(10_000) {
            rule.onAllNodesWithText(merchant).fetchSemanticsNodes().isNotEmpty()
        }

        rule.onNodeWithText(merchant).assertIsDisplayed()
        check(rule.onAllNodesWithText("EXPENSE ·", substring = true).fetchSemanticsNodes().isNotEmpty())
        check(rule.onAllNodesWithText("₹321.45").fetchSemanticsNodes().isNotEmpty())
        rule.onNodeWithTag("screen_transactions").assertExists()
    }
}
