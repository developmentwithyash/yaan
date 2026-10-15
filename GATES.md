# YAAN Mandatory Release Gates Register (B1–B13)

> **Authority**: Autonomous Software Engineering & QA System  
> **Evaluation Mode**: Controlled, Measurable, Evidence-Driven Improvement Loop  
> **Release Target**: Invite-Only Closed Beta (India 18+, English Default)  
> **Release Policy**: No open P0 or P1 findings; missing evidence = OPEN.  
> **Last Updated**: 2026-10-15  

---

## 1. Release Gates Status Table (B1–B13)

| Gate | Description | Current State | Evidence & Verification Path | Blocking Findings |
|:---:|---|:---:|---|:---:|
| **B1** | Clean build, full suite green with real counts (unit vs instrumented) | 🟢 **PASSED** | 97 unique unit tests passing (116 executions across debug/release), 0 failures, 0 skipped, 0 instrumented; `./gradlew testDebugUnitTest :backend:test` | `F-019` (Reconciled) |
| **B2** | Every critical journey passes automatically and on a device | 🟡 **OPEN** | Journeys 1–8 implemented & passing unit contracts; requires physical device automation run | Needs physical device suite |
| **B3** | No open P0 or P1 findings | 🔴 **OPEN** | Open P0: `F-014` (Cloud egress vs zero-knowledge claim), `F-016` (Google Play `USE_EXACT_ALARM` risk). Open P1: `F-013`, `F-015`, `F-017` | `F-013`, `F-014`, `F-015`, `F-016`, `F-017` |
| **B4** | Action safety: every transition and illegal transition tested; destructive tools require confirmation; idempotency under replay and concurrency proven | 🟡 **OPEN** | ActionProposalCard & TwoPhaseActionCommitter live; full Action State Machine transitions & tool registry tests being built | `F-015` |
| **B5** | Time engine suite green including DST/ambiguity cases | 🟡 **OPEN** | Basic regex schedule extraction in `YaanAssistantEngine`; shared KMP time/recurrence engine with DST/ambiguity tests being built | Work in progress |
| **B6** | AI eval meets PROVISIONAL thresholds per category with sample sizes reported; injection suite green; hallucinated-success cases zero | 🟡 **OPEN** | 310-sample eval in `EVAL_REPORT.md` (lexical baseline); requires structured planner eval & hallucinated-success regression set | Work in progress |
| **B7** | Security: threat model written, secrets scan clean, dependency scan reviewed, cross-user isolation tests pass, data-flow matches privacy text, no plaintext prompt logging | 🟢 **PASSED** | `THREAT_MODEL.md`, `SECURITY_AND_PRIVACY.md`, `LlmGatewaySecurityTest` (6 tests pass), `SecurityAuthTest` (6 tests pass) | `F-014` (Copy update) |
| **B8** | Reminder reliability tested per platform with documented limits and visible degraded states | 🟡 **OPEN** | Android AlarmManager + BootReceiver live; `F-016` (exact alarm policy) and `F-017` (iOS 64-request limit) in-progress | `F-016`, `F-017` |
| **B9** | Degraded-mode: with AI/provider down, the offline function list works | 🟢 **PASSED** | 100% offline local SQLite CRUD (notes, tasks, reminders, search) verified in `SqlDelightRepositoryTest` & `YaanAssistantEngineTest` | None |
| **B10** | Cost and performance measured with scenarios and caps enforced | 🟢 **PASSED** | `COST_MODEL.md` (Groq live pricing $0.59/$0.79 per 1M), daily cap 25k tok/user, rate limit 20 req/min in `DefaultLlmGateway` | None |
| **B11** | Operability: runbooks, rollback/kill switch for cloud AI, observability, backups of server ops data tested | 🟢 **PASSED** | Cloud kill switch in `CloudConsentManager`, `CircuitBreaker` in inference mesh, `RELEASE_READINESS.md` runbook | None |
| **B12** | Claims and compliance: every claim traced; Play/App Store checklist for what is addressable; open legal items listed with owners | 🟡 **OPEN** | `CLAIMS_REGISTER.md` active; `F-014` (reconciling 'zero-knowledge' claims) in-progress; `FOUNDER_ACTIONS.md` updated | `F-014` |
| **B13** | Final independent verification pass | 🟡 **OPEN** | Runs at the release-candidate commit after B1–B12 pass or are blocked only by documented human items | Pending release candidate |

---

## 2. Gate Decision Summary
- **Current Verdict**: **NO-GO (Open P0/P1 Findings Under Active Remediation)**
- **Blocking Items**:
  1. `F-014` [P0]: Replace inaccurate 'zero-knowledge' claims in cloud inference paths with 'Consented Cloud-Assisted Intelligence'.
  2. `F-016` [P0]: Android exact alarm permission policy: declare `SCHEDULE_EXACT_ALARM`, check `canScheduleExactAlarms()`, and fallback to inexact idle.
  3. `F-015` [P1]: Ratify Binding Architecture & implement shared `ToolRegistry` and `ActionStateMachine` in `:core:domain`.
  4. `F-017` [P1]: Implement native iOS local notification scheduling bridge respecting the 64-pending request limit.
- **Immediate Target**: Resolve P0 and P1 findings in sequence to advance gates B3, B4, B5, B8, B12 toward release readiness.
