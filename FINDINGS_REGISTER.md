# YAAN Findings Register

> **Authority**: Autonomous Software Engineering & QA System  
> **Status**: CANONICAL REGISTER  
> **Last Updated**: 2026-10-15 (Cycle 00)  

---

## 1. Findings Summary Table

| ID | Title | Loop / Category | Priority | Severity (1-5) | Likelihood (1-5) | Evidence Class | Status | Attempts | Linked Commits / Tests |
|---|---|---|:---:|:---:|:---:|:---:|:---:|:---:|---|
| **`F-001`** | Android Lint failure on `SCHEDULE_EXACT_ALARM` & deprecated `allowBackup` | G/B (Code Quality/Platform) | **P1** | 4 | 5 | VERIFIED | **DONE** | 1 | `audit/EVIDENCE/cycle_01_lint_clean.txt` |
| **`F-002`** | Missing Android AlarmManager Bridge, Notification Channel & Boot Reschedule Receiver | A/F (Reliability/Correctness) | **P0** | 5 | 5 | VERIFIED | **DONE** | 1 | `audit/EVIDENCE/cycle_02_build_evidence.txt`, `SqlDelightReminderSchedulerTest` |
| **`F-003`** | Missing Notification & Microphone permission request flow & degraded state UI | A/E (UX/Correctness) | **P1** | 4 | 4 | VERIFIED | **DONE** | 1 | `PlanScreen.kt`, `OnboardingScreen.kt`, `UiStateContractsTest` |
| **`F-004`** | Action proposals lack affirmative confirmation and execution UI in screens | A/C (Security/Correctness) | **P1** | 4 | 4 | VERIFIED | **DONE** | 1 | `ActionProposalCard.kt`, `AskYaanScreen.kt`, `HomeScreen.kt`, `UiStateContractsTest` |
| **`F-005`** | Vault export in `YouScreen` is an empty stub with no serialized archive output | A/F (Data/Correctness) | **P2** | 3 | 3 | VERIFIED | **DONE** | 1 | `VaultExporterTest.kt`, `YouScreen.kt`, `cycle_05_test_run.txt` |
| **`F-006`** | Missing age eligibility policy and declaration for India DPDP Act 2023 / COPPA | G/C (Legal/Compliance) | **P0** | 5 | 3 | VERIFIED | **DONE** | 1 | `UserSessionRepositoryTest`, `OnboardingScreen.kt` |
| **`F-007`** | Data safety and privacy disclosures lack explicit documentation of Groq LLM egress | G/C (Legal/Compliance) | **P1** | 4 | 4 | VERIFIED | **DONE** | 1 | `audit/DATA_SAFETY.md`, `CLAIMS_REGISTER.md`, `cycle_05_test_run.txt` |
| **`F-008`** | iOS launch crash due to Compose ProMotion frame pacing and missing resources | A/F (Reliability/Correctness) | **P0** | 5 | 5 | VERIFIED | **DONE** | 1 | `Info.plist`, `iosApp.xcodeproj` |
| **`F-009`** | Suggestion chips require manual clipboard paste; voice mode lacks UK accent & loading beacon | E (UX/Delight) | **P2** | 3 | 4 | VERIFIED | **DONE** | 1 | `ExecutePrompt`, `PlatformVoiceSpeaker`, `YaanTopBar.kt` |
| **`F-010`** | Assistant engine missing automatic natural language note/reminder intent detection | A (Correctness) | **P1** | 4 | 5 | VERIFIED | **DONE** | 1 | `YaanAssistantEngine.kt`, `YaanAssistantEngineTest.kt` |
| **`F-011`** | Action proposals not synchronized across turns and iOS voice playback muted by switch | A/E (Correctness/UX) | **P1** | 4 | 5 | VERIFIED | **DONE** | 1 | `AskYaanScreen.kt`, `LiveVoiceModal.kt`, `PlatformVoiceSpeaker.ios.kt` |
| **`F-012`** | Notes screen shows static placeholders; multi-turn chat lost across tabs; regex Native crash | A/B (Correctness/Architecture)| **P1** | 4 | 5 | VERIFIED | **DONE** | 1 | `NotesScreen.kt`, `App.kt`, `ActionSafetyEngine.kt`, `VaultRedactor.kt` |
| **`F-013`** | Progress tracker claims 100% maturity while backend components are mock/in-memory and items unchecked | B/A (Architecture/Governance) | **P1** | 4 | 5 | VERIFIED | **IN-PROGRESS** | 1 | `PROGRESS_TRACKER.md` |
| **`F-014`** | Marketing & docs claim 'zero-knowledge' for cloud-assisted LLM inference paths | C/G (Privacy/Compliance) | **P0** | 5 | 5 | VERIFIED | **IN-PROGRESS** | 1 | `CLAIMS_REGISTER.md`, `DATA_SAFETY.md`, `ARCHITECTURE.md` |
| **`F-015`** | Architectural ambiguity: backend executing tools vs on-device encrypted vault authority | B/C (Architecture/Security) | **P1** | 4 | 5 | VERIFIED | **IN-PROGRESS** | 1 | `ADR-0007`, `core:domain` ToolRegistry |
| **`F-016`** | Android `USE_EXACT_ALARM` violates Google Play policy for reminder/task apps | G/F (Platform/Reliability) | **P0** | 5 | 5 | VERIFIED | **IN-PROGRESS** | 1 | `AndroidManifest.xml`, `AndroidAlarmScheduler.kt` |
| **`F-017`** | Missing iOS local notification scheduling bridge and 64-pending request limit handling | F/A (Reliability/Correctness) | **P1** | 4 | 5 | VERIFIED | **IN-PROGRESS** | 1 | `composeApp/src/iosMain` |
| **`F-018`** | SMS fallback requires plaintext transmission and TRAI DLT registration (TCCCPR 2018) | G/C (Legal/Privacy) | **P2** | 3 | 4 | VERIFIED | **DEFERRED** | 1 | `FOUNDER_ACTIONS.md`, `SPECS_DEFERRED.md` |
| **`F-019`** | Test inventory count drift between tracker entries (82 vs measured 97 unique / 116 executions) | B/H (Quality/Regression) | **P2** | 3 | 5 | VERIFIED | **IN-PROGRESS** | 1 | `PROGRESS_TRACKER.md`, `SCORECARD.md` |

---

## 2. Detailed Finding Profiles

### Finding `F-001`
- **Title**: Android Lint failure on `SCHEDULE_EXACT_ALARM` & deprecated `allowBackup`.
- **Category**: Loop G / Loop B (Platform Policy & Static Analysis).
- **Priority**: P1 (Blocks clean CI/lint release gate G1).
- **Severity**: 4 | **Likelihood**: 5 | **Evidence Class**: VERIFIED.
- **Root Cause**: In Android API 33/34+ (Android 13/14), `SCHEDULE_EXACT_ALARM` is marked as a restricted permission for non-system apps in AGP lint. Alarm/reminder apps must declare `android.permission.USE_EXACT_ALARM`. Furthermore, `allowBackup="false"` in API 31+ requires an XML resource for `android:dataExtractionRules`.
- **Acceptance Criteria**:
  1. Replace `SCHEDULE_EXACT_ALARM` with `USE_EXACT_ALARM` in `composeApp/src/androidMain/AndroidManifest.xml`.
  2. Create `composeApp/src/androidMain/res/xml/data_extraction_rules.xml` and `backup_rules.xml` prohibiting all cloud and device-to-device backup of encrypted vault files.
  3. `./gradlew :composeApp:lintDebug` passes with 0 errors.
- **Evidence Paths**: `audit/EVIDENCE/cycle_00_lint_report.txt`.

---

### Finding `F-002`
- **Title**: Missing Android AlarmManager Bridge, Notification Channel & Boot Reschedule Receiver.
- **Category**: Loop A / Loop F (Functional Correctness & Reliability).
- **Priority**: P0 (Blocks Critical User Journeys 3 & 6: Create reminder -> receive notification -> reboot device -> reminder persists).
- **Severity**: 5 | **Likelihood**: 5 | **Evidence Class**: VERIFIED.
- **Root Cause**: The repository defines `ReminderRepository` and stores reminders in SQLite via SQLDelight, but there is no native Android bridge invoking `AlarmManager.setExactAndAllowWhileIdle()`, no `BroadcastReceiver` listening to alarms or `ACTION_BOOT_COMPLETED`, and no `NotificationManager` notification channel created. On Android, reminders currently sit in SQLite and never trigger an OS notification or survive reboot.
- **Acceptance Criteria**:
  1. Implement `AndroidAlarmScheduler` that schedules exact PendingIntents via `AlarmManager` with wake-lock/idle allowance.
  2. Implement `AlarmBroadcastReceiver` that posts a heads-up system notification with actions ("Acknowledge", "Snooze") using `NotificationManagerCompat`.
  3. Implement `BootCompletedReceiver` that queries pending reminders from `ReminderRepository` on `ACTION_BOOT_COMPLETED` and `ACTION_TIMEZONE_CHANGED` and reschedules them.
  4. Declare receivers and notification channels in `AndroidManifest.xml`.
  5. Add automated unit and integration tests verifying scheduling calculation, dismissal, and reboot query contracts.

---

### Finding `F-003`
- **Title**: Missing Notification & Microphone permission request flow & degraded state UI.
- **Category**: Loop A / Loop E (Functional Correctness & UX).
- **Priority**: P1 (Blocks Critical User Journeys 1 & 5).
- **Severity**: 4 | **Likelihood**: 4 | **Evidence Class**: VERIFIED.
- **Root Cause**: The app does not request `POST_NOTIFICATIONS` or provide an in-app indicator when notification permission is denied. Users who decline notifications will not receive reminders and will have no visibility into why delivery failed.
- **Acceptance Criteria**:
  1. Add permission checking and rationale prompt for `POST_NOTIFICATIONS`.
  2. When permission is denied, display a non-intrusive warning card in `PlanScreen` / `HomeScreen` informing the user that system notifications are disabled and local alarms cannot sound.
- **Status**: **DONE** (Cycle 03).
- **Evidence Paths**: `audit/EVIDENCE/cycle_03_test_run.txt`, `OnboardingScreen.kt`, `PlanScreen.kt`, `UiStateContractsTest`.

---

### Finding `F-004`
- **Title**: Action proposals lack affirmative confirmation and execution UI in screens.
- **Category**: Loop A / Loop C (Correctness & Safety).
- **Priority**: P1 (Blocks Critical User Journey 7).
- **Severity**: 4 | **Likelihood**: 4 | **Evidence Class**: VERIFIED.
- **Root Cause**: `ActionProposal` models exist with `requiresAffirmativeApproval = true`, but AskYaan and Home screens do not provide interactive confirmation cards allowing users to review, confirm, or abort proposals before execution.
- **Acceptance Criteria**:
  1. Provide an `ActionProposalCard` in Compose with clear preview of the action, parameters, and two buttons: "Confirm & Execute" and "Dismiss".
  2. Wire execution to `TwoPhaseActionCommitter` with undo state.
- **Status**: **DONE** (Cycle 04).
- **Evidence Paths**: `audit/EVIDENCE/cycle_04_test_run.txt`, `ActionProposalCard.kt`, `AskYaanScreen.kt`, `HomeScreen.kt`, `UiStateContractsTest`.

---

### Finding `F-005`
- **Title**: Vault export in `YouScreen` is an empty stub with no serialized archive output.
- **Category**: Loop A / Loop F (Functional Correctness & Data Integrity).
- **Priority**: P2 (Blocks Critical User Journey 8).
- **Severity**: 3 | **Likelihood**: 3 | **Evidence Class**: VERIFIED.
- **Root Cause**: `YouUiAction.ExportVault` had a comment `// Export routine` in `YouScreen.kt` without generating an archive.
- **Acceptance Criteria**:
  1. Implement a VaultExporter that serializes notes and tasks into an encrypted or redacted JSON backup.
  2. Provide a download/share intent trigger.
- **Status**: **DONE** (Cycle 05).
- **Evidence Paths**: `audit/EVIDENCE/cycle_05_test_run.txt`, `VaultExporter.kt`, `VaultExporterTest.kt`, `YouScreen.kt`.

---

### Finding `F-006`
- **Title**: Missing age eligibility policy and declaration for India DPDP Act 2023 / COPPA.
- **Category**: Loop G (Legal & Platform Compliance).
- **Priority**: P0 (Regulatory & Store Policy Blocker).
- **Severity**: 5 | **Likelihood**: 3 | **Evidence Class**: INFERRED.
- **Root Cause**: No age eligibility was documented or enforced. India's Digital Personal Data Protection Act (DPDP 2023) prohibits processing data of children (<18) without verifiable parental consent.
- **Acceptance Criteria**:
  1. Restrict YAAN closed beta to users aged 18 and older.
  2. Document in `audit/FOUNDER_ACTIONS.md` and add age declaration in onboarding.
- **Status**: **DONE** (Cycle 03).
- **Evidence Paths**: `audit/EVIDENCE/cycle_03_test_run.txt`, `UserSessionRepositoryTest`, `OnboardingScreen.kt`, `FOUNDER_ACTIONS.md`.

---

### Finding `F-007`
- **Title**: Data safety and privacy disclosures lack explicit documentation of Groq LLM egress.
- **Category**: Loop G / Loop C (Privacy & Compliance).
- **Priority**: P1 (Store Listing & Transparency).
- **Severity**: 4 | **Likelihood**: 4 | **Evidence Class**: VERIFIED.
- **Root Cause**: Prior documentation claimed "100% offline" while the backend/client optionally queries Groq LLM. Google Play Data Safety declaration requires disclosure of external network processing.
- **Acceptance Criteria**:
  1. Generate accurate Google Play Data Safety declaration matrix in `audit/DATA_SAFETY.md`.
  2. Verify that network egress only occurs after affirmative user consent via `CloudConsentManager`.
- **Status**: **DONE** (Cycle 05).
- **Evidence Paths**: `audit/EVIDENCE/cycle_05_test_run.txt`, `audit/DATA_SAFETY.md`, `CLAIMS_REGISTER.md`.

---

### Finding `F-013`
- **Title**: Discrepancy between Progress Tracker maturity claims (100%) and measured implementation actuals.
- **Category**: Loop B / Loop A (Architecture & Governance).
- **Priority**: P1 (Honest Progress Tracking).
- **Severity**: 4 | **Likelihood**: 5 | **Evidence Class**: VERIFIED.
- **Root Cause**: `PROGRESS_TRACKER.md` claimed "100% System Maturity / Closed Beta Ready" while numerous items in Sections 5.1–5.8 are unchecked (`[ ]`), backend services (`CrdtRelayService`, `BriefingWorkerService`, `VaultBackupService`) are in-memory mocks, and iOS native notifications are unimplemented.
- **Acceptance Criteria**:
  1. Recalculate and update `PROGRESS_TRACKER.md` Section 1 to measured reality (~68% maturity).
  2. Mark completed and in-progress items honestly.
  3. Update Executive Health Matrix and Task Audit Log.
- **Status**: **IN-PROGRESS**.
- **Evidence Paths**: `PROGRESS_TRACKER.md`.

---

### Finding `F-014`
- **Title**: Inaccurate 'zero-knowledge' / 'blind relay' marketing claims for cloud LLM paths.
- **Category**: Loop C / Loop G (Privacy & Legal Compliance).
- **Priority**: P0 (Truth in Advertising & Privacy Law).
- **Severity**: 5 | **Likelihood**: 5 | **Evidence Class**: VERIFIED.
- **Root Cause**: Marketing copy and health invariants claim "Zero-Knowledge E2EE Blind Relay" across all flows, but Ask YAAN cloud inference queries transmit user prompt text and top-k note snippets via HTTP to external providers (Groq/Fireworks). Although `VaultRedactor` applies regex PII masking, semantic content is exposed to cloud processors.
- **Acceptance Criteria**:
  1. Audit and update all copy in UI, health endpoints (`/healthz`), and docs: replace "zero-knowledge" with "Consented Cloud-Assisted Intelligence" when referring to LLM features.
  2. Document exact egress payload in `SECURITY_AND_PRIVACY.md` and `DATA_SAFETY.md`.
  3. Ensure 100% on-device local privacy is claimed only for offline SQLDelight storage and on-device parsing.
- **Status**: **IN-PROGRESS**.
- **Evidence Paths**: `backend/src/main/kotlin/com/yaan/backend/Application.kt:112`, `DATA_SAFETY.md`, `CLAIMS_REGISTER.md`.

---

### Finding `F-015`
- **Title**: Architectural ambiguity: backend executing tools vs on-device encrypted vault authority.
- **Category**: Loop B / Loop C (Architecture & Security).
- **Priority**: P1 (Binding Architecture Invariant).
- **Severity**: 4 | **Likelihood**: 5 | **Evidence Class**: VERIFIED.
- **Root Cause**: Earlier documents ambiguously suggested backend services might execute actions on user data. In reality, the user vault is encrypted on-device (SQLDelight + hardware AES-GCM-256) and the server possesses no decryption keys.
- **Acceptance Criteria**:
  1. Ratify Binding Architecture: Backend is strictly a stateless planner returning `ANSWER | CLARIFY | PROPOSAL | REFUSE | ERROR`.
  2. Client is the deterministic executor owning the Action State Machine and SQLDelight mutations.
  3. Implement unified `ToolRegistry` and shared schemas in KMP `:core:domain` used by both client and backend.
- **Status**: **IN-PROGRESS**.
- **Evidence Paths**: `docs/adr/0007-grounded-ask-yaan-architecture.md`, `core/domain/src/commonMain/kotlin/com/yaan/core/domain/tool/`.

---

### Finding `F-016`
- **Title**: Android `USE_EXACT_ALARM` violates Google Play policy for reminder/task apps.
- **Category**: Loop G / Loop F (Platform Policy & Reliability).
- **Priority**: P0 (Play Store Rejection Blocker).
- **Severity**: 5 | **Likelihood**: 5 | **Evidence Class**: VERIFIED.
- **Root Cause**: In Cycle 01, `SCHEDULE_EXACT_ALARM` was replaced with `USE_EXACT_ALARM` to clear an AGP lint error. However, current Google Play policy strictly restricts `USE_EXACT_ALARM` to dedicated alarm clock and calendar apps; generic reminder/to-do apps are subject to store rejection.
- **Acceptance Criteria**:
  1. Declare `SCHEDULE_EXACT_ALARM` in `composeApp/src/androidMain/AndroidManifest.xml`.
  2. In `AndroidAlarmScheduler.kt`, check `alarmManager.canScheduleExactAlarms()` before calling `setExactAndAllowWhileIdle()`.
  3. Fallback gracefully to `setAndAllowWhileIdle()` if permission is not granted.
  4. Ensure lint configuration handles the permission appropriately without failing release builds.
- **Status**: **IN-PROGRESS**.
- **Evidence Paths**: Google Play Policy (2025/2026), `AndroidAlarmScheduler.kt`.

---

### Finding `F-017`
- **Title**: Missing iOS local notification scheduling bridge and 64-pending request limit handling.
- **Category**: Loop F / Loop A (Reliability & Functional Correctness).
- **Priority**: P1 (Platform Feature Parity).
- **Severity**: 4 | **Likelihood**: 5 | **Evidence Class**: VERIFIED.
- **Root Cause**: While Android has `AndroidAlarmScheduler`, iOS has no `AlarmScheduler` implementation in `:composeApp/src/iosMain`. Apple's UserNotifications framework enforces a hard limit of 64 pending notification requests.
- **Acceptance Criteria**:
  1. Implement `IosAlarmScheduler` in `:composeApp/src/iosMain` using `UNUserNotificationCenter`.
  2. Implement a rolling window queue of up to 64 soonest scheduled notifications to prevent system dropping.
  3. Re-queue/refresh pending notifications on app launch and background refresh.
- **Status**: **IN-PROGRESS**.
- **Evidence Paths**: Apple UNUserNotificationCenter documentation, `composeApp/src/iosMain`.

---

### Finding `F-018`
- **Title**: SMS fallback requires plaintext transmission and TRAI DLT registration (TCCCPR 2018).
- **Category**: Loop G / Loop C (Legal Compliance & Privacy).
- **Priority**: P2 (Formally Deferred).
- **Severity**: 3 | **Likelihood**: 4 | **Evidence Class**: VERIFIED.
- **Root Cause**: Sending SMS fallback notifications in India requires registration under TRAI TCCCPR 2018 (DLT portal registration, principal entity KYC, pre-approved header/templates, scrub against NDNC registry). Furthermore, SMS requires routing user reminder text unencrypted through telecom carriers and third-party SMS aggregators.
- **Acceptance Criteria**:
  1. Formally record deferral of SMS fallback in `SPECS_DEFERRED.md` and `FOUNDER_ACTIONS.md`.
  2. Document regulatory and cryptographic privacy justifications.
- **Status**: **DEFERRED**.
- **Evidence Paths**: `SPECS_DEFERRED.md`, `FOUNDER_ACTIONS.md`, TRAI TCCCPR 2018.

---

### Finding `F-019`
- **Title**: Test inventory count drift between tracker entries (82 vs measured 97 unique / 116 executions).
- **Category**: Loop B / Loop H (Quality & Regression Tracking).
- **Priority**: P2 (Verification Accuracy).
- **Severity**: 3 | **Likelihood**: 5 | **Evidence Class**: VERIFIED.
- **Root Cause**: `PROGRESS_TRACKER.md` reported 82 tests, while empirical test runner logs show 97 unique unit tests (116 executions across debug and release build variants), and 0 instrumented tests.
- **Acceptance Criteria**:
  1. Reconcile test counts in `PROGRESS_TRACKER.md`, `SCORECARD.md`, and `GATES.md` to exact measured values: 97 unique unit tests, 116 executions, 0 instrumented tests.
- **Status**: **IN-PROGRESS**.
- **Evidence Paths**: `build/test-results/**/*.xml`, `audit/EVIDENCE/cycle_00_test_inventory.txt`.

