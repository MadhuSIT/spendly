# Phase 1A Design Record — Android Foundation

## Status
**DESIGN READY FOR REVIEW — NO IMPLEMENTATION YET**

This document is the concrete design output for #33. It converts the repository's product, architecture, UX, and delivery rules into an implementation contract.

---

## 1. Phase 1 outcome

Phase 1 is not intended to demonstrate the full finance product.

Its outcome is a **real, production-oriented Android foundation** that proves:

```text
Android App
   ↓
Compose UI
   ↓
ViewModel / UI State
   ↓
Domain / Use Cases
   ↓
Repository Boundary
   ↓
Room / SQLite
```

The foundation must be capable of growing into the complete local-first ledger without requiring an architectural rewrite.

---

## 2. Architecture decision

### Decision

Use a layered, unidirectional architecture:

```text
┌─────────────────────────────┐
│       Compose UI            │
│  renders state + emits      │
│  user intents               │
└──────────────┬──────────────┘
               │
               ▼
┌─────────────────────────────┐
│ ViewModel / UI State        │
│ lifecycle-aware state       │
│ coordinates user actions   │
└──────────────┬──────────────┘
               │
               ▼
┌─────────────────────────────┐
│ Domain / Use Cases          │
│ product behavior            │
│ pure/testable where possible│
└──────────────┬──────────────┘
               │
               ▼
┌─────────────────────────────┐
│ Repository Interfaces       │
│ stable domain boundary      │
└──────────────┬──────────────┘
               │
               ▼
┌─────────────────────────────┐
│ Room / SQLite               │
│ local authoritative state   │
└─────────────────────────────┘
```

### Dependency rule

Dependencies point inward/downward toward stable abstractions.

UI must not directly access:
- Room
- DAOs
- SQL
- raw persistence models
- infrastructure services

The domain must not depend on Compose.

---

## 3. Source-of-truth rule

The local database is the authoritative persisted source of truth.

Derived UI state is disposable.

Reports, summaries, filters, and future insights must be derivable from persisted domain data rather than maintaining independent financial truth.

```text
Persisted domain state
        │
        ├── Home
        ├── Transactions
        ├── Accounts
        ├── Cards
        └── Insights
```

A mutation must not require manually updating several screens.

The database change propagates through observable state.

---

## 4. Local-first persistence

### Decision

Use **SQLite through Room** for the core local product.

No backend is required for the core ledger.

Room provides:
- schema management
- migrations
- typed queries
- transaction support
- observable queries
- testability
- lifecycle-independent persistence

### Database ownership

The application owns one authoritative Room database for the core product.

Do not create separate databases for:
- UI cache
- reports
- SMS
- cards
- categories

Additional storage is allowed only for clearly different technical purposes and must not compete with the ledger as financial truth.

---

## 5. Phase 1 database scope

Phase 1 should prove the persistence architecture without prematurely implementing the final financial schema.

A minimal foundation entity may be used to prove:

```text
Create
  ↓
Repository
  ↓
Room
  ↓
Process/App restart
  ↓
Room
  ↓
Repository
  ↓
UI
```

The final financial transaction model belongs to the ledger/domain phase.

This prevents Phase 1 from inventing financial semantics before their dedicated design gate.

---

## 6. Reactive state model

Every screen follows a predictable state model.

Conceptually:

```text
UiState =
    Loading
    Content
    Empty
    Error
    Degraded
    ActionInProgress
```

The concrete Kotlin representation may differ, but the behavior must remain explicit.

### Rules

- Persistent state comes from ViewModel/domain/repository/database flows.
- UI renders state.
- User actions become events/intents.
- Events are handled by the appropriate layer.
- Transient events are not stored as permanent screen state.

Examples of transient events:
- snackbar
- navigation command
- one-time confirmation

Examples of persistent state:
- saved transaction
- selected account
- database-backed setting

---

## 7. One-way data flow

```text
User Action
    ↓
UI Event
    ↓
ViewModel
    ↓
Use Case
    ↓
Repository
    ↓
Room
    ↓
Flow
    ↓
ViewModel State
    ↓
Compose UI
```

Do not create bidirectional UI/database synchronization hacks.

Avoid manual "refresh" flags when observable persistence can provide the state.

---

## 8. Navigation design

Phase 1 implements the navigation foundation, not the complete product.

The architecture must support the eventual primary areas:

```text
Home
Transactions
Accounts
Insights
```

Global actions such as add/review/search/settings must be architecturally supportable without creating navigation chaos.

### Navigation rules

- One navigation owner.
- Routes are explicit and type-safe where practical.
- Route arguments are validated.
- Back navigation is predictable.
- Navigation does not own business state.
- Screen state survives normal recreation where required.
- Deep-link readiness is considered even if deep links are not implemented in Phase 1.
- No screen directly manipulates another screen's state.

---

## 9. UI design system

Material 3 is the foundation.

Create reusable semantic design tokens for:

- typography
- spacing
- shapes
- elevation
- color roles
- component states
- touch targets
- icons

### UX quality target

Spendly should feel:

**soothing + smooth + calm + responsive + trustworthy**

This means:
- minimal visual noise
- predictable hierarchy
- restrained animation
- immediate feedback
- stable layouts
- clear errors
- purposeful empty states
- comfortable one-handed interaction
- accessibility by default

"Crazy UX" means reducing friction and cognitive load, not adding visual effects.

---

## 10. Accessibility baseline

Foundation components must support:

- semantic roles
- content descriptions where needed
- adequate touch targets
- scalable typography
- meaningful traversal order
- usable contrast
- screen-reader compatibility
- keyboard/IME-safe layouts where applicable

Accessibility must be built into reusable components instead of patched screen-by-screen later.

---

## 11. Lifecycle model

Assume Android can:

- recreate activities
- kill the process
- pause/resume the app
- interrupt work
- delay background work

Therefore:

### Durable state
Lives in Room.

### Screen state
Lives in ViewModel/state mechanisms appropriate for recreation.

### Transient events
Must not be treated as durable data.

The architecture must remain correct after recreation and restart.

---

## 12. Background work

Phase 1 establishes the WorkManager boundary only.

Future architecture:

```text
Android Event / Scheduled Work
          ↓
Application Use Case
          ↓
Same Domain Pipeline
          ↓
Repository
          ↓
Room
```

No permanent foreground service is introduced merely to support future SMS processing.

Future SMS ingestion must reuse domain/persistence semantics rather than creating a parallel data path.

---

## 13. Error model

Errors are explicit product states.

Every failure should answer:

1. What failed?
2. Is persisted data safe?
3. Can the user retry?
4. Is retry safe/idempotent?
5. What should the UI communicate?
6. Should the failure be logged?
7. Does it need a regression test?

Never convert failure into false success.

Never silently discard a financial event.

---

## 14. Testing architecture

```text
Pure/domain logic
       ↓
Unit tests

Room/persistence
       ↓
Database/integration tests

ViewModel/UI state
       ↓
Unit/UI tests

Compose interaction
       ↓
Compose UI tests

Android lifecycle
       ↓
Instrumentation tests

Complete user path
       ↓
Real device/emulator E2E
```

Tests should validate behavior, not implementation trivia.

---

## 15. CI contract

CI must reproduce the important local quality gates.

At minimum:

```text
Checkout
  ↓
Build
  ↓
Unit tests
  ↓
Database/integration tests
  ↓
UI/instrumentation checks where environment permits
  ↓
Lint/static analysis
  ↓
Result
```

A CI failure requires root-cause analysis.

Repeatedly rerunning the same failing job is not a fix.

---

## 16. Build configuration

Phase 1 must establish:

- reproducible Gradle build
- debug variant
- structurally valid release variant
- dependency/version management
- SDK policy
- local configuration strategy
- CI configuration

No credentials or signing secrets belong in the repository.

---

## 17. Package/module boundary

Initial structure should preserve clear ownership.

Conceptually:

```text
app
├── presentation
│   ├── navigation
│   ├── screens
│   ├── components
│   └── theme
│
├── domain
│   ├── model
│   ├── repository
│   └── usecase
│
├── data
│   ├── local
│   │   ├── database
│   │   ├── dao
│   │   └── entity
│   └── repository
│
└── core
    ├── common
    └── testing
```

This is a conceptual boundary, not permission to create unnecessary modules.

Start simple; introduce physical Gradle modules only when they provide real isolation/value.

---

## 18. Phase 1 E2E proof

The foundation must demonstrate one complete persistence-driven mobile path.

Example:

```text
Launch App
   ↓
Open foundation screen
   ↓
Perform supported test action
   ↓
ViewModel receives intent
   ↓
Use Case executes
   ↓
Repository persists
   ↓
Room emits updated state
   ↓
UI updates
   ↓
Restart App
   ↓
State still exists
```

This is the minimum vertical proof that our architecture is actually connected.

---

## 19. Quality metrics

Phase 1 will be reviewed against:

### Correctness
- persistence survives restart
- state remains consistent
- no duplicate mutations

### Responsiveness
- no avoidable main-thread work
- no visible blocking on basic interactions
- navigation feels immediate

### Task efficiency
- minimal steps
- no unnecessary confirmations
- no confusing backtracking

### Navigation
- predictable destinations
- correct back behavior
- no dead ends

### Accessibility
- semantic controls
- usable touch targets
- scalable content
- readable hierarchy

### Reliability
- lifecycle-safe behavior
- failure states
- deterministic tests

### Maintainability
- clear boundaries
- limited coupling
- no UI-owned business logic

### Trust
- explicit outcomes
- understandable errors
- no silent data mutation

---

## 20. Explicitly deferred

Phase 1 deliberately does **not** solve:

- SMS detection
- SMS parsing
- provider identification
- transaction normalization
- financial transaction types
- entity resolution
- deduplication
- categories
- merchant rules
- reconciliation
- credit-card statements
- payment matching
- monthly insights
- full production onboarding
- production SMS permission UX

These belong to later phases.

---

## 21. Architecture invariants

The following must remain true as implementation progresses:

1. UI does not own financial truth.
2. Domain logic is testable without Compose.
3. Persistence is behind repository boundaries.
4. Room is the authoritative local persistence foundation.
5. Database state can be observed reactively.
6. Screen recreation does not corrupt durable state.
7. Background work is retry-safe.
8. Future ingestion and reconciliation converge on the same domain pipeline.
9. Reports do not become an independent source of truth.
10. Phase scope is controlled.

---

## 22. Design approval checklist

- [x] Product intent understood.
- [x] Architecture direction defined.
- [x] Local-first persistence decision defined.
- [x] UI state model defined.
- [x] Navigation principles defined.
- [x] Design-system direction defined.
- [x] Lifecycle strategy defined.
- [x] WorkManager boundary defined.
- [x] Testing strategy defined.
- [x] CI quality contract defined.
- [x] Phase 1 non-goals defined.
- [x] Quality metrics defined.
- [x] Future-phase boundaries protected.

## Design gate

Implementation may begin only after this design record is accepted and #33 is explicitly closed.

**Next issue after approval: #34 — Phase 1B Implementation.**
