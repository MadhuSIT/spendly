# Phase 2A Design Record — Ledger and Financial Domain

## Status
**DESIGN READY FOR REVIEW**

## 1. Phase outcome
Phase 2 establishes Spendly's normalized local financial ledger as the single source of financial truth.

Phase 2 is limited to ledger/domain correctness. SMS ingestion/parsing, reconciliation, categorization, credit-card statement workflows, insights and release hardening remain later phases.

## 2. Authoritative execution sequence
The GitHub master execution contract (#3) and Phase 2 issue (#5) define the current delivery sequence:

1. Android foundation
2. Ledger/domain correctness
3. SMS ingestion + parsing
4. Core mobile UX
5. Reconciliation/resilience
6. Credit cards/statements/payments
7. Categorization/insights
8. Production hardening
9. Release candidate/production validation

The older numbered list in docs/PRODUCT_SPEC.md is inconsistent with this contract and must be corrected to avoid phase ambiguity. Product capabilities remain unchanged.

## 3. Source of truth
The normalized ledger in Room/SQLite is authoritative.

```text
Manual input / future SMS / future import
             ↓
       Domain validation
             ↓
      Canonical ledger
             ↓
     Derived totals / UI
```

No UI, report, snapshot or cache may become an independent financial source of truth.

## 4. Financial entities
Canonical entity types: BANK_ACCOUNT, CREDIT_CARD, DEBIT_CARD, CASH, OTHER.

Identity uses stable internal IDs. Provider + entity type + masked identifier/last-four may assist resolution, but last-four alone is not unique.

Foundation fields: id, type, provider/issuer, display name, masked identifier, last four where applicable, currency, status, created_at, updated_at.

## 5. Canonical transaction
Required fields: id, source_entity_id, destination_entity_id nullable, transaction_type, amount, currency, merchant_name nullable, description nullable, transaction_timestamp, status, reference_number nullable, upi_reference nullable, created_at, updated_at.

Audit/source fields: raw event reference nullable, parser source/version nullable, confidence nullable, review_required, relationship references.

Transaction types: EXPENSE, INCOME, TRANSFER, CARD_PAYMENT, REFUND, FEE, CASH_WITHDRAWAL, REVERSAL.

Operational status is separate from transaction type; pending/failed/cancelled/confirmed semantics must not become additional financial types.

## 6. Accounting semantics
- EXPENSE: real spending outflow; included in gross spending.
- INCOME: real income inflow; included in income totals.
- TRANSFER: movement between owned entities; never increases spending or income.
- CARD_PAYMENT: funding account to credit-card entity; never creates another expense.
- REFUND: reversal of prior spending; reduces net spending and should link to the original.
- REVERSAL: neutralizes a related transaction and must not create duplicate spending.
- FEE: real financial charge; included in spending.
- CASH_WITHDRAWAL: movement from account to cash; not itself spending.

## 7. Derived totals
- gross spending = applicable confirmed spending charges, excluding transfers and card payments
- refunds = confirmed refund impact
- net spending = gross spending minus refunds and reversal impact
- income = confirmed income
- transfers and card payments are excluded from spending

Exact reversal/refund relationship treatment must be deterministic and tested; no double subtraction.

## 8. Relationships
Persist explicit relationships for expense → refund, transaction → reversal, source account → transfer destination, funding account → card payment destination, and duplicate event → canonical transaction.

Relationships must be auditable and must not duplicate financial impact.

## 9. Manual transaction use cases
Manual entry uses the same canonical ledger model as future automatic ingestion.

Initial use cases: create expense, income, transfer, refund, fee, cash withdrawal; edit/correct transaction; link relationship; mark/review status where supported.

Validation occurs in domain/use-case code before repository persistence.

## 10. Repository/domain boundary
Compose never writes Room directly.

```text
UI → ViewModel → Use case/domain service → Repository interface → Room DAO
```

Domain accounting functions should be pure Kotlin where practical. Repository implementations own persistence mechanics; domain use cases own financial semantics.

## 11. Auditability
Financially meaningful mutations must retain enough metadata to explain creation source, manual correction, relationship changes, previous/current values where applicable, actor/source and timestamp.

Do not log sensitive raw SMS text.

## 12. Persistence and migration
Extend the existing Room database rather than creating a second financial database.

Schema changes require an explicit version increment, migration, migration test, persistence/reopen test and regression validation.

Financial tables must permit derived totals to be rebuilt from canonical transactions.

## 13. Financial invariants
1. Own-account transfers never increase expense totals.
2. Card payments never increase expense totals.
3. Failed transactions never create expenses.
4. Refunds reduce net spending.
5. Reversals neutralize related transactions.
6. Repeated application of the same ledger event is idempotent where applicable.
7. Manual and future automatic transactions share the same canonical model.
8. Rebuilding derived totals never mutates the ledger.
9. Persisted corrections survive recreation/restart.
10. Relationships do not duplicate financial impact.

## 14. Testing matrix
Unit: validation, accounting calculations, transfer/card-payment exclusion, refund/reversal semantics, relationship validation, derived totals, invalid-state rejection.

Room/integration: entity persistence, transaction persistence, relationships, migration, reopen/restart and transactional writes.

UI/Compose: manual transaction flow, validation errors, loading/content/empty/error states and navigation/back behavior.

E2E: create transaction → domain/repository/Room persistence → visible canonical ledger result → app recreation → identical financial state.

## 15. Explicit non-goals
SMS receiver; SMS classifier/parser/provider parsers; reconciliation scanner; merchant categorization engine; credit-card statement/due-date subsystem; notifications/reminders; monthly insights; cloud sync; full production onboarding; release signing/distribution.

## 16. Design acceptance criteria
- [x] Canonical source of truth defined.
- [x] Financial entity model defined.
- [x] Transaction model and status separation defined.
- [x] Accounting semantics defined.
- [x] Relationship strategy defined.
- [x] Repository/domain boundaries defined.
- [x] Manual transaction scope defined.
- [x] Derived-total rules defined.
- [x] Auditability requirements defined.
- [x] Migration/testing strategy defined.
- [x] Financial invariants defined.
- [x] Phase boundaries defined.
- [x] Product-spec phase-numbering discrepancy identified for correction.

## 17. Gate
Phase 2B may begin only after this design record is reviewed and Phase 2A is explicitly accepted.