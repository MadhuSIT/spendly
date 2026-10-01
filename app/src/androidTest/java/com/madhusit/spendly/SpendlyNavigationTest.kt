package com.madhusit.spendly

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpendlyNavigationTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomNavigationReachesAllPrimaryDestinations() {
        composeRule.onNodeWithText("Home").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Transactions").performClick()
        composeRule.onNodeWithText("Transactions", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Accounts").performClick()
        composeRule.onNodeWithText("Accounts", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Insights").performClick()
        composeRule.onNodeWithText("Insights", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Home").performClick()
        composeRule.onNodeWithText("Spendly").assertIsDisplayed()
    }
}
