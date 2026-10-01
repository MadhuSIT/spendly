# Spendly — End-to-End Architecture

## 1. Architecture goals

The architecture prioritizes financial correctness, local-first privacy, deterministic behavior, recoverability and testability.

The normalized ledger is the authoritative data model. Raw SMS events, parser output and derived reports exist around it.

## 2. Runtime architecture

Android application:
- Jetpack Compose UI
- ViewModel/state layer
- domain/use-case layer
- repository layer
- Room/SQLite persistence
- SMS ingestion receiver
- WorkManager reconciliation
- Android notification subsystem

High-level flow:

SMS_RECEIVED
  -> SmsReceiver
  -> SmsClassifier
  -> ProviderIdentifier
  -> ProviderParser or GenericParser
  -> NormalizedTransaction
  -> Validation
  -> EntityResolver
  -> Deduplication
  -> LedgerRepository
  -> Categorization
  -> Notifications

Reconciliation follows the same pipeline:

WorkManager
  -> SMS history scan
  -> same classifier/parser/validation/entity/dedupe pipeline
  -> LedgerRepository
  -> checkpoint update

## 3. Layer boundaries

### UI

Compose screens should never parse SMS or implement accounting rules.

Responsibilities:
- display ledger state
- accept user corrections
- expose account/card management
- expose review queues
- display statements and insights
- trigger manual transactions
- expose privacy/export/delete controls

### Presentation

ViewModels expose immutable UI state and invoke domain use cases.

Examples:
- ObserveTransactions
- ReviewTransaction
- CreateManualTransaction
- ManageFinancialEntity
- CategorizeTransaction
- ObserveStatement
- ResolveCardPayment
- ObserveMonthlyInsights

### Domain

Pure Kotlin business rules:
- transaction normalization
- accounting semantics
- entity resolution
- deduplication
- categorization
- statement calculations
- card-payment matching
- reconciliation decisions
- insight calculations

Domain code should be testable without Android framework dependencies.

### Data

Repositories coordinate Room, SMS access and domain processing.

The data layer must not bypass domain validation for convenience.

## 4. SMS ingestion

Use an Android SMS broadcast/event mechanism to wake the application when a relevant message arrives.

Do not run a permanent 24/7 foreground service for SMS ingestion.

The receiver should:
1. obtain the event
2. perform lightweight filtering
3. enqueue or invoke processing
4. return quickly

Heavy or retryable work should be delegated to an appropriate background execution mechanism.

The app must request only the minimum SMS permissions required by its supported ingestion model.

## 5. Parser engine

Components:

SmsClassifier
- determines whether an SMS is financially relevant

ProviderIdentifier
- identifies likely bank/card/payment provider

ProviderParser
- provider-specific deterministic extraction

GenericParser
- fallback extraction for unknown formats

Normalizer
- maps parser output to the canonical transaction model

Validator
- rejects impossible or unsafe results

ParserResult must contain structured data rather than directly modifying persistence.

Every parser result records parser source and version.

## 6. Entity resolution

EntityResolver maps a normalized source/destination identifier to a financial entity.

Matching order should consider:
1. provider
2. entity type
3. card/account identifier
4. last four
5. user-defined entity mapping

Ambiguous matches must not silently choose an entity. They should be marked for review.

## 7. Ledger persistence

Room is the local source of truth.

Recommended entities:
- FinancialEntityEntity
- TransactionEntity
- TransactionRawEventEntity
- TransactionRelationshipEntity
- CategoryEntity
- MerchantCategoryRuleEntity
- StatementEntity
- CardPaymentEntity
- ReminderEntity
- ReconciliationCheckpointEntity
- MonthlySnapshotEntity
- AuditEventEntity
- AppSettingsEntity

Database writes should happen through repositories/use cases so financial invariants cannot be bypassed.

## 8. Transaction state and relationships

A transaction has a type and operational status.

Examples:
- EXPENSE + CONFIRMED
- EXPENSE + PENDING
- CARD_PAYMENT + CONFIRMED
- TRANSFER + CONFIRMED
- REFUND + CONFIRMED
- REVERSAL + CONFIRMED

Relationships connect:
- original transaction -> reversal
- original expense -> refund
- card purchase -> statement
- bank transaction -> card payment
- duplicate event -> canonical transaction

Avoid duplicating financial impact merely to represent a relationship.

## 9. Deduplication

Deduplication must be deterministic and idempotent.

Primary keys/signals:
- provider reference
- UPI reference
- bank/card reference
- normalized fingerprint

Fallback matching:
- source entity
- transaction type
- amount
- normalized merchant
- time window

Amount + merchant alone is insufficient.

The same event processed by real-time ingestion and reconciliation must resolve to the same canonical transaction.

## 10. Credit-card subsystem

Credit-card management is built on top of the ledger.

Card:
- identity
- issuer
- last four
- credit limit
- statement schedule
- payment due schedule

Statement:
- billing period
- statement amount
- minimum due
- due date
- status

Payment matcher evaluates source account, destination card, amount, date, outstanding amount and references.

Payment status is derived from linked payments rather than assumed from one transaction.

## 11. Reconciliation subsystem

WorkManager periodically scans a bounded SMS history window.

Persist a successful scan checkpoint.

Always use an overlap interval around the checkpoint because event delivery and timestamp boundaries can race.

Processing must be idempotent.

Only advance the checkpoint after the processing transaction succeeds.

If a run fails, retry without losing the previous checkpoint.

## 12. Categorization

Categorization is a domain service.

Precedence:
1. explicit user rule
2. user-confirmed merchant rule
3. deterministic built-in rule
4. optional future model suggestion
5. Others / Needs Review

A user correction must update the persistent rule when appropriate and must not be overwritten by later generic inference.

## 13. Insights

Insights are projections over the ledger.

Core calculations:
- income
- gross expense
- refunds
- net expense
- category totals
- entity totals
- largest transactions
- recurring transactions
- subscriptions
- unusual spending
- upcoming card obligations

Transfers and card payments are not spending.

Monthly comparisons require sufficient historical coverage. Partial data must be represented as partial.

Snapshots can improve performance but must always be rebuildable from ledger data.

## 14. Audit trail

Every mutation that changes financial meaning should be auditable.

Examples:
- parser version
- category correction
- transaction edit
- duplicate merge
- transaction split
- relationship creation
- card-payment match
- reversal/refund link

Do not log sensitive raw SMS content.

## 15. Privacy and security

Sensitive information should stay local by default.

Never put raw SMS, OTPs, full card numbers or full account numbers into ordinary logs.

Use masked identifiers in diagnostics.

Secure sensitive local data and expose explicit user controls for export and deletion.

Any future sync must be opt-in and designed around least privilege.

## 16. Failure handling

Failure categories:
- permission unavailable
- malformed SMS
- unknown provider
- parser failure
- ambiguous entity
- duplicate candidate
- database failure
- notification failure
- reconciliation failure

Rules:
- parser failure must not create a guessed financial transaction
- ambiguous entity must be reviewable
- database failure must leave the event recoverable
- notification failure must not roll back a valid ledger transaction
- reconciliation failures must preserve the last successful checkpoint

## 17. Testing architecture

Unit tests:
- parsers
- classifier
- normalizer
- validator
- entity resolver
- dedupe engine
- categorization
- accounting calculations
- statement/payment matching
- insights

Integration tests:
- Room repositories
- ingestion pipeline
- reconciliation
- transaction relationships
- migrations

End-to-end tests:
- representative SMS -> canonical ledger transaction
- duplicate SMS -> one transaction
- real-time then reconciliation -> one transaction
- card purchase + card payment -> one expense plus one payment
- refund -> reduced net spending
- reversal -> neutralized original
- failed transaction -> no expense

## 18. CI expectations

Every change should run:
- Kotlin compilation
- unit tests
- integration tests where configured
- Android lint/static analysis
- formatting checks
- database migration tests

CI must block merges when financial correctness tests fail.

## 19. Evolution

Initial implementation should remain deterministic and local.

Future extensions can add:
- additional bank/card providers
- richer merchant learning
- optional on-device ML
- encrypted backup
- opt-in cloud sync
- export/import
- advanced budgeting

These extensions must not weaken the ledger's source-of-truth principle or introduce duplicate financial impact.

## 20. Architectural invariant

No feature may create financial impact outside the normalized ledger.

SMS, manual entry, reconciliation, imports and future integrations are all input paths into the same canonical ledger and the same accounting rules.
