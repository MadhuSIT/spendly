# Spendly — AGENTS.md

## Purpose

This document is the mandatory contribution contract for Spendly.

Spendly is a **local-first Android personal finance ledger**. Its core responsibility is to turn financial events into a reliable, auditable normalized ledger. Because financial data is correctness-sensitive, contributors must optimize for **correctness, recoverability, testability, privacy, and user trust** before speed or feature count.

This file applies to human contributors, AI agents, automation, and any other contributor operating on the repository.

---

# 1. NON-NEGOTIABLE CONTRIBUTION RULES

1. **Do not skip the phase process.**
2. **Do not start work from a later phase while the current phase is incomplete.**
3. **Do not treat a green CI run as proof that a phase is complete.**
4. **Do not treat a merged PR as proof that a phase is complete.**
5. **Do not silently expand scope.**
6. **Do not implement behavior without understanding the relevant product/architecture specification.**
7. **Do not hide defects to make CI green.**
8. **Do not disable tests, weaken assertions, suppress failures, or increase arbitrary timeouts merely to obtain a passing build.**
9. **Do not introduce a second source of financial truth.**
10. **Do not put financial/business rules inside Compose UI code.**
11. **Do not add a backend dependency to core local-first functionality unless explicitly approved by the product architecture.**
12. **Do not use a permanent foreground service for functionality that can be handled through Android events and WorkManager.**
13. **Do not merge unvalidated financial behavior.**
14. **Every discovered defect must either be fixed or explicitly tracked with a documented disposition.**
15. **Every correctness-sensitive bug that can recur should gain regression coverage.**
16. **If uncertain, stop and inspect the repository/spec/issues rather than guessing.**

---

# 2. SOURCE OF TRUTH HIERARCHY

When information conflicts, use this order:

1. Current approved product requirements and acceptance criteria
2. `docs/PRODUCT_SPEC.md`
3. `docs/ARCHITECTURE.md`
4. `docs/UI_UX_SPEC.md`
5. `docs/UI_UX_FLOWS.md`
6. Relevant GitHub phase/child issue acceptance criteria
7. Existing implementation
8. Contributor assumption

Existing code is **not automatically correct** merely because it already exists.

If implementation conflicts with an approved requirement, do not silently preserve the implementation. Raise the discrepancy, fix it when in scope, or record an explicit decision.

---

# 3. PHASE-GATED DELIVERY

The master delivery contract is GitHub issue **#3**.

Phase 1 is GitHub issue **#4**.

The mandatory phase lifecycle is:

```text
DESIGN & SCOPE
      ↓
IMPLEMENT
      ↓
AUTOMATED TEST
      ↓
END-TO-END VALIDATION
      ↓
UX / QUALITY METRICS
      ↓
GAP ANALYSIS
      ↓
IMPROVE + REGRESSION TEST
      ↓
FINAL ACCEPTANCE
      ↓
PHASE SIGN-OFF
      ↓
ONLY THEN → NEXT PHASE
```

For Phase 1 the child issues are:

- #33 — Design & Scope
- #34 — Implementation
- #35 — Automated Testing
- #36 — End-to-End Validation
- #37 — UX / Quality Metrics Review
- #38 — Gap Analysis & Improvement
- #39 — Final Acceptance & Sign-off

**Do not begin Phase 2 implementation until #39 has been explicitly completed and Phase 1 has been signed off.**

A contributor may discover or document future-phase requirements while working on the current phase, but must not implement them unless they are required to complete the current phase.

---

# 4. ONE ISSUE = ONE CONTROLLED UNIT OF WORK

Before changing code:

1. Identify the exact GitHub issue.
2. Read its complete description and acceptance criteria.
3. Read the parent phase issue.
4. Read relevant product/architecture/UX documentation.
5. Inspect the current repository state.
6. Identify existing tests and CI checks.
7. Define what will change and what will explicitly not change.

If the work does not map to an existing issue, create or update the appropriate issue before implementation.

Do not use a vague issue such as "improve app" as justification for unrelated changes.

---

# 5. CONTRIBUTION WORKFLOW

Every implementation contribution should follow:

```text
Understand
   ↓
Inspect
   ↓
Design
   ↓
Implement
   ↓
Test
   ↓
Review
   ↓
Measure
   ↓
Fix
   ↓
Validate
   ↓
PR
   ↓
CI
   ↓
Merge
```

For significant changes, the PR description must explain this chain.

---

# 6. BRANCHING AND COMMITS

## Branches

Use a dedicated branch for meaningful work.

Preferred patterns:

- `feat/<short-description>`
- `fix/<short-description>`
- `test/<short-description>`
- `docs/<short-description>`
- `chore/<short-description>`

Do not develop production work directly on `main`.

## Commits

Keep commits:
- focused
- understandable
- logically grouped
- independently reviewable where practical

Prefer messages such as:

- `feat: add local persistence foundation`
- `test: cover database restart persistence`
- `fix: preserve navigation state on recreation`
- `docs: document Android architecture`

Do not create meaningless commits such as `update`, `changes`, or `fix stuff`.

---

# 7. PULL REQUEST RULES

Every non-trivial change goes through a PR.

A PR must include:

## What changed
Concise description of implementation.

## Why
Reference the issue and product/architecture reason.

## Scope
Explicitly state what is included and excluded.

## Validation
List:
- unit tests
- integration/database tests
- UI tests
- instrumentation tests
- E2E validation
- lint/static analysis
- manual validation

## UX impact
For user-facing changes, describe:
- interaction flow
- loading/empty/error states
- accessibility
- responsiveness
- navigation/back behavior
- visual consistency

## Risks
Document:
- migration risk
- lifecycle risk
- data-loss risk
- concurrency risk
- performance risk
- compatibility risk

## Known limitations
Do not hide known limitations.

## Screenshots/video
For meaningful UI changes, provide evidence where useful.

---

# 8. FINANCIAL CORRECTNESS RULES

The normalized ledger is the source of truth.

```text
Raw SMS / Manual Input
          ↓
      Domain Logic
          ↓
   Normalized Ledger
          ↓
 Derived Reports / UI
```

Never make a report, screen, cache, or UI calculation an independent financial source of truth.

Critical invariants:

1. Own-account transfers must not increase expense totals.
2. Credit-card payments must not create another expense.
3. Reprocessing the same financial event must not create another financial impact.
4. Refunds must not increase net spending.
5. Failed transactions must not become expenses.
6. Reversals must neutralize/link to the original transaction appropriately.
7. Real-time ingestion and reconciliation must converge to the same ledger result.
8. User category corrections must persist.
9. Derived reports must be rebuildable from the ledger.
10. Monthly reporting must not invent missing history.

Any code affecting these invariants requires explicit tests.

---

# 9. DATA AND DATABASE RULES

Room/SQLite is the local persistence foundation.

Rules:

- Define schema deliberately.
- Version schema changes.
- Provide migration coverage.
- Never casually delete or rename persisted data.
- Do not bypass repositories from UI.
- Do not perform blocking database operations on the main thread.
- Treat migrations as production code.
- Test persistence across app/database recreation.
- Preserve auditability for correctness-sensitive mutations.
- Prefer deterministic transformations.
- Do not introduce a second local database for the same source of truth.

Before changing a schema, ask:

1. What existing data does this affect?
2. Is migration required?
3. How is migration tested?
4. What happens on upgrade failure?
5. Can old app data still be read safely?
6. Does the change affect financial invariants?

---

# 10. UI / UX RULES

Spendly is a mobile application. The target is:

**soothing, smooth, calm, fast, effortless, trustworthy.**

"Crazy UX" means thoughtful interaction quality, not visual complexity.

Every user-facing flow should consider:

- one-thumb usability
- minimal typing
- progressive disclosure
- sensible defaults
- immediate feedback
- stable layouts
- predictable navigation
- correct back behavior
- useful empty states
- useful error states
- graceful degraded states
- accessibility
- readable financial information
- clear confirmation of important actions
- easy recovery from mistakes

Avoid:
- unnecessary modals
- excessive animations
- decorative complexity
- dense screens without hierarchy
- arbitrary loading spinners
- unexplained errors
- silent data mutations
- duplicated formatting/styling logic
- giant composables

Use the design system rather than inventing per-screen styling.

---

# 11. PERFORMANCE RULES

Performance is part of correctness for a mobile UX.

Never knowingly:
- block the main thread with database/network/file work
- perform expensive computation during composition
- introduce unnecessary recomposition
- poll aggressively when event-driven work is sufficient
- add sleeps to hide race conditions
- use arbitrary timeouts to hide slow operations
- introduce a permanent foreground service without architectural justification

Measure before optimizing where practical, but do not knowingly introduce avoidable performance problems.

---

# 12. TESTING RULES

Tests must prove behavior, not merely increase coverage numbers.

Use the appropriate level:

```text
Pure logic        → Unit test
Persistence       → Room/database test
UI state          → ViewModel/UI test
User interaction  → Compose/UI test
Android lifecycle → Instrumentation test
Complete flow     → E2E/device validation
```

Rules:

- No knowingly flaky tests.
- No disabled tests without documented reason.
- No weak assertions just to pass.
- No arbitrary sleeps for synchronization.
- No mocking the entire system when an integration test is required.
- Every production bug that can recur should have regression coverage when practical.
- Tests must be deterministic and repeatable.

---

# 13. ERROR HANDLING

Errors are product states, not just logs.

For every failure path, determine:

1. What failed?
2. Is data safe?
3. What does the user see?
4. Can the operation be retried?
5. Is retry idempotent?
6. Is the failure observable?
7. Does it require telemetry/logging?
8. Does it require a regression test?

Never display success when the underlying operation failed.

Never silently discard financial events.

---

# 14. ANDROID LIFECYCLE

Assume Android can:
- recreate activities
- kill processes
- pause/resume apps
- revoke permissions
- interrupt background work
- delay scheduled work
- deliver events more than once

Design accordingly.

Persist durable state. Keep transient UI state separate.

Any background operation must be safe against:
- retry
- duplication
- interruption
- delayed execution
- process death

---

# 15. SMS / INGESTION FUTURE RULES

When Phase 3 begins, SMS is an input source—not the ledger.

Expected conceptual pipeline:

```text
SMS_RECEIVED
    ↓
Detect financial SMS
    ↓
Classify
    ↓
Identify provider
    ↓
Parse
    ↓
Normalize
    ↓
Validate
    ↓
Resolve entity
    ↓
Deduplicate
    ↓
Persist ledger transaction
    ↓
Categorize
    ↓
Notify
```

Real-time ingestion and reconciliation must use the same parsing/validation/deduplication semantics.

Never write parser output directly into arbitrary UI state.

Never assume an SMS is unique merely because it arrived once.

---

# 16. SECURITY AND PRIVACY

Spendly handles sensitive financial information.

Contributors must:
- minimize stored sensitive data
- avoid logging raw financial SMS unnecessarily
- avoid logging secrets
- avoid committing credentials/tokens/keys
- use secure configuration practices
- treat SMS/database data as sensitive
- document permission requirements
- avoid unnecessary analytics/data collection

Never commit:
- API keys
- signing credentials
- passwords
- tokens
- private certificates
- real financial SMS samples containing identifying information

Use sanitized fixtures.

---

# 17. OBSERVABILITY

Logs must help diagnose problems without leaking sensitive financial data.

Prefer:
- event type
- parser/provider identifier
- operation result
- safe identifiers
- error class
- timing
- correlation/reference identifiers where safe

Avoid raw SMS contents and unnecessary account/card details.

---

# 18. DOCUMENTATION RULE

If behavior, architecture, workflow, schema, or developer setup changes, update the relevant documentation in the same contribution when appropriate.

Documentation must describe reality.

Do not leave instructions that no longer work.

---

# 19. DEFINITION OF DONE

A task is not done because code exists.

A meaningful implementation is done only when:

- [ ] Scope matches the issue.
- [ ] Design was considered.
- [ ] Implementation is complete.
- [ ] Relevant tests exist.
- [ ] Tests pass.
- [ ] Static analysis/lint passes where applicable.
- [ ] E2E behavior is validated when applicable.
- [ ] UX is reviewed when user-facing.
- [ ] Accessibility is considered when user-facing.
- [ ] Failure paths are considered.
- [ ] Documentation is updated where needed.
- [ ] No known critical defect is hidden.
- [ ] PR is reviewable.
- [ ] CI passes.
- [ ] Reviewer feedback is resolved.
- [ ] Remaining limitations are explicitly documented.

---

# 20. PHASE SIGN-OFF STANDARD

A phase can be marked complete only when:

```text
Design approved
      AND
Implementation complete
      AND
Automated tests green
      AND
E2E flow demonstrated
      AND
UX / quality metrics reviewed
      AND
Gaps analyzed
      AND
Required improvements completed
      AND
No blocking defects remain
      AND
Evidence recorded
      AND
Explicit sign-off posted
```

If any condition is false, the phase remains open.

**Do not advance phases because of schedule pressure.**

---

# 21. AI AGENT-SPECIFIC RULES

AI agents must behave as engineering contributors, not code generators.

Before changing code:
- inspect the relevant repository state
- read the issue
- read relevant documentation
- understand existing conventions
- identify tests
- identify current CI expectations

While working:
- make small, traceable changes
- verify assumptions
- do not fabricate files, tests, commands, or results
- do not claim a tool/action succeeded unless it actually did
- do not claim CI passed without checking CI
- do not claim a phase is complete without the phase gate
- do not silently broaden scope

After working:
- run relevant validation
- inspect failures rather than repeatedly rerunning blindly
- perform root-cause analysis
- fix the root cause
- add regression coverage where appropriate
- revalidate
- report exact results

If a tool fails:
- record the actual failure
- determine whether it is an environment/tool limitation or product defect
- do not pretend the operation completed

If requirements are ambiguous:
- prefer the documented product/architecture contract
- if still ambiguous, create a tracked clarification rather than inventing behavior

---

# 22. NO "JUST MAKE CI GREEN" CULTURE

The following are explicitly prohibited:

- deleting tests because they fail
- weakening assertions because implementation is wrong
- skipping E2E because unit tests pass
- suppressing lint without justification
- increasing timeouts to hide performance problems
- mocking away a real integration problem
- changing expected behavior solely to match a broken implementation
- merging known P0/P1 defects
- marking an issue complete because the PR merged
- moving unfinished work into a later phase without documenting why

A green pipeline is a **signal**, not the definition of product quality.

---

# 23. CHANGE DISCIPLINE

For every meaningful change ask:

### Correctness
Does this preserve product invariants?

### Architecture
Does this preserve dependency boundaries?

### Data
Could this lose, duplicate, corrupt, or misrepresent persisted information?

### UX
Does this make the user interaction clearer, faster, calmer, or safer?

### Lifecycle
What happens after recreation, process death, retry, or interruption?

### Testing
How will a regression be detected?

### Scope
Is this required for the current issue/phase?

If the answer is unclear, investigate before merging.

---

# 24. FINAL PRINCIPLE

Spendly should be built as if every financial record will eventually be relied upon by a user making a real financial decision.

Therefore:

**Correctness over speed.  
Evidence over assumption.  
Simple architecture over clever architecture.  
User trust over visual polish.  
Measured UX over subjective claims.  
Regression protection over repeated manual fixes.  
Phase completion over feature accumulation.**

The goal is not to produce the most code.

The goal is to produce a product that remains correct, understandable, testable, smooth, and trustworthy as it grows.
