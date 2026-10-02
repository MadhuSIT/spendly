package com.madhusit.spendly

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
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

        // Persistence runs in a ViewModel coroutine, so synchronize on the resulting UI state rather than sleeping.
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        check(device.wait(androidx.test.uiautomator.Until.hasObject(By.text("Spendly")), 5_000)) {
            "Add Expense did not return to Home within 5 seconds."
        }
        rule.waitForIdle()
        rule.onNodeWithTag("screen_home").assertIsDisplayed()
        rule.onNodeWithTag("bottom_nav_transactions").performClick()
        rule.onNodeWithText("Test Merchant").assertIsDisplayed()
        rule.onNodeWithText("EXPENSE ·", substring = true).assertIsDisplayed()
        rule.onNodeWithText("₹125.50").assertIsDisplayed()

        rule.activityRule.scenario.recreate()
        rule.waitForIdle()

        rule.onNodeWithTag("screen_transactions").assertIsDisplayed()
        rule.onNodeWithText("Test Merchant").assertIsDisplayed()
        rule.onNodeWithText("EXPENSE ·").assertIsDisplayed()
        rule.onNodeWithText("₹125.50").assertIsDisplayed()
    }
}
