package com.madhusit.spendly

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class LedgerVerticalSliceTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun manualExpenseFlowsFromEntryToPersistedLedgerAndSurvivesRecreation() {
        rule.onNodeWithTag("add-expense").performClick()
        rule.onNodeWithTag("expense-amount").performTextInput("125.50")
        rule.onNodeWithTag("expense-merchant").performTextInput("Test Merchant")
        rule.onNodeWithTag("save-expense").performClick()

        // The save callback pops the Add Expense route after persistence completes.
        // Wait for that completion before navigating elsewhere, otherwise the callback can
        // pop the Transactions route that the test just opened.
        rule.waitUntil(10_000) {
            rule.onAllNodesWithTag("save-expense").fetchSemanticsNodes().isEmpty()
        }

        rule.onNodeWithTag("bottom_nav_transactions").performClick()
        rule.waitUntil(10_000) {
            rule.onAllNodesWithText("Test Merchant").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Test Merchant").assertIsDisplayed()
        rule.onNodeWithText("EXPENSE ·", substring = true).assertIsDisplayed()
        rule.onNodeWithText("₹125.50").assertIsDisplayed()

        rule.activityRule.scenario.recreate()
        rule.waitForIdle()

        // Recreation restores the activity from its start destination; explicitly return to the ledger.
        rule.onNodeWithTag("bottom_nav_transactions").performClick()
        rule.waitUntil(10_000) {
            rule.onAllNodesWithText("Test Merchant").fetchSemanticsNodes().isNotEmpty()
        }
        // After recreation, verify the Room-backed ledger state is restored without depending on
        // emulator rendering/bounds at that exact frame.
        rule.onNodeWithTag("screen_transactions").assertExists()
        rule.onNodeWithText("Test Merchant").assertExists()
        rule.onNodeWithText("EXPENSE ·", substring = true).assertExists()
        rule.onNodeWithText("₹125.50").assertExists()
    }
}
