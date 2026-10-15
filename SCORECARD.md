# YAAN System Quality & Release Readiness Scorecard

> **Authority**: Autonomous Software Engineering & QA System  
> **Evaluation Mode**: Controlled, Measurable, Evidence-Driven Improvement Loop  
> **Target Release**: Invite-Only Closed Beta (India 18+, English Default)  
> **Date**: 2026-10-15 (Cycle 06 Baseline)  

---

## 1. Scorecard Dimension Matrix (18 Categories)

| # | Dimension / Category | Baseline (Cycle 00) | Latest Measurement (Cycle 06) | Target Threshold | Open Defects | Evidence Path | Residual Risk | Next Action | Trend |
|---|---|---|---|---|---|---|---|---|:---:|
| **1** | **Functional Correctness** | Basic SQLite CRUD; 82 unit tests pass | 97 unique unit tests pass (116 executions); CRUD & search verified | 100% Critical workflows verified (EXTERNAL) | None open | `testDebugUnitTest` | Low | Maintain regression harness | 🟢 Improving |
| **2** | **Critical-Journey Coverage** | 3/8 journeys functional in UI | 8/8 journeys functional in UI & unit contracts | 8/8 journeys verified end-to-end on device (EXTERNAL) | Device run open | `UiStateContractsTest.kt`, `YouScreen.kt` | Med (needs physical device run) | Automate on hardware | 🟢 Improving |
| **3** | **Time/Recurrence Correctness** | Regex schedule extraction ('tomorrow', '2pm') | Basic parsing in `YaanAssistantEngine`; shared KMP engine in-progress | Exhaustive relative, DST & ambiguity test suite (EXTERNAL) | `F-015` | `YaanAssistantEngineTest.kt` | Med (DST/gap unvalidated) | Implement shared time engine | 🟡 In-Progress |
| **4** | **Action Safety** | Proposals staged without interactive confirmation card | `ActionProposalCard` with Confirm & Undo live; State Machine in-progress | Every transition/illegal transition tested; idempotency proven (EXTERNAL) | `F-015` | `ActionProposalCard.kt`, `ActionSafetyEngine.kt` | Low | Implement full Action State Machine | 🟢 Improving |
| **5** | **AI Quality** | Heuristic prompt routing | 310-sample lexical eval in `EVAL_REPORT.md`; 100% citation rate | >= 90% Intent Accuracy, 0% hallucinated actions (PROVISIONAL) | Cloud planner eval | `EVAL_REPORT.md` | Med | Implement structured planner eval | 🟡 In-Progress |
| **6** | **Prompt-Injection Resistance** | Basic regex disarm | `VaultUntrustedContentGuard` (35 test cases, Markdown exfil, XSS, override disarmed) | 100% resistance across >= 50 test cases (EXTERNAL) | Add 15 test cases | `ActionSafetyTest.kt` | Low | Expand red-team suite to 50+ | 🟢 Improving |
| **7** | **Security** | AuthGuard string check, AES-GCM-256 cipher | AES-GCM-256 at rest, AuthGuard BOLA check, 12 security tests green | OWASP Mobile Top 10 clean, strict cross-user isolation (EXTERNAL) | Server auth heuristic | `SecurityAuthTest.kt`, `LlmGatewaySecurityTest.kt` | Low | Cryptographic auth token check | 🟢 Improving |
| **8** | **Privacy** | ConsentManager active; LLM egress undocumented | `DATA_SAFETY.md` published; `VaultRedactor` PII substitution; cloud egress disclosed | Zero unauthorized egress; accurate disclosures (EXTERNAL) | `F-014` (Copy) | `DATA_SAFETY.md`, `VaultRedactor.kt` | Low | Reconcile copy across UI & /healthz | 🟢 Improving |
| **9** | **Data Integrity** | In-memory DB in tests, local SQLite in app | SQLDelight SQLite with AES-GCM-256 encrypted fields; Vault export live | Zero data loss on kill/upgrade; verified export/import (EXTERNAL) | Migration tests | `SqlDelightRepositoryTest.kt`, `VaultExporterTest.kt` | Low | Add schema migration test | 🟢 Improving |
| **10** | **Reminder Reliability (Platform)** | Alarms not scheduled in AlarmManager | Android `AlarmManager.setExactAndAllowWhileIdle` & `BootCompletedReceiver` live; iOS open | Reliable delivery on Android & iOS with degraded state visibility (EXTERNAL) | `F-016`, `F-017` | `AndroidAlarmScheduler.kt`, `AlarmBroadcastReceiver.kt` | Med | Switch to `SCHEDULE_EXACT_ALARM`; build iOS bridge | 🟡 In-Progress |
| **11** | **Reliability & Recovery** | Crash on unbundled iOS resources | iOS resource bundle fix; crash rate 0% in tests; circuit breaker in mesh | 0 crashes; graceful degradation on provider outage (EXTERNAL) | iOS notification open | `CircuitBreaker.kt`, `Info.plist` | Low | iOS notification recovery | 🟢 Improving |
| **12** | **Performance** | Android APK 19 MB; build time 13s | Android APK 19.7 MB; build time 5–8s; latency ceiling sub-150ms | APK < 25 MB, TTFT < 150ms, Cold start < 1200ms (PROVISIONAL) | Physical cold start | `audit/EVIDENCE/cycle_02_build_evidence.txt` | Low | Measure on physical device | 🟢 Improving |
| **13** | **Cost** | Theoretical estimates | Groq live pricing $0.59/$0.79 per 1M; daily cap 25k tok/user; rate limit 20 req/min | $0.00 provider spend on free tier; < $0.15/user/mo at scale (EXTERNAL) | None open | `COST_MODEL.md`, `DefaultLlmGateway.kt` | Low | Monitor token usage | 🟢 Improving |
| **14** | **Accessibility & UX** | High contrast kinetic tokens; missing warning banner | Degraded permission banner in `PlanScreen`; high contrast tokens (`#785600` on `#FDF9F3` > 4.5:1) | WCAG 2.1 AA compliant; scaled font support (EXTERNAL) | Screen reader test | `PlanScreen.kt`, `YaanThemeTest.kt` | Low | Physical TalkBack/VoiceOver check | 🟢 Improving |
| **15** | **Test Strength** | 82 unit tests, 0 UI tests, 0 migration tests | 97 unique unit tests (116 executions), 0 failures, 0 regressions | Critical workflow coverage + fault-injection checks (EXTERNAL) | 0 instrumented tests | `build/test-results/**/*.xml` | Med | Add UI/integration tests | 🟢 Improving |
| **16** | **Compliance** | Missing age gate for DPDP Act 2023 | 18+ age declaration enforced in onboarding & repository; Data Safety matrix declared | India DPDP Act 2023 & Google Play/App Store policies (EXTERNAL) | `F-016`, `F-018` | `OnboardingScreen.kt`, `DATA_SAFETY.md` | Low | Defer SMS fallback; fix alarm perm | 🟢 Improving |
| **17** | **Operational Readiness** | Local build only; no runbooks | Runbooks in `RELEASE_READINESS.md`; AI kill switch in `CloudConsentManager` | Verified runbooks, rollback switches, server ops persistence (PROVISIONAL) | In-memory server store | `RELEASE_READINESS.md`, `CloudConsentManager.kt` | Med | Add durable server ops store | 🟡 In-Progress |
| **18** | **Claims Honesty** | 12 architectural claims audited; contradictions found | Contradictions recorded in `CLAIMS_REGISTER.md`; 'zero-knowledge' cloud egress in review | Zero unsupported claims in code, docs, or UI copy (EXTERNAL) | `F-014` | `CLAIMS_REGISTER.md`, `ARCHITECTURE.md` | Low | Reconcile copy across repository | 🟢 Improving |

---

## 2. Release Blockers Tracker
- **B3 Release Blocker 1 (`F-014` [P0])**: Inaccurate 'zero-knowledge' claims in cloud inference paths -> Update copy to 'Consented Cloud-Assisted Intelligence'.
- **B3 Release Blocker 2 (`F-016` [P0])**: Android exact alarm permission Play Store rejection risk -> Declare `SCHEDULE_EXACT_ALARM`, check `canScheduleExactAlarms()`, and fallback.
- **B3 Release Blocker 3 (`F-015` [P1])**: Ratify Binding Architecture & build shared `ToolRegistry` and `ActionStateMachine`.
- **B3 Release Blocker 4 (`F-017` [P1])**: Implement iOS local notification scheduling bridge respecting the 64-request limit.
