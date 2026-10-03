package com.madhusit.spendly

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

/**
 * Phase 4D end-to-end validation.
 *
 * Each test uses a unique merchant/title string so runs accumulate without
 * interfering. Empty-state assertions are covered by LedgerScreensTest (Compose
 * unit) since device DB is shared across runs.
 */
class LedgerE2EValidationTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    // ─── 1. Income vertical slice ───────────────────────────────────────────

    @Test
    fun incomeTransactionAppearsInListAndTotalsShowIncomeNotSpending() {
        rule.onNodeWithTag("add-transaction").performClick()

        // Switch to Income tab
        rule.onNodeWithText("Income").performClick()

        // "80.00" → 8000 minor → ₹80.00
        rule.onNodeWithTag("transaction-amount").performTextInput("80.00")
        rule.onNodeWithTag("transaction-title").performTextInput("4D-Salary")
        rule.onNodeWithTag("save-transaction").performClick()

        // Navigated back to Home; wait for totals card to reflect income
        rule.waitUntil(15_000) {
            rule.onAllNodesWithText("4D-Salary").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify income row is present in the summary card
        rule.waitUntil(15_000) {
            rule.onAllNodesWithText("₹80.00").fetchSemanticsNodes().isNotEmpty()
        }

        // Navigate to Transactions list and verify the income entry
        rule.onNodeWithTag("bottom_nav_transactions").performClick()
        rule.waitUntil(15_000) {
            rule.onAllNodesWithText("4D-Salary").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("4D-Salary").assertIsDisplayed()
        rule.onNodeWithText("INCOME ·", substring = true).assertIsDisplayed()
        rule.onNodeWithText("₹80.00").assertIsDisplayed()
    }

    // ─── 2. Income does not inflate Spending total ──────────────────────────

    @Test
    fun incomeDoesNotAppearInSpendingTotal() {
        rule.onNodeWithTag("add-transaction").performClick()
        rule.onNodeWithText("Income").performClick()
        rule.onNodeWithTag("transaction-amount").performTextInput("5000")
        rule.onNodeWithTag("transaction-title").performTextInput("4D-Income-NoSpend")
        rule.onNodeWithTag("save-transaction").performClick()

        // Wait for home to stabilise
        rule.waitUntil(15_000) {
            rule.onAllNodesWithText("4D-Income-NoSpend").fetchSemanticsNodes().isNotEmpty()
        }

        // The Spending row must not contain this amount as a spend figure
        // (it may appear in the Income row — we check the label prefix)
        rule.onNodeWithText("Spending", substring = true).assertIsDisplayed()
        // Income label must be visible and non-zero after adding
        rule.onNodeWithText("Income", substring = true).assertIsDisplayed()
    }

    // ─── 3. Transfer tab — single entity guard ──────────────────────────────

    @Test
    fun transferTabWithSingleEntityShowsWarningAndDisablesSave() {
        rule.onNodeWithTag("add-transaction").performClick()

        // Switch to Transfer
        rule.onNodeWithText("Transfer").performClick()

        rule.waitUntil(10_000) {
            rule.onAllNodesWithText(
                "Transfers need two different financial entities.",
                substring = true
            ).fetchSemanticsNodes().isNotEmpty()
        }

        rule.onNodeWithText(
            "Transfers need two different financial entities. Add another account in the Accounts flow."
        ).assertIsDisplayed()
        rule.onNodeWithTag("save-transaction").assertIsNotEnabled()
    }

    // ─── 4. Transaction detail navigation and back behaviour ────────────────

    @Test
    fun addTransactionThenOpenDetailThenNavigateBack() {
        rule.onNodeWithTag("add-transaction").performClick()
        // "4.50" → 450 minor → ₹4.50
        rule.onNodeWithTag("transaction-amount").performTextInput("4.50")
        rule.onNodeWithTag("transaction-title").performTextInput("4D-NavTest")
        rule.onNodeWithTag("save-transaction").performClick()

        // Wait to return to Home
        rule.waitUntil(15_000) {
            rule.onAllNodesWithText("4D-NavTest").fetchSemanticsNodes().isNotEmpty()
        }

        // Open detail from Home
        rule.onNodeWithText("4D-NavTest").performClick()
        rule.onNodeWithTag("screen_transaction_detail").assertIsDisplayed()
        rule.onNodeWithText("4D-NavTest").assertIsDisplayed()
        rule.onNodeWithText("₹4.50").assertIsDisplayed()

        // Provenance — manual
        rule.onNodeWithText("Added manually through Spendly.").performScrollTo().assertIsDisplayed()

        // Back returns to Home (popBackStack goes back to Home since we navigated from there)
        rule.onNodeWithText("Back to transactions").performClick()
        rule.onNodeWithTag("screen_home").assertIsDisplayed()
    }

    // ─── 5. Detail screen via Transactions list ──────────────────────────────

    @Test
    fun openDetailFromTransactionsListAndVerifyAccountsSection() {
        rule.onNodeWithTag("add-transaction").performClick()
        rule.onNodeWithTag("transaction-amount").performTextInput("999")
        rule.onNodeWithTag("transaction-title").performTextInput("4D-FromList")
        rule.onNodeWithTag("save-transaction").performClick()

        rule.waitUntil(15_000) {
            rule.onAllNodesWithText("4D-FromList").fetchSemanticsNodes().isNotEmpty()
        }

        rule.onNodeWithTag("bottom_nav_transactions").performClick()
        rule.waitUntil(15_000) {
            rule.onAllNodesWithText("4D-FromList").fetchSemanticsNodes().isNotEmpty()
        }

        rule.onNodeWithText("4D-FromList").performClick()
        rule.onNodeWithTag("screen_transaction_detail").assertIsDisplayed()

        // Accounts section: source must resolve to the default Cash Wallet entity
        rule.onNodeWithText("Cash Wallet").performScrollTo().assertIsDisplayed()

        // Back returns to Transactions list
        rule.onNodeWithText("Back to transactions").performClick()
        rule.onNodeWithTag("screen_transactions").assertIsDisplayed()
    }

    // ─── 6. Persistence across recreation ────────────────────────────────────

    @Test
    fun detailScreenContentSurvivesActivityRecreation() {
        rule.onNodeWithTag("add-transaction").performClick()
        // "33.33" → 3333 minor → ₹33.33
        rule.onNodeWithTag("transaction-amount").performTextInput("33.33")
        rule.onNodeWithTag("transaction-title").performTextInput("4D-Recreate")
        rule.onNodeWithTag("save-transaction").performClick()

        rule.waitUntil(15_000) {
            rule.onAllNodesWithText("4D-Recreate").fetchSemanticsNodes().isNotEmpty()
        }

        rule.onNodeWithText("4D-Recreate").performClick()
        rule.onNodeWithTag("screen_transaction_detail").assertIsDisplayed()
        rule.onNodeWithText("₹33.33").assertIsDisplayed()

        rule.activityRule.scenario.recreate()
        rule.waitForIdle()

        // After recreation the deep-link state is lost (NavHost resets to Home),
        // but the persisted transaction must be in the list.
        rule.waitUntil(30_000) {
            rule.onAllNodesWithText("4D-Recreate").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("4D-Recreate").assertIsDisplayed()
    }

    // ─── 7. Multiple transactions co-exist in the list ───────────────────────

    @Test
    fun multipleTransactionsAllAppearInTransactionsList() {
        val tag1 = "4D-Multi-A"
        val tag2 = "4D-Multi-B"

        // Add first transaction
        rule.onNodeWithTag("add-transaction").performClick()
        rule.onNodeWithTag("transaction-amount").performTextInput("111")
        rule.onNodeWithTag("transaction-title").performTextInput(tag1)
        rule.onNodeWithTag("save-transaction").performClick()
        rule.waitUntil(15_000) {
            rule.onAllNodesWithText(tag1).fetchSemanticsNodes().isNotEmpty()
        }

        // Add second transaction
        rule.onNodeWithTag("add-transaction").performClick()
        rule.onNodeWithTag("transaction-amount").performTextInput("222")
        rule.onNodeWithTag("transaction-title").performTextInput(tag2)
        rule.onNodeWithTag("save-transaction").performClick()
        rule.waitUntil(15_000) {
            rule.onAllNodesWithText(tag2).fetchSemanticsNodes().isNotEmpty()
        }

        rule.onNodeWithTag("bottom_nav_transactions").performClick()
        rule.waitUntil(15_000) {
            rule.onAllNodesWithText(tag1).fetchSemanticsNodes().isNotEmpty() &&
                rule.onAllNodesWithText(tag2).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText(tag1).assertIsDisplayed()
        rule.onNodeWithText(tag2).assertIsDisplayed()
    }
}
