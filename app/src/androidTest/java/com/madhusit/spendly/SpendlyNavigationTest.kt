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
class SpendlyNavigationTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomNavigationReachesAllPrimaryDestinations() {
        composeRule.onNodeWithText("Home").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_nav_transactions").performClick()
        composeRule.onNodeWithText("Transactions", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_nav_accounts").performClick()
        composeRule.onNodeWithText("Accounts", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_nav_insights").performClick()
        composeRule.onNodeWithText("Insights", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_nav_home").performClick()
        composeRule.onNodeWithText("Spendly").assertIsDisplayed()
    }
}
