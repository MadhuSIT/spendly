# Phase 3 — SMS Ingestion & Parser Engine Design

## 1. Problem and user outcome

Spendly must turn financial SMS into normalized ledger transactions automatically, while preserving the Phase 2 ledger as the only financial source of truth.

User outcome:

`SMS received -> financial message detected -> classified -> provider identified -> parsed -> normalized -> validated -> entity resolved -> deduplicated -> ledger transaction persisted`

The system must be safe when the same message is processed more than once and must never silently convert an ambiguous or failed parse into financial impact.

## 2. Scope

### In scope
- Android SMS event ingestion boundary.
- Financial-message detection and negative filters.
- Message classification.
- Provider identification.
- Provider-specific parser framework.
- Generic fallback parser.
- Canonical normalized parser result.
- Parser version and confidence metadata.
- Financial entity resolution using provider/type/identifier signals.
- Deterministic/idempotent duplicate detection.
- Reviewable parse failures/ambiguous messages.
- Fixture-driven parser regression tests.
- Integration with the existing Phase 2 ledger write path.
- Privacy-safe logging: raw SMS content must not appear in normal logs.

### Explicitly out of scope
- Categorization/merchant rules.
- Reconciliation/backfill scheduling and missed-SMS recovery policy.
- Credit-card statements/payment matching.
- Monthly insights.
- Full review-queue UX beyond the minimum state needed to surface an unprocessable message.
- Cloud/backend processing.
- Permanent foreground service.

## 3. Architecture

The ingestion pipeline is separated from the ledger domain:

`SMS_RECEIVED`
-> `SmsDetector`
-> `SmsClassifier`
-> `ProviderIdentifier`
-> `ProviderParser`
-> `GenericParser`
-> `NormalizedSmsTransaction`
-> `LedgerValidation`
-> `EntityResolver`
-> `Deduplication`
-> `Create/Update Ledger Transaction`

Rules:
1. Parsers produce structured output; parsers never write Room directly.
2. The ledger repository remains the only financial persistence boundary.
3. Real-time SMS handling and future reconciliation must call the same parsing/normalization/dedupe pipeline.
4. No permanent foreground service is introduced.
5. Raw SMS is retained only where explicitly required for audit/review and must not be emitted to ordinary logs.

## 4. Detection and classification

### Positive financial signals
Examples include: debited, credited, spent, paid, purchase, transaction, payment, withdrawn, received, refund, reversed, UPI, card, POS, ATM and bill payment.

### Negative filters
Messages dominated by these signals must be rejected before financial parsing:
- OTP/verification codes.
- Promotional/marketing offers.
- Loan/card offers.
- Login/security alerts without a financial transaction.
- Balance-only notifications.

Detection must be measurable with a fixture corpus so false positives and false negatives can be tracked.

## 5. Normalized parser contract

Parsers return a normalized object containing:
- transaction type
- amount in minor units
- currency
- merchant
- source/destination hints
- provider
- masked account/card identifier
- last four when available
- transaction timestamp
- SMS received timestamp
- reference number / UPI reference
- parser source
- parser version
- confidence
- parse status / ambiguity reason

The parser result is not itself the ledger transaction. Validation and entity resolution occur before ledger persistence.

## 6. Transaction semantics

Map messages into the Phase 2 transaction model:

- debit/purchase -> EXPENSE
- credit/receipt -> INCOME
- refund -> REFUND
- reversal -> REVERSAL relationship
- fee -> FEE
- ATM withdrawal -> CASH_WITHDRAWAL
- own-account movement -> TRANSFER
- credit-card bill payment -> CARD_PAYMENT
- failed/rejected -> FAILED status with no financial impact

Ambiguous classification must become reviewable rather than silently choosing a financial meaning.

## 7. Entity resolution

Resolution order:
1. provider + financial entity type + stable masked identifier/last four.
2. provider + stable account/card identifier.
3. explicit source/destination hints where available.
4. unresolved -> review required.

Last four alone is not globally unique.

A resolver must never silently attach a transaction to an unrelated entity merely because the amount or merchant matches.

## 8. Deduplication / idempotency

Preferred identity signals:
1. provider + reference/UPI reference.
2. deterministic event fingerprint.
3. source entity + amount + type + stable reference.
4. fallback source + amount + merchant + bounded timestamp window.

Amount + merchant alone is never sufficient for global deduplication.

Processing the same SMS through real-time ingestion and later reconciliation must produce one financial impact.

## 9. Failure and review behavior

Failures are classified:
- NOT_FINANCIAL
- UNSUPPORTED_PROVIDER
- UNPARSEABLE
- AMBIGUOUS_ENTITY
- AMBIGUOUS_TRANSACTION
- DUPLICATE
- INVALID_NORMALIZED_RESULT

Only validated, resolved, non-duplicate results may enter the ledger.

A parser failure must be observable/reviewable without leaking SMS content into normal logs.

## 10. End-to-end proof target

Phase 3D must demonstrate at least:

1. Feed a realistic financial SMS fixture through the same application ingestion pipeline.
2. Detect and classify it as financial.
3. Identify provider/parser.
4. Produce normalized transaction.
5. Resolve the correct Phase 2 entity.
6. Deduplicate and persist exactly one ledger transaction.
7. Verify the transaction is visible in the ledger UI.
8. Process the same message again.
9. Verify ledger financial impact remains unchanged.
10. Exercise an unparseable/ambiguous fixture and verify it does not create financial impact.

Real-device SMS permission/receipt validation is separately identified as a device-dependent evidence item if the CI environment cannot provide it.

## 11. Acceptance criteria

- Representative fixtures cover debit, credit, refund, reversal, failed, UPI, card, ATM, fee, transfer and bill-payment cases.
- OTP, promotional and balance-only fixtures are rejected.
- Provider-specific and generic parser behavior is deterministic.
- Parser output is validated before ledger persistence.
- Correct financial entity resolution is demonstrated.
- Duplicate processing produces exactly one financial impact.
- Parser failure never silently creates a financial transaction.
- Raw SMS content is absent from normal application logs.
- Existing Phase 2 financial invariants remain green.
- End-to-end ingestion-to-ledger behavior is demonstrated.
- No permanent foreground service is introduced.
- Main remains buildable and releasable throughout the phase.

## 12. Quality targets

- Determinism: identical fixture + parser version produces identical normalized output.
- Idempotency: repeated processing of one financial event produces one financial impact.
- Safety: ambiguous/unparseable input produces zero financial impact until resolved.
- Regression safety: every discovered parser defect becomes a fixture.
- Observability: parser source/version/confidence and failure reason are inspectable without logging raw SMS.
- Performance: SMS handling should return promptly and defer non-critical work to lifecycle-safe background execution where necessary; no unmeasured hard latency claim is made before profiling.
- Maintainability: provider-specific parsing is isolated behind a common parser contract.

## 13. Dependencies and risks

Dependencies:
- Phase 2 ledger repository/domain.
- Android SMS APIs and permission model.
- Fixture corpus representing realistic provider formats.

Risks:
- SMS formats vary by provider and change over time.
- Sender IDs can be inconsistent.
- The same provider may use multiple message templates.
- Account/card identifiers can be masked differently.
- A single SMS can contain ambiguous semantics.
- Device/OS restrictions can affect SMS delivery testing.

Mitigations:
- versioned provider parsers.
- fixture regression suite.
- generic fallback with explicit confidence/review state.
- deterministic entity resolution.
- idempotent dedupe.
- no direct parser-to-database writes.
- explicit deferred device evidence where CI cannot prove it.

## 14. Phase boundary

Phase 3 builds the trustworthy ingestion/parser foundation and its ledger integration. Categorization, reconciliation policy, credit-card statement matching, insights and broader UX remain later phases.

## Gate

This design freezes the Phase 3 scope. Implementation (#28) may begin only after this design is accepted and #27 is explicitly closed.
