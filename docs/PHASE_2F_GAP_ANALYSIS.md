# Phase 2F — Gap Analysis & Improvement

## Gate

Phase 2E documented P2 UX gaps. Phase 2F resolves the gaps that can be improved safely within the current ledger slice without inventing later product domains.

## Implemented improvements

### 1. Transaction hierarchy
Transaction rows now expose:
- merchant/title
- canonical transaction type
- transaction timestamp
- amount
- non-confirmed status when relevant

### 2. Explicit empty state
The Transactions screen now distinguishes an empty ledger from a populated ledger and gives the user a clear next action.

### 3. Submission/recovery feedback
Add Expense now:
- prevents duplicate submission while the save is running
- disables editing/cancel during persistence
- shows Saving feedback
- clears stale errors when the user edits input
- keeps a visible semantic error state when persistence fails

### 4. Visual hierarchy
Home, Add Expense and Transactions now use Material 3 typography hierarchy consistently.

## Deferred gaps

The following remain intentionally deferred because they require capabilities not yet implemented:

- Source/entity selection in Add Expense: requires account/entity management UX.
- Category selection/rules: later categorization capability.
- Transaction detail/provenance: requires a dedicated detail/edit/linking slice.
- Full Loading/Partial/Degraded state model: requires reconciliation/SMS lifecycle semantics.
- Manual TalkBack/contrast/dynamic-type/device performance review: no physical Android device is available.
- Transfer/refund/reversal/fee/cash-withdrawal user flows: domain support exists, but their user-facing flows belong to subsequent vertical slices.

These are recorded rather than silently treated as complete.

## Quality decision

No P0/P1 issue was found. The improvements above address the actionable P2 UX issues that can be resolved without crossing later-phase boundaries.

## Validation

CI must pass before this gap-analysis phase can be accepted. Phase 2G remains gated until this PR is merged and final acceptance/sign-off is recorded.
