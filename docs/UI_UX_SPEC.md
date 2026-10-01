# Spendly — UI/UX Foundation

## UX direction
Spendly should feel like a calm financial control center, not a spreadsheet and not a banking clone.
Primary goals: understand today's financial position quickly; see what changed; correct uncertain automation quickly; keep the ledger trustworthy and explainable; surface card obligations early.
Design principle: confidence before cleverness. Automated transactions should be easy to trust, review, correct and trace.

## Information architecture
Use four primary destinations:
- Home — current financial picture and actions
- Transactions — searchable canonical ledger
- Accounts — bank accounts, cards and cash
- Insights — monthly analysis and trends
Global actions: Add transaction, Review items, Search, Notifications/reminders, Settings.
Use compact Android bottom navigation rather than a large permanent sidebar.

## Home
- Header: month selector, review/notification indicator, optional privacy/status indicator.
- Primary summary card: Net spending, Income, Spending, Refunds.
- Never count transfers or card payments as spending.
- If coverage is partial, show a visible Partial data state and covered date range.
- Account snapshot: bank accounts, credit cards and cash with masked identifier, balance/outstanding and relevant status.
- Attention section: review items, uncategorized transactions, card payments due, reconciliation recovery and permission/parser issues.
- Recent transactions: 5–8 rows with merchant, category, entity, timestamp, amount and status.

## Transactions
This is the canonical ledger view.
Filters: search, date/month, entity, category, transaction type, review/pending.
Transaction row hierarchy: merchant/title; category + account/card + time; amount; optional status.
Do not show raw SMS text in the primary list.
Detail view must show amount/type, merchant, category, source/destination entities, timestamp, reference, parser/source, confidence, linked refund/reversal/payment and useful audit history.
Actions: edit, change category, link/correct. Keep destructive actions away from primary actions.

## Add transaction
Streamlined flow: Type -> Amount -> Date/time -> Source -> Destination when needed -> Merchant/title -> Category -> Note -> Save.
Show only fields relevant to the selected transaction type. Transfer asks for destination; expense does not.

## Review queue
Automation needs a first-class review experience.
Each review item explains why attention is required: unknown account, low parser confidence, unknown merchant, possible duplicate, ambiguous type or possible card payment.
Offer the smallest correction: choose account, choose category, confirm duplicate, choose type, link payment or ignore.
After correction, move directly to the next review item.

## Accounts
Group entities into Bank accounts, Credit cards and Cash.
Bank account card: name, masked last four, balance, recent activity.
Credit card card: name, last four, outstanding, statement amount, minimum due, due date and payment status.
Cash card: cash balance and recent manual activity.
Entity detail should keep one entity's activity and configuration together.

## Credit-card UX
Top section: current outstanding, available limit when known, due date, minimum due and payment status.
Then show statement summary, purchases, payments, refunds and remaining amount.
Make the accounting distinction explicit: Purchase = spending; Card payment = money moved to settle the card.
Support full, minimum and custom payments, including partial and multiple payments.

## Insights
Month selector followed by: Spending overview, Category breakdown, Entity breakdown, Largest transactions, Recurring/subscription activity, covered-period changes and Upcoming obligations.
Avoid decorative charts without actionable interpretation.
If historical coverage is insufficient, say so instead of showing a misleading comparison.

## Notifications
Notification taps deep-link to the exact action: review transaction, card payment, statement, or review queue.
Do not notify for every successfully parsed transaction.

## Empty/degraded states
Home: Your ledger is ready. Add an account to start tracking.
Transactions: No transactions yet. Spendly will add supported financial transactions automatically.
Review: Everything is reconciled. Nothing needs your attention.
Insights: Not enough history yet. Keep using Spendly to unlock monthly trends.
Errors should explain the action needed, not expose stack traces. Never hide financial uncertainty.

## Design system
Visual character: clean, compact but breathable, strong typography hierarchy, restrained color and rounded cards used for grouping rather than decoration.
Use semantic tokens for income, expense, warning/review, error, neutral and pending. Never use color alone to communicate financial meaning.
Use Material 3 typography and a consistent 4dp-based spacing scale.
Reusable components: FinancialSummaryCard, EntityCard, TransactionRow, TransactionStatusChip, ReviewCard, CategoryChip, StatementCard, InsightCard, EmptyState, ErrorState and FilterSheet.

## Accessibility
- Minimum 48dp touch targets.
- Content descriptions for meaningful icons.
- Semantic labels for amounts and statuses.
- No color-only meaning.
- Dynamic font scaling and sufficient contrast.
- Predictable focus order.
- Screen-reader-friendly transaction descriptions.

## Onboarding
1. Explain the value: financial SMS becomes a private ledger.
2. Explain privacy.
3. Request SMS permission.
4. Add/confirm financial entities.
5. Run initial reconciliation.
6. Review imported transactions.
7. Land on Home.
Do not request every optional permission on first launch.

## Trust UX
Every automated transaction should be explainable.
A compact Why Spendly added this section can show that a financial SMS was detected, provider identified, account resolved, parser confidence calculated and a category rule applied.
Do not expose raw sensitive SMS content by default.

## Navigation
Bottom navigation: Home | Transactions | Accounts | Insights.
Use bottom sheets for filters, category selection, entity selection and quick add.
Use full-screen navigation for transaction detail, entity detail, statement detail, settings and review workflow.

## UI state model
Every major screen explicitly models Loading, Content, Empty, Partial/Degraded and Error.
Transaction data additionally models Pending, Needs review, Confirmed, Linked and Refunded/Reversed.
Avoid ambiguous null values as UI state.

## UX acceptance criteria
- Home communicates net spending and income at a glance and surfaces actionable problems.
- Transactions make amount, type, account, category and review state obvious.
- Credit cards clearly separate purchases from payments and represent partial payments correctly.
- Review items explain why they need attention and can be processed consecutively.
- Insights derive from ledger semantics and disclose incomplete history.
- Primary actions are accessible and screen-reader understandable.

## Implementation order
1. App shell + Material 3 theme
2. Bottom navigation
3. Home
4. Transactions list/detail
5. Add transaction
6. Review queue
7. Accounts
8. Credit-card detail/statement/payment UX
9. Insights
10. Settings/privacy
11. Loading/empty/error/degraded states
12. Accessibility and UI tests

The first UI milestone should feel complete with local/mock ledger data. Real SMS ingestion should plug into the same screens without requiring navigation redesign.

## Detailed screen and flow specification

The precise screen-by-screen behavior, states, navigation, ASCII flow diagrams, and end-to-end UX flows are maintained in [`docs/UI_UX_FLOWS.md`](./UI_UX_FLOWS.md). This document is the implementation-oriented source of truth; this foundation document remains the higher-level UX direction and design-system reference.
