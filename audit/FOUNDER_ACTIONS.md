# YAAN Founder Actions & Strategic Governance Log
**Document Identifier**: `FOUNDER-ACTIONS-2026-001`  
**Date**: 2026-10-15  
**Auditor**: Senior Staff Engineer, ML & Security Reviewer  
**Status**: ACTIVE PENDING FOUNDER REVIEW

---

## 1. Immediate Decisions Required From Founder

### Decision 1: Evaluation Vault Language Scope
- **Context**: The evaluation harness in Phase 1 requires synthetic test vaults (100, 1,000, 10,000 items) and >= 300 labelled queries.
- **Options**:
  - **Option A (Recommended Default)**: English-only test fixtures for Closed Beta Phase 1. Fastest path to reliable baseline; eliminates multilingual tokenization noise.
  - **Option B**: Bilingual English + Hindi/Hinglish test fixtures. Requires specialized Hindi romanized token normalization and Hinglish semantic matching.
- **Current Action**: Proceeding with **Option A (English)** as default. Ready to ingest Hindi/Hinglish fixtures upon founder confirmation.

### Decision 2: Cloud Inference Provider & Commercial Terms
- **Context**: Groq pricing for `llama-3.3-70b-versatile` is verified at **$0.59 / 1M input** and **$0.79 / 1M output tokens** (not $0.05).
- **Options**:
  - **Option A**: Use Groq LPU with verified $0.59/$0.79 pricing. Requires founder to set up account, enter billing information, and accept Groq Commercial Terms of Service.
  - **Option B**: Use Fireworks AI / Together AI / DeepSeek API as primary or secondary provider based on evaluated cost per token.
  - **Option C**: Pure local search and rule-based parser only for Closed Beta ($0 cloud cost).
- **Current Action**: Building provider-agnostic gateway interface with simulated/local adapters. **No accounts opened, no terms accepted, no funds committed.**

### Decision 3: Client Attestation Mechanism for Closed Beta
- **Context**: Backend security requires authenticating genuine Android clients to prevent API key abuse.
- **Options**:
  - **Option A (Recommended)**: Google Play Integrity API for Android production builds; HMAC app-secret signature for local debug builds.
  - **Option B**: Custom mTLS certificate pinned in app bundle.
- **Action Required by Founder**: Enable Play Integrity API in Google Play Console prior to production release.

### Decision 4: Age Eligibility & India DPDP Act 2023 Parental Consent
- **Context**: India's Digital Personal Data Protection Act (DPDP 2023) Section 9 prohibits processing personal data of children (defined as under 18) without verifiable parental consent, and strictly prohibits tracking/behavioral monitoring.
- **Options**:
  - **Option A (Recommended Default)**: Restrict Closed Beta to adults aged 18 and older. Require an affirmative age declaration during onboarding.
  - **Option B**: Implement verifiable parental consent mechanisms (requires government ID / DigiLocker verification, high operational friction).
- **Current Action**: Enforcing **Option A (18+ only)** in app terms and onboarding to eliminate child data liabilities. Founder to ratify in legal terms.

---

## 2. Legal, Regulatory & External Compliance To Be Executed By Founder

> [!WARNING]
> The engineering team will NOT accept terms, sign DPAs, or make statutory filings. The founder must execute the following prior to commercial launch:

1. **Data Processing Agreements (DPAs)**:
   - Execute standard DPA with Anthropic, Groq, or any external model provider explicitly selecting "Zero Data Retention" (ZDR) for user API traffic.
2. **HIPAA & SOC2 Attestations**:
   - Do NOT market or claim HIPAA or SOC2 compliance until formal BAA execution and external auditor certification have taken place.
3. **App Store Data Safety Declarations**:
   - Google Play: Declare ephemeral transmission of text queries for AI assistance (only when user consents).
   - Apple App Store: Disclose user data collection categories and opt-in status.
4. **Terms of Service & Privacy Policy**:
   - Legal review of draft Data Safety disclosures produced in `THREAT_MODEL.md`.

---

## 3. Remote Feature Flags & Kill Switches

The following flags will be provisioned in the configuration system (all defaulting to OFF except local search):
- `local_search_enabled`: **true** (Local SQLite FTS5)
- `cloud_ai_enabled`: **false** (Requires explicit user consent & founder switch)
- `voice_mode_enabled`: **false** (Gated behind Phase 6 evaluation)
- `local_slm_intent_enabled`: **false** (Gated behind Phase 5 evaluation)
- `kill_switch_cloud_all`: **false** (When true, immediately cuts all cloud egress across all active clients)
