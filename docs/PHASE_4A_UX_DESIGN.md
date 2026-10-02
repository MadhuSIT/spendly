# Spendly — Phase 4A UX Design & Scope

## Status

Approved Phase 4A design baseline for implementation on the existing local-first Android architecture.

Parent: #7 — Phase 4: Core mobile UX vertical slices.

## Design objective

Turn the existing functional Compose shell into the first production-quality Spendly experience without changing the financial source of truth.

The first milestone is intentionally limited to two connected vertical slices:

1. Home → Transactions → Transaction Detail
2. Add Expense / Add Income / Add Transfer → canonical ledger → refreshed UI

This gives Spendly a complete, trustworthy daily ledger loop before adding the larger review, account, card, insights and onboarding surfaces.

## Existing baseline

The current app already provides:

- Material 3 Compose application shell.
- Four bottom-navigation destinations: Home, Transactions, Accounts, Insights.
- Room-backed ledger state exposed through LedgerViewModel.
- Home totals from the ledger.
- Transaction list from the canonical ledger.
- Add Expense flow.
- SMS ingestion already feeds the same ledger.
- Accounts and Insights are currently placeholders.
- Transaction rows currently lack detail navigation.
- Add Expense is currently narrower than the required transaction model.

Phase 4 must evolve this foundation rather than introduce a second UI/data architecture.

## Source of truth

The UI architecture remains:

SMS / Manual action → Domain use case → Room ledger → observable state → ViewModel → Compose UI

Rules:

- UI never owns financial truth.
- UI does not calculate accounting semantics independently.
- Manual and automatic transactions use the same ledger model.
- Home, Transactions and future Accounts/Insights observe the same persisted ledger.
- Navigation state is separate from financial state.
- No backend is required for this milestone.

## Slice 1 — Home → Transactions → Transaction Detail

### Home

Purpose: communicate the selected period's financial position and expose the primary action.

Required content:

- Spendly header.
- Month/period context.
- Primary summary:
  - Net spending
  - Income
  - Spending
  - Refunds
- Partial coverage state when history does not cover the selected period.
- Recent transactions, approximately 5–8.
- Primary Add action.
- Review/attention affordance only when actionable data exists.

For this milestone, account/credit-card attention content may remain deferred if the underlying feature does not yet exist.

States:

- Loading
- First-use empty
- Content
- Partial/degraded
- Error

### Transactions

Purpose: canonical ledger list.

Required:

- Date/month context.
- Transaction type/status visibility.
- Merchant/title.
- Category when available.
- Entity when available.
- Time.
- Amount.
- Review/pending status when relevant.
- Empty and error states.

A transaction row must navigate to Transaction Detail.

Transfers and card payments must remain visually distinct from spending.

### Transaction Detail

Purpose: make one ledger entry understandable and correctable.

Required sections:

1. Amount, currency and type.
2. Merchant/title.
3. Source entity.
4. Destination entity when applicable.
5. Transaction timestamp.
6. Reference/UPI reference when available.
7. Category.
8. Status.
9. Parser/source/confidence for automated entries.
10. Related transaction links when available.
11. Compact "Why Spendly added this" provenance.

Raw SMS is not shown by default.

Actions in this milestone:

- Edit supported fields where existing domain APIs safely support them.
- Change category when category infrastructure is available.
- Back navigation.

Destructive actions are not part of the first milestone.

## Slice 2 — Add transaction

The existing Add Expense screen is expanded into a shared transaction-entry flow.

### Entry sequence

Type → Amount → Date/time → Source → Destination if needed → Merchant/title → Category → Note → Save

Progressive disclosure is mandatory.

### Expense

Required:

- Amount
- Date/time with safe current-time default
- Source entity
- Merchant/title
- Category
- Note optional

### Income

Required:

- Amount
- Date/time
- Destination/source semantics according to the domain model
- Title/source
- Category where supported
- Note optional

### Transfer

Required:

- Amount
- Date/time
- Source entity
- Destination entity
- Note optional

Transfer must not increase spending.

### UX behavior

- Validation is local and immediate.
- Save gives immediate feedback.
- Save uses the canonical ledger use case.
- On success, return to the originating context and show the newly persisted transaction.
- On failure, preserve entered values and explain the action needed.
- Do not block the UI thread with database work.
- Prevent duplicate submission while save is in progress.

## Navigation contract

Primary destinations remain:

Home | Transactions | Accounts | Insights

Milestone navigation:

Home → Transactions → Transaction Detail

Home → Add Transaction → Save → previous context

Transactions → Add Transaction → Save → Transactions

Back from detail returns to the list/home context that opened it.

The four-tab shell must remain stable as future features are added.

## UI state contract

Every milestone screen explicitly models:

- Loading
- Content
- Empty
- Error

Where applicable:

- Partial/degraded
- Pending
- Needs review
- Confirmed
- Linked
- Refunded
- Reversed

Do not use unexplained nulls as UI state.

## Accessibility contract

Initial implementation must include:

- Minimum 48dp touch targets.
- Meaningful semantics for navigation items.
- Content descriptions for meaningful icons.
- Screen-reader-readable amounts, types and statuses.
- Dynamic text sizing without clipped critical information.
- No financial meaning communicated by color alone.
- Predictable focus order.

## Performance contract

The local ledger must feel immediate.

Implementation requirements:

- Observe Room data rather than reloading manually after every mutation.
- Database/parsing work stays off the main thread.
- Avoid unnecessary full-screen loading states when local data is already available.
- Use stable list keys.
- Avoid unnecessary recomposition and duplicated formatting/business logic.
- Add UI test coverage for the critical navigation/mutation path.

Quantitative frame-time profiling is a later quality-review activity unless a regression is observed during implementation.

## Financial correctness contract

The UI must communicate the existing domain semantics:

- Expense → spending increases.
- Income → income increases.
- Refund → net spending decreases.
- Transfer → balances move; spending unchanged.
- Card payment → card outstanding changes; spending unchanged.
- Duplicate SMS → no second financial impact.
- Failed transaction → no expense impact.

No UI-specific accounting implementation is permitted.

## Acceptance criteria for Phase 4A

Design is considered complete when implementation can proceed without inventing:

- primary screens and navigation;
- required fields;
- financial semantics;
- loading/empty/error behavior;
- accessibility expectations;
- source-of-truth boundaries;
- E2E proof expectations.

## Explicitly out of this first implementation slice

These remain later Phase 4 slices:

- Review Queue / Correction.
- Accounts and entity detail.
- Credit-card detail.
- Statements and payment matching.
- Categories & Rules management.
- Search/filter implementation beyond any shell needed by the first slice.
- Notifications/attention.
- Insights.
- Onboarding.
- Initial reconciliation UI.
- Settings/privacy.
- Full physical-device accessibility and performance profiling.

They are not removed from the product; they are sequenced behind the first connected UX milestone.

## Implementation sequence

1. App shell/theme cleanup required by the milestone.
2. Upgrade Home.
3. Upgrade Transactions list.
4. Add Transaction Detail navigation and screen.
5. Generalize Add Expense into shared Add Transaction flow.
6. Add Income.
7. Add Transfer.
8. Add state/error/accessibility behavior.
9. Add automated UI/E2E coverage.
10. Perform UX quality review before Phase 4B is considered complete.

## Phase 4A gate

Phase 4B may begin only against this scope. Any newly discovered product capability belongs in a tracked follow-up issue unless it is required to satisfy this milestone's acceptance criteria.
