# Phase 1D — E2E Validation Evidence

## Validation target

Phase 1D validates the Phase 1 Android foundation as a runnable mobile application. This document records the evidence available from the repository and CI.

## Build/environment

- Build variant: `debug`
- Android compile/target SDK: 35
- CI Java: Temurin 17
- CI emulator: Android API 34, `google_apis`, x86_64
- Emulator mode: headless, SwiftShader, animations disabled, snapshots disabled
- CI runner: `ubuntu-latest`
- Validation commit: the Phase 1C merged `main` revision at the start of Phase 1D
- CI instrumentation command: `./gradlew connectedDebugAndroidTest`

## Scenario checklist

| Scenario | Result | Evidence / disposition |
|---|---|---|
| First launch | PASS | Instrumentation smoke/E2E tests launch the real Android application and assert the initial UI. |
| Navigation | PASS | Instrumentation coverage navigates across Home, Transactions, Accounts and Insights and verifies system-back behavior. |
| Persistence | PASS | E2E test performs the supported foundation persistence action, recreates the activity, verifies the persisted state, then returns to Home. |
| Lifecycle | PASS (covered subset) | Activity recreation is exercised by the E2E test; ViewModel initialization is guarded against duplicate initialization. |
| Failure/degraded state | DEFERRED | Phase 1 foundation has no user-facing production failure/retry workflow beyond repository failure propagation tested at unit level. Introducing artificial UI failure machinery here would add scope before the financial domain exists. This is explicitly deferred to the phase where real ledger persistence/error states are implemented. |
| Accessibility basics | SMOKE PASS / MANUAL DEVICE REVIEW PENDING | Compose UI tests use stable semantic/test tags and visible navigation labels. A full TalkBack/manual touch-target/contrast audit requires an interactive device session not available in CI. |
| Performance feel | CI SMOKE PASS / MANUAL REVIEW PENDING | No blocking work or artificial sleeps are used in the tested foundation flow; instrumentation runs headless with animations disabled. Interactive jank/frame-time measurement requires a device session. |

## Automated evidence

The Phase 1C CI validation already demonstrated:

- debug build: PASS
- unit tests: PASS
- lint: PASS
- Android instrumentation: PASS
- emulator boot/runtime: PASS
- all final Phase 1C instrumentation tests passed on the final validated commit

The instrumentation suite covers launch, navigation, persistence/reopen behavior and recreation.

## Known limitations

1. No physical Android device is available to this execution environment, so a physical-device run cannot honestly be claimed.
2. Accessibility manual inspection with TalkBack and visual contrast/touch-target review remains a manual-device activity.
3. Performance feel is not a substitute for benchmark/frame-time profiling; the foundation has no production-scale data workload yet.
4. Failure/degraded-state UI is intentionally deferred until the application has real ledger persistence and user-facing error/recovery flows.

These limitations are non-blocking for the current foundation scope but must be revisited before release and when the corresponding product behavior exists.

## Phase 1D gate

The automated real-Android foundation flow is validated. Remaining manual/device-only items are explicitly recorded rather than represented as completed. Phase 1D should only be marked complete after the project owner accepts these documented limitations for this foundation phase.
