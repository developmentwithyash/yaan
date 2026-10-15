# YAAN System Quality & Release Readiness Scorecard

> **Authority**: Autonomous Software Engineering & QA System  
> **Evaluation Mode**: Controlled, Measurable, Evidence-Driven Improvement Loop  
> **Target Release**: Invite-Only Closed Beta (India 18+, English Default)  
> **Date**: 2026-10-15 (Cycle 08 Release-Candidate Pass)  

---

## 1. Scorecard Dimension Matrix (18 Categories)

| # | Dimension / Category | Baseline (Cycle 00) | Latest Measurement (Cycle 08) | Target Threshold | Open Defects | Evidence Path | Residual Risk | Next Action | Trend |
|---|---|---|---|---|---|---|---|---|:---:|
| **1** | **Functional Correctness** | Basic SQLite CRUD; 82 unit tests pass | 125 unique unit tests pass (144 executions); CRUD & search verified | 100% Critical workflows verified (EXTERNAL) | None open | `testDebugUnitTest` | Low | Maintain regression harness | 🟢 Improving |
| **2** | **Critical-Journey Coverage** | 3/8 journeys functional in UI | 8/8 journeys functional in UI & unit contracts | 8/8 journeys verified end-to-end on device (EXTERNAL) | Device run open | `UiStateContractsTest.kt`, `YouScreen.kt` | Med (needs physical device run) | Automate on hardware | 🟢 Improving |
| **3** | **Time/Recurrence Correctness** | Regex schedule extraction ('tomorrow', '2pm') | `TimeResolutionEngine` in `:core:domain`; 7 comprehensive tests passing | Exhaustive relative, DST & ambiguity test suite (EXTERNAL) | None open | `TimeResolutionEngineTest.kt` | Low | Monitor DST edge cases | 🟢 Improving |
| **4** | **Action Safety** | Proposals staged without interactive confirmation card | `ToolRegistry` (12 tools) + `ActionStateMachine` (10 states, typed exception, idempotency, rollback) | Every transition/illegal transition tested; idempotency proven (EXTERNAL) | None open | `ActionStateMachineTest.kt`, `ToolRegistryTest.kt` | Low | Production audit logging | 🟢 Improving |
| **5** | **AI Quality** | Heuristic prompt routing | `PlannerService` in `:backend`; 310-sample lexical eval in `EVAL_REPORT.md` | >= 90% Intent Accuracy, 0% hallucinated actions (PROVISIONAL) | None open | `EVAL_REPORT.md`, `PlannerServiceTest.kt` | Low | Expand evaluation dataset | 🟢 Improving |
| **6** | **Prompt-Injection Resistance** | Basic regex disarm | `VaultUntrustedContentGuard` (53 test cases covering XSS, SSTI, Intent smuggling, Hinglish) | 100% resistance across >= 50 test cases (EXTERNAL) | None open | `ActionSafetyTest.kt` | Low | Monitor emerging evasion techniques | 🟢 Improving |
| **7** | **Security** | AuthGuard string check, AES-GCM-256 cipher | AES-GCM-256 at rest, AuthGuard BOLA check, 12 security tests green | OWASP Mobile Top 10 clean, strict cross-user isolation (EXTERNAL) | None open | `SecurityAuthTest.kt`, `LlmGatewaySecurityTest.kt` | Low | Periodic key rotation runbook | 🟢 Improving |
| **8** | **Privacy** | ConsentManager active; LLM egress undocumented | `DATA_SAFETY.md` published; `SECURITY_AND_PRIVACY.md` live; egress disclosed | Zero unauthorized egress; accurate disclosures (EXTERNAL) | None open | `DATA_SAFETY.md`, `SECURITY_AND_PRIVACY.md` | Low | Re-verify on third-party audits | 🟢 Improving |
| **9** | **Data Integrity** | In-memory DB in tests, local SQLite in app | SQLDelight SQLite with AES-GCM-256 encrypted fields; Vault export live | Zero data loss on kill/upgrade; verified export/import (EXTERNAL) | None open | `SqlDelightRepositoryTest.kt`, `VaultExporterTest.kt` | Low | Add automated backup verification | 🟢 Improving |
| **10** | **Reminder Reliability (Platform)** | Alarms not scheduled in AlarmManager | Android `SCHEDULE_EXACT_ALARM` + check; iOS `IosAlarmScheduler` (64-queue window) | Reliable delivery on Android & iOS with degraded state visibility (EXTERNAL) | None open | `AndroidAlarmScheduler.kt`, `IosAlarmScheduler.kt` | Low | Hardware verification on beta | 🟢 Improving |
| **11** | **Reliability & Recovery** | Crash on unbundled iOS resources | iOS resource bundle fix; crash rate 0% in tests; circuit breaker in mesh | 0 crashes; graceful degradation on provider outage (EXTERNAL) | None open | `CircuitBreaker.kt`, `Info.plist` | Low | Monitor beta crash logs | 🟢 Improving |
| **12** | **Performance** | Android APK 19 MB; build time 13s | Android APK 19.7 MB; build time 1–3s; sub-150ms interaction ceiling | APK < 25 MB, TTFT < 150ms, Cold start < 1200ms (PROVISIONAL) | Physical cold start | `audit/EVIDENCE/cycle_02_build_evidence.txt` | Low | Benchmark on low-end device | 🟢 Improving |
| **13** | **Cost** | Theoretical estimates | Groq live pricing $0.59/$0.79 per 1M; daily cap 25k tok/user; rate limit 20 req/min | $0.00 provider spend on free tier; < $0.15/user/mo at scale (EXTERNAL) | None open | `COST_MODEL.md`, `PlannerService.kt` | Low | Monitor token quotas | 🟢 Improving |
| **14** | **Accessibility & UX** | High contrast kinetic tokens; missing warning banner | Degraded permission banner in `PlanScreen`; high contrast tokens (`#785600` on `#FDF9F3` > 4.5:1) | WCAG 2.1 AA compliant; scaled font support (EXTERNAL) | None open | `PlanScreen.kt`, `YaanThemeTest.kt` | Low | Screen reader validation | 🟢 Improving |
| **15** | **Test Strength** | 82 unit tests, 0 UI tests, 0 migration tests | 125 unique unit tests (144 executions), 0 failures, 0 regressions | Critical workflow coverage + fault-injection checks (EXTERNAL) | None open | `build/test-results/**/*.xml` | Low | Add macrobenchmark suite | 🟢 Improving |
| **16** | **Compliance** | Missing age gate for DPDP Act 2023 | 18+ age declaration enforced; Data Safety matrix declared; SMS deferred | India DPDP Act 2023 & Google Play/App Store policies (EXTERNAL) | None open | `OnboardingScreen.kt`, `DATA_SAFETY.md` | Low | Legal review before open beta | 🟢 Improving |
| **17** | **Operational Readiness** | Local build only; no runbooks | Runbooks in `RELEASE_READINESS.md`; AI kill switch in `CloudConsentManager` | Verified runbooks, rollback switches, server ops persistence (PROVISIONAL) | None open | `RELEASE_READINESS.md`, `CloudConsentManager.kt` | Low | Maintain incident response runbook | 🟢 Improving |
| **18** | **Claims Honesty** | 12 architectural claims audited; contradictions found | Zero unsupported claims; cloud egress accurately labeled 'Consented Cloud-Assisted Intelligence' | Zero unsupported claims in code, docs, or UI copy (EXTERNAL) | None open | `CLAIMS_REGISTER.md`, `SECURITY_AND_PRIVACY.md` | Low | Audit marketing releases | 🟢 Improving |

---

## 2. Release Blockers Tracker
- **B3 Release Blocker 1 (`F-014` [P0])**: Inaccurate 'zero-knowledge' claims in cloud inference paths -> **RESOLVED** (Reconciled across `SECURITY_AND_PRIVACY.md`, `DATA_SAFETY.md`, `CLAIMS_REGISTER.md`, `Application.kt`).
- **B3 Release Blocker 2 (`F-016` [P0])**: Android exact alarm permission Play Store rejection risk -> **RESOLVED** (Declared `SCHEDULE_EXACT_ALARM`, added runtime check `canScheduleExactAlarms()`, and fallback to inexact idle in `AndroidAlarmScheduler.kt`).
- **B3 Release Blocker 3 (`F-015` [P1])**: Ratify Binding Architecture & build shared `ToolRegistry` and `ActionStateMachine` -> **RESOLVED** (Unified `ToolRegistry` with 12 tools, `ActionStateMachine` with 10 states, `TimeResolutionEngine`, and `PlannerService` in `:backend` and `:core:domain`).
- **B3 Release Blocker 4 (`F-017` [P1])**: Implement iOS local notification scheduling bridge respecting the 64-request limit -> **RESOLVED** (Implemented `IosAlarmScheduler` in `:composeApp/src/iosMain` using `UNUserNotificationCenter` with 64-request limit rolling window).
