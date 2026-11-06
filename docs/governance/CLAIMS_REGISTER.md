# YAAN System Architecture Claims Register & Empirical Audit
**Audit Date**: 2026-10-15  
**Auditor**: Senior Staff Engineer, ML & Security Reviewer  
**Context**: Ask YAAN v1 Grounded Architecture  
**Status**: CANONICAL REGISTER (Pre-Coding Verification)

---

## 1. Executive Summary

This register audits all architectural, performance, security, and economic claims made across `docs/YAAN_AI_SYSTEM_ARCHITECTURE.md`, `docs/adr/0006-hybrid-edge-cloud-ai-architecture.md`, and `docs/YAAN_AI_ARCHITECTURE_JURY_TRIAL.md`. Every claim is classified into one of three strict statuses:
- **Verified**: Confirmed by running code, reproducible measurements, or official provider documentation.
- **Contradicted**: Directly refuted by codebase reality, laws of distributed systems/physics, security invariants, or current provider pricing.
- **Unverified**: Theoretical hypothesis or unmeasured assertion without empirical data or test suite proof.

---

## 2. Comprehensive Claims Register Table

| # | Document & Section | Original Claim | Audit Finding & Evidence | Canonical Status | Remediation Required |
|---|---|---|---|:---:|---|
| 1 | `YAAN_AI_SYSTEM_ARCHITECTURE.md` §1 | **"Zero-Knowledge Privacy" & "100% Offline"** while running speculative cloud inference | `InferenceMeshService.kt` and Architecture Layer 3 describe speculative streaming to Groq LPUs over HTTP/3 when $\theta < 0.75$. Plaintext prompts and vault snippets leaving the device to a cloud provider are neither "zero-knowledge" nor "100% offline". | **CONTRADICTED** | Scrub "Zero-Knowledge" and "100% Offline" from all data paths involving cloud calls. The app must default to local-only search and require explicit, informed user consent before any snippet leaves the device. |
| 2 | `YAAN_AI_SYSTEM_ARCHITECTURE.md` §1 | **"0 ms local baseline" and "< 100 ms TTFT"** | Physical on-device computation takes finite time: quantized Whisper-Tiny takes ~180ms RTF, local embeddings take ~25ms, and SLM decode takes 20-50ms/token. Cellular/Wi-Fi TTFT to Groq involves DNS, TLS handshake, transmission, and queueing (~120–250ms mobile network p95). "0ms local" is physically impossible. | **CONTRADICTED & UNVERIFIED** | Replace "0ms" with measured latency budgets. Label all latency targets as PROVISIONAL until measured on physical target devices (Pixel 6a, low-end ARM). |
| 3 | `YAAN_AI_SYSTEM_ARCHITECTURE.md` §1 | **"95% of queries resolve locally"** | There is zero empirical telemetry, no user query distribution dataset, and no eval harness testing what fraction of queries can be answered by an on-device SLM versus retrieval or cloud. | **UNVERIFIED** | Treat 95% as an unproven founder hypothesis. Build evaluation harness in Phase 1 to measure actual local retrieval coverage. |
| 4 | `YAAN_AI_SYSTEM_ARCHITECTURE.md` §3 Layer 3 | **"$0.00 on cancelled speculative streams"** | Cloud LLM providers (including Groq, Fireworks, and OpenAI) bill for prompt input tokens immediately upon ingestion at the API gateway regardless of whether the client issues an HTTP/2 RST_STREAM or QUIC RESET_STREAM mid-generation. | **CONTRADICTED** | Acknowledge that speculative requests incur prompt input token charges. Eliminate speculative background cloud streaming prior to local resolution. |
| 5 | `YAAN_AI_ARCHITECTURE_JURY_TRIAL.md` Juror 09 | **"99.8% schema adherence via GBNF"** | No GBNF grammar parser is integrated in the Kotlin Multiplatform codebase; no test suite or benchmark outputs exist to substantiate 99.8%. | **UNVERIFIED** | Implement deterministic rule/grammar validators and measure actual schema validity and semantic validity on the eval dataset. |
| 6 | `YAAN_AI_SYSTEM_ARCHITECTURE.md` §3 Layer 3 | **"Groq LPU llama-3.3-70b-versatile ($0.05 / 1M input tokens)"** | Official Groq API pricing (verified October 2025/2026) for `llama-3.3-70b-versatile` is **$0.59 per 1M input tokens** and **$0.79 per 1M output tokens**. The claim of $0.05 is an **11.8x underestimate** of input costs. | **CONTRADICTED** | Re-calculate all unit economic models using $0.59 / $0.79 per 1M tokens. Read model names and prices from configuration instead of hardcoding. |
| 7 | `YAAN_AI_SYSTEM_ARCHITECTURE.md` §1 | **"98% gross margins" & "$0.14/user/mo blended COGS"** | Derived using the erroneous $0.05/1M token pricing, without accounting for app store commissions (Apple/Google 15–30%), payment processing fees (Stripe 2.9% + $0.30), token length variance, and real query volumes. | **CONTRADICTED** | Rebuild multi-scenario cost model (Phase 9) based on verified provider pricing, app store cuts, and token caps. |
| 8 | `YAAN_AI_SYSTEM_ARCHITECTURE.md` §3 Layer 5 | **"Claude 3.5 Sonnet Batch API inside AWS Nitro Confidential Enclaves"** | Anthropic's Claude API is a proprietary, hosted SaaS endpoint (`api.anthropic.com`). External model weights cannot be loaded inside a private Nitro Enclave; calling the API from within an enclave transmits plaintext data over the public internet to Anthropic, negating enclave hardware isolation. | **CONTRADICTED** | Clarify that enclave processing can only isolate local container code (e.g., self-hosted models or data pre-processing). Third-party APIs are external processors requiring DPAs and explicit user consent. |
| 9 | `docs/adr/0006-hybrid-edge-cloud-ai-architecture.md` §Context | **"HIPAA compatibility / HIPAA attestation"** | YAAN has no executed Business Associate Agreement (BAA) with AWS, Groq, Fireworks, or Anthropic, nor has an independent SOC2 Type II or HIPAA audit been performed. Making HIPAA claims exposes the company to severe regulatory liability. | **CONTRADICTED & PROHIBITED** | Strip all mentions of HIPAA compliance from docs, marketing, and UI copy. |
| 10 | `docs/YAAN_AI_ARCHITECTURE_JURY_TRIAL.md` Juror 09 | **"Persona DPO on a hosted model"** | Direct Preference Optimization (DPO) requires computing policy and reference model log-probabilities over token gradients during backpropagation. This is technically impossible on hosted black-box APIs (like Groq or Anthropic). | **CONTRADICTED** | Defer custom model training. Rely on prompt engineering, few-shot examples, and local rule validators. Log deferral in `SPECS_DEFERRED.md`. |
| 11 | `YAAN_AI_SYSTEM_ARCHITECTURE.md` §3 Layer 4 | **"Automated TTL pruning daemon removing stale context (c < 0.20)"** | Automatically deleting user notes or knowledge based on an exponential decay formula violates product integrity and user trust. Users must never have their notes or data auto-deleted by a decay algorithm. | **CONTRADICTED** | Recency must strictly be a ranking score weight during retrieval. User notes and tasks must NEVER be auto-deleted. Derived memory facts may only be marked expired if visible and restorable by the user. |
| 12 | `AskYaanScreen.kt` Line 237 | **UI Badge: "Zero-Cloud Memory · On-Device SQLite"** | Displaying "Zero-Cloud Memory" while the reasoner can issue speculative or fallback cloud API calls to Groq/Fireworks is misleading and false whenever cloud is active. | **CONTRADICTED** | Replace UI copy with dynamic truth: "Local Vault Only" when offline/unconsented, and "Cloud-Assisted (Consented)" when cloud queries are executed. |

---

## 3. Mandatory Governance Outcomes
1. **Superseding ADR**: Publish ADR 0007 (`docs/adr/0007-grounded-ask-yaan-architecture.md`) superseding ADR 0006 on all contradicted invariants.
2. **Founder Action Items**: Capture all commercial, legal, and configuration decisions in `FOUNDER_ACTIONS.md`.
3. **Copy Redaction**: Clean all misleading badges and claims from `AskYaanScreen.kt`.
