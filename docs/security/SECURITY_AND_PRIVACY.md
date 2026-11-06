# YAAN Security & Privacy Specification

> **Authority**: Head of Security & Applied AI Architecture  
> **Classification**: CANONICAL ARCHITECTURAL SPECIFICATION  
> **Target Release**: Invite-Only Closed Beta (India 18+, English Default)  
> **Status**: BINDING & VERIFIED  
> **Last Updated**: 2026-10-15  

---

## 1. System Threat Model & Trust Boundaries

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                  DEVICE TRUST BOUNDARY                                 │
│                                                                                        │
│   ┌───────────────────────────┐                ┌───────────────────────────────────┐   │
│   │   User Interface / Voice  │                │   Android Keystore / iOS Keychain │   │
│   │   (Compose Multiplatform) │                │   (Hardware Master Key AES-256)   │   │
│   └─────────────┬─────────────┘                └─────────────────┬─────────────────┘   │
│                 │                                                │                     │
│                 ▼                                                ▼                     │
│   ┌───────────────────────────┐  Local Cipher  ┌───────────────────────────────────┐   │
│   │   On-Device Action Engine │ ────────────── │   SQLDelight Encrypted SQLite     │   │
│   │   & Deterministic Parser  │   (AES-GCM)    │   (Zero Plaintext at Rest)        │   │
│   └─────────────┬─────────────┘                └───────────────────────────────────┘   │
│                 │                                                                      │
│                 ▼                                                                      │
│   ┌───────────────────────────┐                                                        │
│   │   VaultRedactor (RAM-only)│ ◄── Replaces PII with temporary opaque tokens          │
│   └─────────────┬─────────────┘                                                        │
│                 │                                                                      │
│                 ▼                                                                      │
│   ┌───────────────────────────┐                                                        │
│   │    CloudConsentManager    │ ◄── Mandatory affirmative opt-in gate                  │
│   └─────────────┬─────────────┘                                                        │
└─────────────────┼──────────────────────────────────────────────────────────────────────┘
                  │
                  │ HTTPS / TLS 1.3 (Signed Request / X-Play-Integrity)
                  ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                CLOUD SERVER BOUNDARY (Ktor)                            │
│                                                                                        │
│   ┌───────────────────────────┐                ┌───────────────────────────────────┐   │
│   │    Ktor Stateless Planner │                │   In-Memory Blind CRDT Relay      │   │
│   │   (Schema Validation,     │                │   (Encrypted blobs only;          │   │
│   │    Quotas, Idempotency)   │                │    Zero Decryption Keys)          │   │
│   └─────────────┬─────────────┘                └───────────────────────────────────┘   │
│                 │                                                                      │
│                 │ Zero Plaintext Logging (Audit metadata only: prompt chars, latency)  │
└─────────────────┼──────────────────────────────────────────────────────────────────────┘
                  │
                  │ TLS 1.3 Egress
                  ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                              EXTERNAL PROVIDER BOUNDARY                                │
│                                                                                        │
│   ┌───────────────────────────┐                ┌───────────────────────────────────┐   │
│   │   Groq LPU (Primary)      │                │   Fireworks AI (Fallback)         │   │
│   │   llama-3.3-70b-versatile │                │   llama-3.3-70b-instruct          │   │
│   │   (Ephemeral Inference)   │                │   (Ephemeral Inference)           │   │
│   └───────────────────────────┘                └───────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. End-to-End Data Flow Matrix

Every byte of data in YAAN is categorized across four operational flows:

| Flow Name | Data Content | Egress Destination | Encryption State | Plaintext Retention | Disclosure Status |
|---|---|---|---|---|---|
| **1. Local Notes, Reminders & Tasks** | User notes, reminders, task titles, checklists, search queries | **None (100% On-Device)** | Hardware-backed AES-GCM-256 in local SQLite | Device local sandbox only | Zero cloud egress; fully offline functional |
| **2. Multi-Device State Sync (CRDT)** | Encrypted delta envelopes, Lamport timestamps, vector clocks | Ktor Backend (`/api/v1/sync`) | End-to-End Encrypted (AES-GCM-256 on client) | **Zero on server** (Server is a blind relay; cannot decrypt) | Disclosed in `DATA_SAFETY.md` Section 2 |
| **3. Ask YAAN Cloud Inference** | User prompt text + top-k relevant note snippets (after `VaultRedactor` PII substitution) | External LLM Provider (Groq / Fireworks) via Ktor Gateway | TLS 1.3 in transit; Ephemeral RAM during model inference | **Zero at rest**; Server logs metadata only; Provider contract enforces zero-retention | Disclosed as "Consented Cloud-Assisted Intelligence" |
| **4. Pre-Computed Morning Briefing** | Commitment counts, scheduling conflict timestamps (no personal note text) | Ktor Backend (`/api/v1/briefing`) | TLS 1.3 in transit; In-memory envelope | In-memory cache only | Disclosed in `DATA_SAFETY.md` Section 3 |

---

## 3. Truth-in-Privacy Reconciliation (Addressing Finding `F-014`)

> [!IMPORTANT]
> **CANONICAL TERMINOLOGY GOVERNANCE**:
> - **Permitted Term**: "Zero-Knowledge" applies **STRICTLY** to the CRDT Sync Relay (`CrdtRelayService`) and local vault storage at rest, where the server possesses zero decryption keys.
> - **Forbidden Term**: The phrase "Zero-Knowledge" is **STRICTLY FORBIDDEN** when referring to Cloud LLM Inference (`Ask YAAN` or `InferenceMeshService`).
> - **Mandatory Standard Term**: Cloud reasoning must always be accurately labelled in UI, docs, and marketing as **"Consented Cloud-Assisted Intelligence"**.

### What Leaves the Device During Cloud Inference:
1. **User Prompt Text**: Sanitized through `VaultRedactor` (phone numbers, emails, and financial PINs replaced with `[PHONE_1]`, `[EMAIL_1]`).
2. **Consented Context Snippets**: Up to 3 relevant vault notes (max 100 characters each) selected by local lexical BM25 search.
3. **Hardware / Integrity Metadata**: Play Integrity token (Android) or App Attest token (iOS) for bot/abuse mitigation.

### What NEVER Leaves the Device:
1. Vault encryption master keys (stored exclusively in Android Keystore / iOS Keychain).
2. Unredacted personal credentials, account numbers, or PINs.
3. Full SQLite database files or un-consented note history.
4. Voice audio recordings (STT runs 100% on-device via platform speech recognizers).

---

## 4. Prompt-Injection & Untrusted Content Defenses

1. **Vault Content Is Untrusted Data**: Text stored inside notes or memos can never trigger actions on its own.
2. **Current-Turn Requirement**: An action proposal is generated **ONLY** if the user's explicit current-turn prompt demands it (`VaultUntrustedContentGuard.isUserPromptRequestingAction`).
3. **Directive Neutralization**: Injected command phrases (e.g., `ignore previous instructions`, `transfer money`, `<script>`) are neutralized in memory before entering context pipelines (`VaultUntrustedContentGuard.neutraliseRetrievedDirectives`).
4. **Structured Output Enforcement**: All AI outputs are constrained to schema types (`ANSWER | CLARIFY | PROPOSAL | REFUSE | ERROR`). The model cannot output raw shell, SQL, or arbitrary network requests.

---

## 5. Security Audit Evidence

- **Secrets Scan**: 0 committed API keys, tokens, or private keys across the git tree (verified clean).
- **Network Egress**: Confirmed TLS 1.3-only; plain HTTP disabled via `android:usesCleartextTraffic="false"`.
- **Cross-User Isolation**: Verified via `SecurityAuthTest` (BOLA prevention rejects cross-tenant token manipulations).
