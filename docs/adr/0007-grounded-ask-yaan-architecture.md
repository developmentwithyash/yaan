# ADR 0007: Grounded "Ask YAAN" Architecture & Empirical Safety Invariants

## Status
**SUPERSEDES ADR 0006** (Ratified 2026-10-15 following Phase 0 Claims Audit)

---

## Context
ADR 0006 adopted a theoretical "Hybrid Edge-Cloud AI Architecture" that included several claims contradicted by codebase analysis and current cloud realities:
1. It claimed "Zero-Knowledge" and "100% Offline" while simultaneously specifying an unconsented 80ms speculative cloud cascade to Groq LPUs.
2. It assumed Groq Llama 3.3 70B cost $0.05 / 1M input tokens, whereas verified provider pricing is $0.59 / 1M input and $0.79 / 1M output (11.8x difference).
3. It claimed $0.00 billing on cancelled streams, ignoring that cloud providers bill for prompt tokens upon ingestion.
4. It proposed running Claude 3.5 Sonnet inside AWS Nitro Enclaves, which is technically impossible for a hosted third-party SaaS API.
5. It proposed automated TTL pruning of notes by an exponential decay formula, which violates user trust and data retention safety.
6. It referenced HIPAA compliance without executed BAAs or third-party audits.

This ADR establishes empirical, evidence-gated architectural invariants for "Ask YAAN" v1.

---

## Decision

### 1. Deterministic State Decoupled from Probabilistic AI
- The application remains 100% functional with all AI disabled.
- AI (local SLMs or cloud models) can **only generate proposals** (`ActionProposal`).
- Repositories commit mutations only upon affirmative single-tap user confirmation.
- Every committed action produces an `UndoToken` allowing 1-tap rollback.

### 2. Retrieval Invariants & Data Safety
- User notes, tasks, and transcripts must **NEVER be auto-deleted, pruned, or truncated by decay formulas**.
- Recency is strictly an optional ranking weight during retrieval scoring.
- Derived knowledge facts may only be flagged as expired if the user can inspect, search, and restore them.
- Every answer or proposed action MUST carry explicit `citationSourceId` references linking directly to source vault items.

### 3. Consented, Minimal Cloud Path (No Speculative Leakage)
- No speculative cloud requests are issued before local resolution.
- The app defaults to **Local Vault Search Only** (100% offline, $0 cloud cost).
- Cloud assistance is gated behind an explicit, plain-language consent modal:
  - Discloses that only top-k retrieved snippets leave the device (never the full vault).
  - Identifies the processor and zero-retention policy.
  - Provides a toggle to "Ask locally only" per query or globally.
  - Can be revoked at any time in Settings.
- Pre-flight redaction: regex-based scrubbers mask phone numbers, emails, credit card patterns, and user-specified protected terms before egress.

### 4. Honest Abstention Over Hallucination
- If retrieved context does not contain sufficient factual evidence to answer a query, or if retrieval confidence is below a provisional threshold, the system must abstain and honestly state: *"I could not find information about that in your vault."*
- Models must not invent dates, attendees, commitments, or facts not present in cited vault records.

### 5. Multi-Provider Gateway Architecture
- The backend utilizes a provider-agnostic `LlmGateway` interface.
- Model names, token caps, and pricing are loaded dynamically from configuration.
- Standard streaming (SSE/HTTP) is used for reliable token delivery.
- A remote-controlled kill switch (`kill_switch_cloud_all`) immediately terminates all cloud egress if needed.

### 6. Truth in UI Copy & Terminology
- UI copy is strictly forbidden from claiming "Zero-Knowledge", "100% Offline", or "HIPAA-Compliant" when cloud components are engaged.
- Badges must truthfully state current execution state (e.g., "Local Vault Search" vs. "Cloud-Assisted Q&A").

---

## Consequences
- **Security & Privacy**: Zero data leakage without explicit user consent; eliminates speculative prompt egress.
- **Product Safety**: User notes cannot be destroyed by automated algorithms.
- **Cost Realism**: Accurate economic models based on verified $0.59 / $0.79 Groq pricing prevent unexpected runway depletion.
- **Compliance**: Eliminates statutory and regulatory exposure from ungrounded HIPAA/Zero-Knowledge claims.
