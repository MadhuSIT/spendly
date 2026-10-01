# Spendly — End-to-End Product Specification

## 1. Product definition

Spendly is a local-first Android personal finance application whose primary job is to automatically turn financial transaction SMS into a reliable, normalized financial ledger.

It is not merely an expense list.

The product must understand bank accounts, debit cards, credit cards, cash, expenses, income, refunds, reversals, fees, cash withdrawals, own-account transfers, credit-card purchases, credit-card bill payments, statements, due dates, partial and full payments, reminders, manual corrections, categorization, missed-SMS reconciliation, duplicate detection, and monthly insights.

Source of truth: the normalized ledger. Raw SMS is an input event; reports and insights are derived views.

## 2. Success metrics

Primary: percentage of real financial transactions correctly represented in the ledger without manual entry and without double counting.

Secondary metrics:
- parser success rate
- reconciliation recovery rate
- duplicate rate
- automatic categorization rate
- incorrect categorization rate
- card-payment matching rate
- insight coverage
- notification action rate

## 3. End-to-end ingestion

SMS_RECEIVED -> detect -> classify -> identify provider -> parse -> normalize -> validate -> resolve entity -> deduplicate -> persist ledger -> categorize -> notify

Real-time SMS processing and reconciliation use the same parser, validation and deduplication pipeline.

### Detection

Positive financial signals include debit, credit, spent, paid, purchase, transaction, payment, withdrawn, received, refund, reversed, UPI, card, POS, ATM and bill payment.

Negative signals include OTP, verification codes, promotional offers, loan/card marketing, login alerts and generic balance-only messages.

The initial detector is rule-based and measurable so false positives and false negatives can be tested.

### Parser architecture

SmsClassifier -> ProviderIdentifier -> ProviderParser -> GenericParser -> NormalizedTransaction

Provider-specific parsers may support HDFC, SBI, Axis, ICICI, Amex, UPI and other providers. GenericParser is the fallback.

Parser output is structured and must not directly write to the database.

Typical normalized fields include transaction type, amount, currency, merchant, source entity, destination entity, account/card last four, reference, transaction timestamp, confidence, parser source and parser version.

Parser versions are persisted for auditability.

## 4. Financial entities

Supported types:
- BANK_ACCOUNT
- CREDIT_CARD
- DEBIT_CARD
- CASH
- OTHER

Examples include HDFC Savings, SBI Savings, HDFC Credit Card, SBI Card, Amex and Cash Wallet.

Entity resolution uses provider/entity type plus masked identifier or last four. Last four alone is not globally unique.

Every transaction has a source entity and may have a destination entity.

## 5. Transaction ledger

Core fields:
- id
- source_entity_id
- destination_entity_id
- transaction_type
- amount
- currency
- merchant_name
- description
- category_id
- transaction_timestamp
- sms_received_timestamp
- raw_sms_reference
- reference_number
- upi_reference
- parser_source
- parser_version
- confidence
- status
- created_at
- updated_at

Useful operational fields include normalized/original merchant, masked account identifier, card last four, bank identifier, pending flag, review-required flag, duplicate group, related transaction, statement and payment relationships.

Transaction types:
- EXPENSE
- INCOME
- TRANSFER
- CARD_PAYMENT
- REFUND
- FEE
- CASH_WITHDRAWAL
- REVERSAL

Pending is a separate state.

## 6. Accounting rules

Amazon INR 5,000 on an SBI Card is an EXPENSE.

HDFC Bank to SBI Card for INR 5,000 is a CARD_PAYMENT, not another expense.

SBI to HDFC for INR 50,000 is a TRANSFER, not an expense.

A failed transaction must not create an expense. A refund reduces net spending. A reversal neutralizes the original transaction. Pending transactions can later be linked or updated.

Conceptual credit-card outstanding:
Opening outstanding + purchases + fees - refunds - payments - reversals.

Transfers and card payments are excluded from spending totals.

## 7. Deduplication and idempotency

Repeated processing of the same financial event must produce one ledger impact.

Preferred signals:
1. provider/reference/UPI identifier
2. deterministic fingerprint
3. source entity + amount + type + reference
4. fallback similarity using source + amount + merchant + time window

Never deduplicate solely by amount and merchant.

The same SMS processed twice, or once in real time and once during reconciliation, must converge to one result.

## 8. Categorization

Categories:
- Food
- Groceries
- Shopping
- Travel
- Fuel
- Bills
- Rent
- EMI / Loan
- Healthcare
- Entertainment
- Subscriptions
- Education
- Family
- Investment
- Bank Fees
- Card Fees
- Cash
- Transfer
- Others

Rule precedence:
1. explicit user rule
2. learned/user-confirmed merchant rule
3. generic rule/model
4. Others / Needs Review

Examples: Amazon -> Shopping; Swiggy -> Food.

User corrections are authoritative and persistent.

## 9. Credit-card lifecycle

Card fields include issuer, name, last four, credit limit, statement day, payment due day, minimum-due rule, billing cycle and preferred payment sources.

Statement fields include billing period, statement amount, minimum due, due date and status.

Statuses:
OPEN, GENERATED, PARTIALLY_PAID, PAID, OVERDUE, UNKNOWN

Payment matching considers destination card, amount, date, due date, outstanding, source account and reference.

Support full payment, minimum payment, partial payment, multiple payments and overpayment.

Never assume a payment equals the full statement.

## 10. Reconciliation

Use WorkManager as the periodic recovery mechanism.

Persist last_successful_sms_scan_timestamp and use an overlap window so messages near the checkpoint boundary are safely reprocessed.

Reconciliation must scan relevant SMS history, run the exact same parsing/validation/dedupe pipeline, recover missed financial messages, avoid duplicates, and update the checkpoint only after successful processing.

Real-time and reconciliation must converge to the same ledger result.

## 11. Notifications

Actionable notifications include low-confidence transactions, unknown merchants/categories, card payment due dates, generated statements, partial-payment balances and important reconciliation recovery.

Suggested due-date reminders are 5, 2 and 1 days before payment due. Paid statements stop reminders; partial payments continue reminders.

## 12. Monthly insights

Derived from the ledger:
- income
- gross spending
- refunds
- net spending
- category breakdown
- entity/account breakdown
- largest transactions
- largest merchants
- recurring expenses
- subscriptions
- uncategorized transactions
- unusual spending based on the user's own history
- upcoming obligations
- data coverage

Transfers and card payments are excluded from spending.

If only partial history exists, the UI must state that instead of inventing a full-month comparison.

Insights must be reproducible from ledger data.

## 13. Manual transactions

Manual entry uses the same ledger model as SMS transactions.

Use cases include cash expenses, missing transactions, opening balances, corrections, transfers, income and manual refunds.

Manual edits are auditable.

## 14. Privacy and security

Principles:
- local-first
- no third-party cloud by default
- minimum SMS permissions
- no raw SMS in analytics
- never log OTPs, full card numbers, full account numbers or sensitive SMS text
- mask identifiers
- secure local storage
- explicit export/delete controls
- optional cloud sync only as a future opt-in capability

## 15. Data model

Recommended core tables:
- financial_entities
- transactions
- transaction_raw_events
- transaction_relationships
- categories
- merchant_category_rules
- statements
- card_payments
- reminders
- reconciliation_checkpoints
- monthly_snapshots
- audit_events
- app_settings

The ledger is authoritative. Aggregates and snapshots must be rebuildable.

## 16. Auditability

Record parser result/version, source event, normalization, entity resolution, duplicate decisions, category changes, manual corrections, merges/splits, transaction relationships and card-payment matching.

Every transaction should be explainable and traceable to its source event.

## 17. Testing strategy

Parser fixtures must cover debit, credit, refund, reversal, failed transaction, UPI, card purchase, ATM, fee, transfer and bill payment.

Required test groups:
- parser tests
- provider identification tests
- entity resolution tests
- deduplication tests
- accounting invariant tests
- reconciliation tests
- card-payment matching tests
- notification tests
- persistence/migration tests
- end-to-end ingestion tests

Financial invariants:
1. own-account transfers never increase expense totals
2. card payments never increase expense totals
3. repeated SMS processing never creates additional financial impact
4. refunds cannot increase net expense
5. failed transactions cannot create expense
6. reversals neutralize their original impact
7. real-time and reconciliation produce identical ledger results
8. category corrections persist
9. rebuilding reports does not change the ledger
10. monthly reports are reproducible from ledger data

## 18. Delivery phases

Phase 1: SMS ingestion.

Phase 2: parser engine.

Phase 3: ledger and Room persistence.

Phase 4: categorization and merchant rules.

Phase 5: reconciliation with WorkManager.

Phase 6: credit cards, statements, payments and reminders.

Phase 7: financial correctness, invariants and auditability.

Phase 8: monthly insights.

Phase 9: privacy, performance, migrations, failure recovery and release hardening.

## 19. Definition of done

The product is functionally complete when a real financial SMS can enter through the Android SMS event path, be classified and parsed, resolve to the correct financial entity, become exactly one normalized ledger transaction, receive a category, participate correctly in balances/statements/insights, and remain recoverable through reconciliation without double counting.

Every transaction must be explainable, auditable and reproducible from its source event and ledger record.
