package com.madhusit.spendly

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class LedgerVerticalSliceTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun manualExpenseFlowsFromEntryToPersistedLedgerAndSurvivesRecreation() {
        rule.onNodeWithTag("add-transaction").performClick()
        rule.onNodeWithTag("transaction-amount").performTextInput("125.50")
        rule.onNodeWithTag("transaction-title").performTextInput("Test Merchant")
        rule.onNodeWithTag("save-transaction").performClick()

        rule.onNodeWithTag("bottom_nav_transactions").performClick()
        rule.waitUntil(15_000) {
            rule.onAllNodesWithText("Test Merchant").fetchSemanticsNodes().isNotEmpty() &&
                rule.onAllNodesWithText("EXPENSE ·", substring = true).fetchSemanticsNodes().isNotEmpty() &&
                rule.onAllNodesWithText("₹125.50").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Test Merchant").assertIsDisplayed()
        rule.onNodeWithText("EXPENSE ·", substring = true).assertIsDisplayed()
        rule.onNodeWithText("₹125.50").assertIsDisplayed()

        rule.onNodeWithText("Test Merchant").performClick()
        rule.onNodeWithTag("screen_transaction_detail").assertIsDisplayed()
        rule.onNodeWithText("Cash Wallet").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("screen_transaction_detail").performScrollToNode(hasText("Added manually through Spendly."))
        rule.onNodeWithText("Added manually through Spendly.").assertIsDisplayed()

        rule.activityRule.scenario.recreate()
        rule.waitUntil(30_000) {
            rule.onAllNodesWithText("Test Merchant").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Test Merchant").assertIsDisplayed()
    }
}
