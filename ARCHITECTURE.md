# YAAN System Architecture Specification

> **Authority**: Autonomous Senior Engineering System  
> **Status**: ACTIVE & BINDING (Ratified via ADR-0007)  
> **Target Release**: Invite-Only Closed Beta (India 18+, English Default)  
> **Repository Context**: Kotlin Multiplatform (Android + iOS), Ktor Backend  
> **Last Updated**: 2026-10-15  

---

## 1. Architectural Foundations & Invariants

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                 BINDING ARCHITECTURAL INVARIANTS                       │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ 1. DEVICE IS THE SOLE DATA AUTHORITY                                                  │
│    Local SQLDelight SQLite database is the ultimate source of truth.                   │
│    The backend NEVER mutates user data and NEVER holds vault plaintext at rest.        │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ 2. BACKEND IS A STATELESS-FOR-CONTENT PLANNER                                          │
│    Backend provides authenticated entry, schema validation, rate limits, and           │
│    capability-probed provider routing. Returns exactly one of:                         │
│    ANSWER | CLARIFY | PROPOSAL | REFUSE | ERROR.                                       │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ 3. CLIENT IS THE DETERMINISTIC EXECUTOR                                                │
│    Client executes all mutations locally, manages the Action State Machine,           │
│    enforces affirmative user confirmation, re-reads database state to verify commit,   │
│    and generates templated outcome messages from verified results.                     │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ 4. COMMON-PATH DETERMINISTIC ON-DEVICE PARSING                                         │
│    Creating/updating/listing reminders, notes, and tasks are executed locally          │
│    via deterministic grammars without network access. LLM is invoked only for          │
│    low-confidence, complex, or multi-step requests.                                    │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ 5. SHARED VALIDATION & TIME ENGINE                                                     │
│    Tool schemas and the time/recurrence engine reside in a shared KMP module           │
│    (:core:domain) ensuring client and backend validate identically.                    │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ 6. LLM IS NEVER SOURCE OF TRUTH                                                        │
│    The model never determines database state, permissions, action completion, or       │
│    time arithmetic. Success messages are templated from verified commits.              │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Request Lifecycle & Sequence Diagram

```
User Voice / Text
       │
       ▼
┌───────────────────────────┐
│   Deterministic Parser    │──[High Confidence]──► Local Action State Machine ──► SQLDelight Commit
│   (On-Device YaanEngine)  │                                                             │
└─────────────┬─────────────┘                                                             ▼
              │ [Low Confidence / Multi-step]                                     Verified Outcome
              ▼                                                                     (0ms Network)
┌───────────────────────────┐
│  VaultRedactor & Consent  │
│  (PII Masking & Opt-In)   │
└─────────────┬─────────────┘
              │ TLS 1.3 (Signed Request)
              ▼
┌───────────────────────────┐
│    Ktor Cloud Planner     │
│   (Validate, Rate-Limit)  │
└─────────────┬─────────────┘
              │ Prompt + Consented Top-k
              ▼
┌───────────────────────────┐
│   Provider Gateway (Groq) │
│    Capability Probes      │
└─────────────┬─────────────┘
              │ Structured Output
              ▼
┌───────────────────────────┐
│ Server Semantic Validator │
│ (Allowlist, Bounds, IANA) │
└─────────────┬─────────────┘
              │ Exactly one of: ANSWER | CLARIFY | PROPOSAL | REFUSE | ERROR
              ▼
┌───────────────────────────┐
│   Client Action Engine    │
│  (State Machine & Audit)  │
└─────────────┬─────────────┘
              │
    ┌─────────┴─────────┐
    ▼                   ▼
[PROPOSAL]          [ANSWER / CLARIFY]
    │                   │
    ▼                   ▼
Affirmative Tap?    Render Response
    │
 ┌──┴──┐
 ▼     ▼
[YES] [NO]
 │     └── Dismiss / Cancel
 ▼
Local Execution ──► Post-Execution DB Verification ──► Templated Outcome Message
```

---

## 3. Action State Machine

Every action transitions through a formal, deterministic state machine:

```
                  ┌──────────────┐
                  │   PROPOSED   │
                  └──────┬───────┘
                         │
        ┌────────────────┴────────────────┐
        │ [Requires Confirmation]         │ [Safe Read/Create]
        ▼                                 ▼
┌───────────────────────┐       ┌───────────────────┐
│ AWAITING_CONFIRMATION │       │   AUTO_APPROVED   │
└──────────┬────────────┘       └─────────┬─────────┘
           │                              │
     [User Confirms]                      │
           └──────────────┬───────────────┘
                          │
                          ▼
                  ┌───────────────┐
                  │   EXECUTING   │
                  └───────┬───────┘
                          │
           ┌──────────────┼──────────────┐
           ▼              ▼              ▼
┌────────────────────┐ ┌──────┐ ┌─────────────────┐
│ VERIFIED_COMPLETED │ │FAILED│ │ OUTCOME_UNKNOWN │
└──────────┬─────────┘ └──────┘ └────────┬────────┘
           │                             │
       [Undo Tap]                  [Reconcile on]
           ▼                       [ Next Launch]
      ┌─────────┐
      │ UNDONE  │
      └─────────┘
```

### State Definitions:
- `PROPOSED`: Action generated by parser or planner with unique `idempotencyKey`.
- `AWAITING_CONFIRMATION`: Consequential action (delete, bulk update, irreversible change) displayed in UI awaiting explicit user tap.
- `AUTO_APPROVED`: Non-consequential tool invocation (list, search, unambiguous explicit request).
- `EXECUTING`: Local repository mutation in progress.
- `VERIFIED_COMPLETED`: Repository mutation completed and verified by immediate database re-read.
- `FAILED`: Mutation failed with explicit error taxonomy (`VALIDATION_FAILED`, `NOT_FOUND`, `STORAGE_ERROR`).
- `OUTCOME_UNKNOWN`: App killed or crash mid-execution; reconciled on next app boot.
- `EXPIRED`: Proposal not acted on within TTL (default 24 hours).
- `CANCELLED`: User explicitly dismissed proposal.
- `UNDONE`: Reversible action rolled back via `UndoReceipt`.

---

## 4. Shared Tool Registry Specification

Candidate tools implemented in shared KMP module (`:core:domain`):

| Tool Identifier | Scope | Confirmation Policy | Side Effects | Undoable? |
|---|---|---|---|:---:|
| `create_reminder` | Client | Auto-approved if explicit; Confirm if inferred | Inserts row into `reminderEntity`; schedules alarm | Yes (`deleteReminder`) |
| `update_reminder` | Client | Confirm if time shifted > 1 hour | Updates `reminderEntity`; reschedules alarm | Yes (revert previous time) |
| `delete_reminder` | Client | **Explicit Confirmation Mandatory** | Deletes row; cancels PendingIntent | Yes (re-insert) |
| `list_reminders` | Client | Auto-approved (Read-only) | None | N/A |
| `create_note` | Client | Auto-approved | Encrypts & inserts into `noteEntity` | Yes (`deleteNote`) |
| `update_note` | Client | Explicit Confirmation Mandatory | Updates ciphertext in `noteEntity` | Yes (revert text) |
| `delete_note` | Client | **Explicit Confirmation Mandatory** | Deletes row from `noteEntity` | Yes (re-insert) |
| `search_notes` | Client | Auto-approved (Read-only) | None | N/A |
| `create_task` | Client | Auto-approved if explicit; Confirm if inferred | Inserts row into `taskEntity` | Yes (`deleteTask`) |
| `update_task` | Client | Auto-approved for status; Confirm for title | Updates row in `taskEntity` | Yes (revert) |
| `complete_task` | Client | Auto-approved | Sets `is_completed = 1` | Yes (toggle back) |
| `get_preferences` | Client | Auto-approved (Read-only) | Reads user preferences | N/A |

---

## 5. Technology Stack & Module Topology

- **Language & Runtime**: Kotlin Multiplatform 2.0.21, JVM 21, Swift 5.9 / Xcode 16.
- **Client Storage**: SQLDelight 2.0.2, SQLite with on-device AES-GCM-256 field encryption.
- **UI Framework**: Compose Multiplatform 1.7.0 (Android + iOS via ComposeScene).
- **Backend Framework**: Ktor 3.0.0 (Netty engine, ContentNegotiation, StatusPages, CORS).
- **Inference Providers**: Groq LPU (`llama-3.3-70b-versatile`), Fireworks AI (`llama-3.3-70b-instruct`).
