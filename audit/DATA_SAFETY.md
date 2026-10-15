# YAAN Google Play Data Safety & Privacy Disclosures Matrix

> **Authority**: Autonomous Software Engineering & QA System  
> **Status**: CANONICAL REGISTER (Pre-Release Audit)  
> **Finding Addressed**: `F-007` (Data Safety and Privacy Disclosures)  
> **Effective Version**: YAAN v1.0 Closed Beta  
> **Date**: 2026-10-15  

---

## 1. Overview & Google Play Store Declaration Summary

YAAN is designed as a **local-first personal phone assistant**. All core functionality (note taking, task management, exact alarms, reminders, local search via SQLite FTS5) operates 100% on-device with zero cloud dependencies.

To support optional advanced reasoning on complex queries, YAAN provides a **Consented Cloud Inference Bridge** via Groq LPUs. This document details the exact Google Play Data Safety form answers, third-party data flows, redaction mechanisms, and user sovereignty controls.

```
┌────────────────────────────────────────────────────────┐
│             YAAN On-Device Private Vault               │
│   (AES-GCM-256 SQLite · Hardware Android KeyStore)     │
└───────────────────────────┬────────────────────────────┘
                            │
               [User Query Initiated]
                            │
              Is Cloud Consent Granted?
             (CloudConsentManager.kt)
             /                     \
       [NO / Default]            [YES / Opt-In]
           /                         \
          ▼                           ▼
┌──────────────────┐        ┌──────────────────┐
│ 100% Local       │        │ VaultRedactor    │
│ SQLite FTS5      │        │ (Strips PII,     │
│ Rule Matcher     │        │  emails, keys)   │
└──────────────────┘        └─────────┬────────┘
                                      │
                                      ▼
                            ┌──────────────────┐
                            │ TLS 1.3 Egress   │
                            │ Groq API Gateway │
                            │ (Ephemeral ZDR)  │
                            └──────────────────┘
```

---

## 2. Google Play Data Safety Form Matrix

| Data Category | Data Type | Collected? | Shared? | Purpose | Ephemeral? | Required or Optional? |
|---|---|:---:|:---:|---|:---:|:---:|
| **Personal Info** | Name | **No** (Local-only display name) | **No** | App functionality (UI personalization) | N/A | Optional |
| **Personal Info** | Email address | **No** | **No** | None | N/A | N/A |
| **Personal Info** | User IDs | **No** (Anonymous local UUID) | **No** | App functionality | N/A | Required |
| **Messages** | Other in-app messages / notes | **Yes** (Local SQLite) | **Yes (Opt-in only)** | App functionality / Cloud AI assistance | **Yes** (Groq does not retain for training) | **Optional** (Defaults to 100% Local) |
| **Audio** | Voice recordings | **No** (Local Whisper transcription) | **No** | App functionality | N/A | Optional |
| **Calendar & Alarms** | Reminders / Tasks | **Yes** (Local SQLite) | **No** (Remains on device) | App functionality / Alarm scheduling | N/A | Required |
| **Device / Other IDs** | Device attestation token | **Yes** (Play Integrity token) | **Yes** | Fraud prevention & security | **Yes** | Required |

---

## 3. Third-Party Egress & Processor Audit

### 3.1 Primary Cloud Inference Processor
- **Processor Legal Name**: Groq, Inc.
- **Headquarters**: Mountain View, California, USA
- **Endpoint**: `https://api.groq.com/openai/v1/chat/completions`
- **Transport Security**: TLS 1.3 enforced; no fallback to unencrypted HTTP.
- **Data Retention & Training Policy**: Groq commercial API terms state customer inputs and outputs are processed ephemerally and are **NOT used to train foundation models**. Logs are retained only for security and abuse detection (standard 30-day rolling window).
- **Founder Obligation**: Founder must execute enterprise Data Processing Agreement (DPA) with Zero Data Retention (ZDR) schedule (`audit/FOUNDER_ACTIONS.md` Item 1).

### 3.2 Pre-Egress PII Redaction (`VaultRedactor.kt`)
Before any text snippet leaves the device, it is sanitized through `VaultRedactor`:
- Email addresses matched by RFC 5322 regex replaced with `[REDACTED_EMAIL]`.
- Phone numbers (international and national formats) replaced with `[REDACTED_PHONE]`.
- API tokens, JWTs, and private keys replaced with `[REDACTED_SECRET]`.
- URLs containing authentication or query parameters stripped of query strings.

### 3.3 Offline Default & Opt-In Gate (`CloudConsentManager.kt`)
- **Default State**: Cloud inference is strictly **OFF** by default.
- **Explicit Consent**: The user must explicitly toggle "Cloud Reasoning" in `YouScreen` or confirm the Cloud Permission Modal.
- **Degraded Fallback**: If offline, in airplane mode, or if consent is withheld, YAAN falls back to on-device SQLite FTS5 search and deterministic rule execution with zero error dialogs.

---

## 4. User Rights, Deletion, and Sovereign Data Export

### 4.1 Cryptographic Deletion ("Shred Vault")
- The user can trigger **"Shred Vault & Reset"** from `YouScreen` at any time.
- Implementation: `DefaultUserSessionRepository.logoutAndCryptoShred()` deletes all SQLite database tables and destroys cryptographic keys in Android KeyStore.

### 4.2 Data Export Sovereignty (`VaultExporter.kt`)
- In compliance with GDPR Art. 20 and DPDP Act 2023 §12, users can export all personal notes, tasks, and reminders into a self-contained, standard JSON archive at any time via `YouScreen`.

---

## 5. Age Eligibility & Statutory Declarations

- **India DPDP Act 2023 (§9)**: YAAN closed beta strictly prohibits processing personal data of children (<18) and does not engage in behavioral monitoring of minors. Mandatory 18+ declaration is enforced during `OnboardingScreen`.
- **US COPPA**: The application is not directed at children under the age of 13.
- **Target Audience Declaration on Play Console**: Category: "18 and over only".
