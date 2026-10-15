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
| **`F-004`** | Action proposals lack affirmative confirmation and execution UI in screens | A/C (Security/Correctness) | **P1** | 4 | 4 | VERIFIED | **OPEN** | 0 | `AskYaanScreen.kt`, `HomeScreen.kt` |
| **`F-005`** | Vault export in `YouScreen` is an empty stub with no serialized archive output | A/F (Data/Correctness) | **P2** | 3 | 3 | VERIFIED | **OPEN** | 0 | `YouScreen.kt#L114` |
| **`F-006`** | Missing age eligibility policy and declaration for India DPDP Act 2023 / COPPA | G/C (Legal/Compliance) | **P0** | 5 | 3 | VERIFIED | **DONE** | 1 | `UserSessionRepositoryTest`, `OnboardingScreen.kt` |
| **`F-007`** | Data safety and privacy disclosures lack explicit documentation of Groq LLM egress | G/C (Legal/Compliance) | **P1** | 4 | 4 | VERIFIED | **OPEN** | 0 | `CLAIMS_REGISTER.md`, `THREAT_MODEL.md` |

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

---

### Finding `F-005`
- **Title**: Vault export in `YouScreen` is an empty stub with no serialized archive output.
- **Category**: Loop A / Loop F (Functional Correctness & Data Integrity).
- **Priority**: P2 (Blocks Critical User Journey 8).
- **Severity**: 3 | **Likelihood**: 3 | **Evidence Class**: VERIFIED.
- **Root Cause**: `YouUiAction.ExportVault` has a comment `// Export routine` in `YouScreen.kt` without generating a file.
- **Acceptance Criteria**:
  1. Implement a VaultExporter that serializes notes and tasks into an encrypted or redacted JSON backup.
  2. Provide a download/share intent trigger.

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
