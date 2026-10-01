package com.madhusit.spendly

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
        rule.onNodeWithTag("bottom_nav_transactions").performClick()
        rule.onNodeWithText("Test Merchant").assertIsDisplayed()
        rule.onNodeWithText("EXPENSE").assertIsDisplayed()

        rule.activityRule.scenario.recreate()
        rule.waitForIdle()

        rule.onNodeWithTag("screen_transactions").assertIsDisplayed()
        rule.onNodeWithText("Test Merchant").assertIsDisplayed()
        rule.onNodeWithText("EXPENSE").assertIsDisplayed()
    }
}
