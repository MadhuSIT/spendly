package com.madhusit.spendly

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoundationPersistenceE2ETest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchNavigateRecreateAndVerifyPersistedLedgerShell() {
        composeRule.onNodeWithTag("screen_home").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_nav_transactions").performClick()
        composeRule.onNodeWithTag("screen_transactions").assertIsDisplayed()

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("screen_transactions").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_nav_home").performClick()
        composeRule.onNodeWithTag("screen_home").assertIsDisplayed()
        composeRule.onNodeWithText("Spendly").assertIsDisplayed()
        composeRule.onNodeWithText("Net spending").assertIsDisplayed()
    }
}
