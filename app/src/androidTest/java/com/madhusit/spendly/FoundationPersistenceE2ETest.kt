package com.madhusit.spendly

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
    fun launchNavigateRecreateAndVerifyPersistedFoundationState() {
        composeRule.onNodeWithText("Spendly").assertIsDisplayed()
        composeRule.onNodeWithText("Transactions").performClick()
        composeRule.onNodeWithText("Transactions", useUnmergedTree = true).assertIsDisplayed()
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Spendly").assertIsDisplayed()
        composeRule.onNodeWithText("Spendly foundation is ready.").assertIsDisplayed()
    }
}
