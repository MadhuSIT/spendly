package com.madhusit.spendly
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
@RunWith(AndroidJUnit4::class)
class FoundationSmokeTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()
    @Test fun appLaunchesAndShowsFoundation() {
        composeRule.onNodeWithText("Spendly").assertExists()
    }
}
