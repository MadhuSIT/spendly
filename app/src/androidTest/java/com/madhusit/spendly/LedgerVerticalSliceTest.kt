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
        rule.onNodeWithTag("add-transaction").performClick()
        rule.onNodeWithTag("transaction-amount").performTextInput("125.50")
        rule.onNodeWithTag("transaction-title").performTextInput("Test Merchant")
        rule.onNodeWithTag("save-transaction").performClick()

        rule.waitUntil(10_000) {
            rule.onAllNodesWithTag("save-transaction").fetchSemanticsNodes().isEmpty()
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
        rule.onNodeWithTag("bottom_nav_transactions").performClick()
        rule.waitUntil(10_000) {
            rule.onAllNodesWithText("Test Merchant").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("screen_transactions").assertExists()
        rule.onNodeWithText("Test Merchant").assertExists()
        rule.onNodeWithText("EXPENSE ·", substring = true).assertExists()
        rule.onNodeWithText("₹125.50").assertExists()
    }
}
