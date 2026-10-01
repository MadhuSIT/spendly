# Spendly — UI Screen & Flow Specification

> **Status:** Review specification
>
> This document defines the intended Spendly screens, screen states, navigation, user actions, and end-to-end flows in words. It is the source of truth for UI implementation.
>
> This is **not** a visual mockup. Layouts, labels, actions, states, and transitions are specified precisely enough to implement the UI without inventing product behavior.

---

## 1. Product UX model

Spendly is a local-first financial ledger.

The UI must make one distinction obvious everywhere:

- **Financial activity** changes the ledger.
- **Transfers and card payments** move money between entities and must not become spending.
- **Raw SMS** is an input source, not the user-facing source of truth.
- Automated entries must be explainable and correctable.

The primary user journey is:

~~~text
                         ┌──────────────────────┐
                         │      SMS arrives     │
                         └──────────┬───────────┘
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │ Detect financial SMS │
                         └──────────┬───────────┘
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │ Parse + normalize    │
                         └──────────┬───────────┘
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │ Resolve entity       │
                         │ + deduplicate       │
                         └──────────┬───────────┘
                                    │
                     ┌──────────────┴──────────────┐
                     │                             │
                     ▼                             ▼
             ┌───────────────┐              ┌────────────────┐
             │ High confidence│              │ Needs review   │
             └──────┬────────┘              └───────┬────────┘
                    │                              │
                    ▼                              ▼
             ┌──────────────┐              ┌────────────────┐
             │ Ledger entry │◄─────────────│ User correction│
             └──────┬───────┘              └───────┬────────┘
                    │                              │
                    └──────────────┬───────────────┘
                                   ▼
                         ┌──────────────────────┐
                         │ Reports / Insights   │
                         └──────────────────────┘
~~~

---

# 2. Global navigation

Primary navigation has exactly four destinations:

~~~text
┌─────────────────────────────────────────────────────────────┐
│                         CURRENT SCREEN                      │
│                                                             │
│                    Screen content                           │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│  Home       Transactions       Accounts        Insights     │
└─────────────────────────────────────────────────────────────┘
~~~

### Global actions

These are available contextually rather than as permanent navigation destinations:

- Add transaction
- Review
- Search
- Notifications / attention
- Settings

### Navigation rules

| Action | Destination |
|---|---|
| Tap Home | Home |
| Tap Transactions | Transactions |
| Tap Accounts | Accounts |
| Tap Insights | Insights |
| Tap a transaction | Transaction Detail |
| Tap an account | Account Detail |
| Tap a credit card | Credit Card Detail |
| Tap a statement | Statement Detail |
| Tap a review item | Review / correction flow |
| Tap notification | Exact action target |
| Tap Add | Add Transaction |
| Back from detail | Previous screen |

---

# 3. Screen inventory

The implementation must cover these screens:

1. Home
2. Transactions
3. Transaction Detail
4. Review Queue
5. Review Item / Correction
6. Add Transaction — Expense
7. Add Transaction — Income
8. Add Transaction — Transfer
9. Accounts
10. Bank Account Detail
11. Credit Card Detail
12. Statement Detail
13. Card Payment / Payment Matching
14. Categories & Rules
15. Insights
16. Search & Filters
17. Notifications / Attention
18. Onboarding
19. Initial Reconciliation
20. Settings / Privacy

---

# 4. Screen specifications

## 4.1 Home

### Purpose

Give the user the current financial picture and immediately expose anything requiring attention.

### Header

- Current month selector.
- Review indicator when review items exist.
- Notification/attention indicator when actionable items exist.
- Settings entry.

### Primary summary

Show four values:

1. Net spending
2. Income
3. Spending
4. Refunds

Transfers and card payments must never be counted as spending.

### Partial-data state

If the available history does not cover the complete selected period:

~~~text
┌──────────────────────────────────────────┐
│ Partial data                             │
│ Showing transactions from 12 Sep onward. │
│ Some earlier activity may be missing.    │
└──────────────────────────────────────────┘
~~~

Never present incomplete history as a complete month.

### Account snapshot

Group:

- Bank accounts
- Credit cards
- Cash

Each entity row/card shows only information relevant to its type.

### Attention section

Possible items:

- Transactions needing review
- Uncategorized transactions
- Card payment due
- Statement requiring attention
- Reconciliation incomplete
- SMS permission unavailable
- Parser/provider issue

Each item must navigate directly to the action required.

### Recent transactions

Show approximately 5–8 recent ledger entries.

Each row:

~~~text
Merchant / title
Category · Entity · time
                         ₹ amount
Status (only when relevant)
~~~

Do not show raw SMS text.

### Home states

- Loading
- First-use empty
- Normal content
- Partial coverage
- Degraded/reconciliation warning
- Error

---

## 4.2 Transactions

### Purpose

The canonical ledger view.

### Top controls

- Search
- Month/date filter
- Entity filter
- Category filter
- Transaction type filter
- Review/pending filter

Filters should open as a bottom sheet.

### Transaction row

Required hierarchy:

~~~text
┌────────────────────────────────────────────────┐
│ Amazon                                         │
│ Shopping · SBI Card · 10:42 AM                 │
│                                      - ₹5,000  │
└────────────────────────────────────────────────┘
~~~

For transfers:

~~~text
HDFC Savings → SBI Card
Card payment · 11:15 AM
                                      ₹5,000
~~~

### Transaction detail navigation

Tap row → Transaction Detail.

### List states

- Loading
- Transactions available
- No transactions
- No results for filters
- Partial/degraded
- Error

---

## 4.3 Transaction Detail

### Purpose

Provide a complete, trustworthy explanation of one ledger entry.

### Sections

#### Amount

- Amount
- Currency
- Transaction type
- Pending/confirmed status

#### Merchant/title

- Normalized merchant
- Original merchant when useful

#### Entities

- Source entity
- Destination entity when applicable

#### Timing

- Transaction timestamp
- SMS received timestamp when available

#### References

- Bank/provider reference
- UPI reference
- Card/account last four when applicable

#### Classification

- Category
- Confidence
- Review status

#### Relationships

Show links to:

- Original transaction
- Refund
- Reversal
- Card payment
- Related statement
- Duplicate group

#### Why Spendly added this

Show the provenance path:

~~~text
Financial SMS detected
        ↓
Provider identified
        ↓
Account/card resolved
        ↓
Transaction parsed
        ↓
Duplicate check passed
        ↓
Category rule applied
        ↓
Ledger entry created
~~~

Do not expose sensitive raw SMS content by default.

### Actions

- Edit
- Change category
- Correct entity
- Link related transaction
- Resolve review item where applicable

Destructive actions must not be the primary action.

---

# 5. Review experience

## 5.1 Review Queue

### Purpose

Provide one place to resolve uncertainty introduced by automation.

### Queue item

Every item must explain:

1. What transaction is affected.
2. Why Spendly needs attention.
3. What the smallest correction is.

Example:

~~~text
┌────────────────────────────────────────────────┐
│ Amazon                                  -₹5,000│
│ Shopping · Unknown account                     │
│                                                │
│ Spendly could not identify the source card.    │
│                                                │
│ [Choose account]                     [Later]   │
└────────────────────────────────────────────────┘
~~~

### Possible review reasons

- Unknown account
- Low parser confidence
- Unknown merchant
- Possible duplicate
- Ambiguous transaction type
- Possible card payment
- Missing destination entity
- Other financial ambiguity

### Processing rule

After resolving one item:

~~~text
Review Queue
    │
    ▼
Resolve current item
    │
    ▼
Persist correction
    │
    ▼
Recalculate affected derived data
    │
    ▼
Open next review item
    │
    ├── More items → continue
    │
    └── None       → completion state
~~~

### Completion state

~~~text
┌──────────────────────────────────────────┐
│ All caught up                            │
│ Everything currently needs no attention. │
│                                          │
│              [Done]                      │
└──────────────────────────────────────────┘
~~~

---

## 5.2 Review Item / Correction

The correction screen should be focused on one decision.

### Unknown account

~~~text
Transaction
Amazon · ₹5,000

Which account/card was used?

( ) HDFC Credit Card •••• 1234
( ) SBI Card •••• 5678
( ) Amex •••• 9012

                     [Confirm]
~~~

### Unknown category

Show the suggested category first, then alternatives.

~~~text
Amazon · ₹5,000

Suggested category: Shopping

[ Shopping ]
[ Food ]
[ Bills ]
[ Travel ]
[ Other... ]

                     [Save]
~~~

If the user chooses to remember the merchant/category relationship, persist a merchant rule.

### Possible duplicate

Show both candidates:

~~~text
Possible duplicate

A  Amazon   ₹5,000   10:42 AM
B  Amazon   ₹5,000   10:43 AM

[Keep A]  [Keep B]  [Keep both]
~~~

Never silently delete a financial record from this screen.

### Ambiguous type

~~~text
How should this transaction be recorded?

( ) Expense
( ) Card payment
( ) Transfer
( ) Other
~~~

---

# 6. Add Transaction

The flow is:

~~~text
Type
  ↓
Amount
  ↓
Date / time
  ↓
Source
  ↓
Destination (when required)
  ↓
Merchant / title
  ↓
Category
  ↓
Note
  ↓
Save
~~~

The form must dynamically hide fields that are not relevant.

## 6.1 Add Expense

Required:

- Amount
- Date/time
- Source
- Merchant/title
- Category

Optional:

- Note

Flow:

~~~text
Add
 ↓
Expense
 ↓
Amount
 ↓
Source
 ↓
Merchant
 ↓
Category
 ↓
Save
 ↓
Transaction Detail / previous context
~~~

## 6.2 Add Income

Required:

- Amount
- Date/time
- Source/destination entity according to ledger semantics
- Title/source
- Category

## 6.3 Add Transfer

Required:

- Amount
- Date/time
- Source entity
- Destination entity
- Note/title optional

Flow:

~~~text
Add
 ↓
Transfer
 ↓
Amount
 ↓
Source
 ↓
Destination
 ↓
Date/time
 ↓
Save
 ↓
Ledger records movement, not spending
~~~

Source and destination must be visually distinct.

---

# 7. Accounts

Accounts overview groups entities:

~~~text
ACCOUNTS
│
├── Bank accounts
│   ├── HDFC Savings
│   └── SBI Savings
│
├── Credit cards
│   ├── SBI Card
│   └── Amex
│
└── Cash
    └── Cash Wallet
~~~

Each entity card shows:

- Name
- Masked identifier / last four where applicable
- Balance or outstanding
- Status
- Recent activity entry point

---

# 8. Bank Account Detail

### Header

- Account name
- Masked identifier
- Current balance when known

### Content

- Recent transactions
- Income
- Expenses
- Transfers
- Account configuration

The account screen must filter the canonical ledger by entity; it must not create a second interpretation of transactions.

---

# 9. Credit Card Detail

### Header summary

Show:

- Current outstanding
- Available limit when known
- Statement amount
- Minimum due
- Due date
- Payment status

### Sections

~~~text
Credit Card
│
├── Current outstanding
│
├── Upcoming / current statement
│   ├── Statement amount
│   ├── Minimum due
│   ├── Due date
│   └── Payment status
│
├── Purchases
├── Payments
├── Refunds
└── Activity
~~~

### Accounting distinction

~~~text
Card purchase
     = spending

Bank → Card payment
     = money movement to settle card
     ≠ new spending
~~~

---

# 10. Statement Detail

### Header

- Statement period
- Statement amount
- Minimum due
- Due date
- Status

### Statuses

- OPEN
- GENERATED
- PARTIALLY_PAID
- PAID
- OVERDUE
- UNKNOWN

### Activity

Show:

- Purchases
- Fees
- Refunds
- Reversals
- Payments applied to the statement

### Payment progress

~~~text
Statement amount     ₹40,000
Payments applied     ₹25,000
Remaining            ₹15,000
Minimum due           ₹4,000
Due date              15 Oct
Status                PARTIALLY_PAID
~~~

Never infer that a payment equals full settlement unless matching establishes it.

---

# 11. Card Payment / Payment Matching

### Matching flow

~~~text
Bank transaction detected
        │
        ▼
Possible card payment
        │
        ▼
Find destination card
        │
        ▼
Find open statement / outstanding
        │
        ▼
Suggest matching payment
        │
   ┌────┴────┐
   │         │
 Confirm   Ambiguous
   │         │
   ▼         ▼
 Link      Review
~~~

### Payment choices

- Full statement payment
- Minimum payment
- Custom amount
- Partial payment
- Multiple payments
- Overpayment

A matched card payment changes the card outstanding/payment state. It does not increase spending totals.

---

# 12. Categories & Rules

Initial categories:

- Food
- Groceries
- Shopping
- Travel
- Fuel
- Bills
- Rent
- EMI/Loan
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

Categorization precedence:

~~~text
Explicit user rule
       ↓
User-confirmed merchant rule
       ↓
Generic categorization
       ↓
Others / Needs Review
~~~

User corrections are authoritative for future matching unless changed.

---

# 13. Insights

Structure:

~~~text
Insights
│
├── Month selector
├── Spending overview
├── Category breakdown
├── Entity breakdown
├── Largest transactions
├── Recurring / subscriptions
├── Covered-period changes
└── Upcoming obligations
~~~

If historical coverage is insufficient:

~~~text
Not enough history yet

Spendly needs more complete ledger history
before showing a meaningful monthly comparison.
~~~

Do not fabricate month-over-month comparisons.

Transfers and credit-card payments are excluded from spending totals.

---

# 14. Search & Filters

Search operates on the normalized ledger.

Searchable concepts:

- Merchant
- Description/title
- Reference
- Entity
- Category where supported

Filters:

- Date/month
- Entity
- Category
- Type
- Review state
- Pending state

Flow:

~~~text
Transactions
     │
     ▼
Search / Filter
     │
     ▼
Bottom sheet
     │
     ▼
Apply
     │
     ▼
Filtered ledger
~~~

Show active filter chips and a clear-all action.

---

# 15. Notifications / Attention

Notify only when action is useful:

- Review item needs attention
- Credit-card payment approaching due date
- Statement generated
- Reconciliation needs attention
- Permission/parser issue affecting ingestion

Do not notify for every successfully parsed transaction.

Deep-link rules:

~~~text
Notification
     │
     ├── Review transaction → Review Item
     ├── Card payment due  → Card Payment / Statement
     ├── Statement         → Statement Detail
     └── Reconciliation    → Reconciliation status
~~~

---

# 16. Onboarding

First-run flow:

~~~text
Welcome
  ↓
What Spendly does
  ↓
Privacy explanation
  ↓
Request SMS permission
  ↓
Add / confirm financial entities
  ↓
Initial reconciliation
  ↓
Review imported transactions
  ↓
Home
~~~

Do not request every optional permission on first launch.

If SMS permission is denied, the app remains usable but explains that automatic SMS ingestion is unavailable until permission is granted.

---

# 17. Initial Reconciliation

Flow:

~~~text
Start reconciliation
       │
       ▼
Scan supported SMS history
       │
       ▼
Classify
       │
       ▼
Parse
       │
       ▼
Normalize
       │
       ▼
Resolve entities
       │
       ▼
Deduplicate
       │
       ▼
Persist ledger
       │
       ▼
Categorize
       │
       ▼
Create review items
       │
       ▼
Summary
~~~

Summary must show:

- Messages scanned
- Financial messages detected
- Transactions created
- Duplicates ignored
- Items needing review
- Coverage date range

Example:

~~~text
Reconciliation complete

1,248 messages scanned
   186 financial messages
   172 ledger transactions created
    11 duplicates ignored
     3 need review

Coverage: 01 Sep – 01 Oct

[Review 3 items]    [Go to Home]
~~~

---

# 18. Settings / Privacy

Sections:

- Privacy
- SMS ingestion
- Reconciliation
- Notifications
- Categories/rules
- Financial entities
- Data/export
- App preferences

The UI should explain:

- Data is local-first.
- Why SMS permission is required.
- What data is stored.
- How automated transactions are processed.
- Appropriate data-management actions.

---

# 19. UI state model

Every major screen explicitly supports:

~~~text
LOADING
   │
   ├──► CONTENT
   │
   ├──► EMPTY
   │
   ├──► PARTIAL / DEGRADED
   │
   └──► ERROR
~~~

Transaction-specific states:

- PENDING
- NEEDS_REVIEW
- CONFIRMED
- LINKED
- REFUNDED
- REVERSED

Do not use unexplained null values as UI state.

---

# 20. End-to-end flows

## 20.1 Normal automatic expense

~~~text
SMS arrives
  ↓
Financial SMS detected
  ↓
Provider identified
  ↓
Expense parsed
  ↓
Source entity resolved
  ↓
Duplicate check
  ↓
Category resolved
  ↓
Ledger entry created
  ↓
Home / Transactions updated
~~~

## 20.2 Automatic transaction requiring review

~~~text
SMS
 ↓
Parse
 ↓
Unknown account
 ↓
Ledger entry + NEEDS_REVIEW
 ↓
Review indicator appears
 ↓
User opens Review Queue
 ↓
Selects account
 ↓
Correction persisted
 ↓
Transaction becomes CONFIRMED
 ↓
Next review item
~~~

## 20.3 Duplicate SMS

~~~text
SMS A
 ↓
Transaction created
 ↓
Same SMS / equivalent event arrives again
 ↓
Deduplication
 ↓
Existing transaction matched
 ↓
No second financial impact
~~~

## 20.4 Card purchase

~~~text
Amazon SMS
 ↓
EXPENSE
 ↓
Source = SBI Card
 ↓
Category = Shopping
 ↓
Spending increases
 ↓
Card outstanding increases
~~~

## 20.5 Card bill payment

~~~text
HDFC Bank debit
        ↓
Possible card payment
        ↓
Destination = SBI Card
        ↓
CARD_PAYMENT
        ↓
SBI Card outstanding decreases
        ↓
Spending unchanged
~~~

## 20.6 Own-account transfer

~~~text
SBI Savings
    │
    │ ₹50,000
    ▼
HDFC Savings
    │
    ▼
TRANSFER
    │
    ├── SBI balance changes
    ├── HDFC balance changes
    └── Spending unchanged
~~~

## 20.7 Refund

~~~text
Original purchase
      ↓
Refund SMS
      ↓
REFUND linked to original
      ↓
Net spending decreases
~~~

## 20.8 Reversal

~~~text
Original transaction
      ↓
Reversal detected
      ↓
Link reversal
      ↓
Original financial impact neutralized
~~~

## 20.9 Partial card payment

~~~text
Statement = ₹40,000
        ↓
Payment = ₹10,000
        ↓
Statement = PARTIALLY_PAID
        ↓
Remaining = ₹30,000
        ↓
Reminder remains active when applicable
~~~

## 20.10 Monthly review

~~~text
Home
 ↓
Select month
 ↓
Review summary
 ↓
Open category / entity / transaction details
 ↓
Resolve uncategorized or review items
 ↓
Open card obligations
 ↓
Return to Home
~~~

---

# 21. Screen-to-screen map

~~~text
                           ┌──────────────┐
                           │    ONBOARD   │
                           └──────┬───────┘
                                  │
                                  ▼
                     ┌────────────────────────┐
                     │ INITIAL RECONCILIATION │
                     └────────────┬───────────┘
                                  │
                                  ▼
┌────────────┐      ┌──────────────────────┐      ┌───────────────┐
│TRANSACTIONS│◄────►│        HOME          │◄────►│   ACCOUNTS    │
└──────┬─────┘      └──────────┬───────────┘      └──────┬────────┘
       │                        │                         │
       ▼                        ▼                         ▼
┌──────────────┐       ┌────────────────┐       ┌──────────────────┐
│ TRANSACTION  │       │ REVIEW QUEUE   │       │ ENTITY DETAIL    │
│ DETAIL       │       └───────┬────────┘       └────────┬─────────┘
└──────────────┘               │                         │
                               ▼                         ▼
                      ┌────────────────┐       ┌──────────────────┐
                      │ REVIEW /       │       │ CREDIT CARD      │
                      │ CORRECTION     │       │ DETAIL           │
                      └────────────────┘       └────────┬─────────┘
                                                        │
                                                        ▼
                                               ┌──────────────────┐
                                               │ STATEMENT DETAIL │
                                               └────────┬─────────┘
                                                        │
                                                        ▼
                                               ┌──────────────────┐
                                               │ PAYMENT MATCHING │
                                               └──────────────────┘

       ┌────────────────┐
       │ ADD TRANSACTION│
       └───────┬────────┘
               │
       ┌───────┼───────────────┐
       ▼       ▼               ▼
    Expense   Income        Transfer
~~~

---

# 22. UI implementation rules

1. Build screens against a shared domain/view-model state model.
2. Do not duplicate accounting logic inside individual screens.
3. Transactions and account screens read the same canonical ledger.
4. Home and Insights use derived reporting data from the ledger.
5. Review corrections update the canonical ledger and immediately refresh affected screens.
6. Card purchases and card payments have visually distinct labels and semantics.
7. Transfer entries never appear as spending.
8. Raw SMS text is not displayed by default.
9. Every automated transaction has an explainable provenance path.
10. Every major screen has loading, empty, degraded/partial and error states.
11. Every destructive or irreversible action requires explicit confirmation.
12. Accessibility semantics are part of the initial implementation.
13. UI tests cover critical navigation and accounting presentation.
14. UI works with local/mock ledger data before real SMS ingestion is connected.
15. Real SMS ingestion plugs into the same UI state model without redesigning navigation.

---

# 23. Implementation sequence

~~~text
1.  App shell + Material 3 theme
        ↓
2.  Bottom navigation
        ↓
3.  Home
        ↓
4.  Transactions + Transaction Detail
        ↓
5.  Add Transaction
        ↓
6.  Review Queue + Correction
        ↓
7.  Accounts + Entity Detail
        ↓
8.  Credit Card + Statement + Payment Matching
        ↓
9.  Categories + Rules
        ↓
10. Search + Filters
        ↓
11. Notifications / Attention
        ↓
12. Insights
        ↓
13. Onboarding + Initial Reconciliation
        ↓
14. Settings + Privacy
        ↓
15. Loading / Empty / Error / Degraded states
        ↓
16. Accessibility + UI tests
~~~

The first milestone should be a complete navigable UI using deterministic local/mock ledger data.

No screen should be implemented as a disconnected visual mockup. Every screen must participate in the navigation and state flows described in this document.
