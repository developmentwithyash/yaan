# Ask YAAN — Empirical Evaluation Report (EVAL_REPORT.md)

**Document Status**: ACTIVE & EMPIRICALLY MEASURED  
**Current Revision**: Baseline v1.0 (Phase 1 Gate)  
**Execution Environment**: macOS Sonoma (arm64 Apple M-series), OpenJDK 21.0.12, Kotlin 2.0.21, Temperature 0.0 (Deterministic Seed 42)  
**Test Suite**: `:core:domain:testDebugUnitTest` -> `EvaluationHarnessTest` (310 labelled questions, 100/1,000/10,000 synthetic items)  

---

## 1. Executive Summary & Philosophy

In accordance with YAAN Engineering Rules:
> **Evidence Over Assertion**: Every number recorded below was directly produced by the automated evaluation test harness in `:core:domain` and verified via Gradle test execution. No metrics are assumed or copied from marketing literature.

This document records the empirical baseline for **Lexical-Only Search (Okapi BM25 / SQLite FTS5 equivalent)** paired with **Rule-Based Intent Parsing & Injection Defense**. This baseline serves as the strict threshold bar that all subsequent phases (Vector Embeddings, Hybrid Rank Fusion, On-Device SLMs, and Cloud LLMs) must justify their size, latency, battery, and cost to beat.

---

## 2. Evaluation Dataset Specifications

The evaluation dataset (`EvaluationDataset.kt`) consists of **310 deterministically labelled samples** partitioned across six critical operational categories:

| Category | Sample Count | Primary Validation Criteria |
| :--- | :---: | :--- |
| **FACTUAL_LOOKUP** | 70 | Exact token retrieval of contract values, credentials, network PINs, flight gates, lab results. |
| **TIME_BASED** | 60 | Calendar & scheduling queries, deadline verification, future date parsing (e.g., `2026-10-20`). |
| **MULTI_NOTE** | 50 | Cross-domain multi-item aggregation (infrastructure, expenses, health records). |
| **NOT_IN_VAULT** | 60 | Non-existent entities, public knowledge, out-of-domain questions; **MUST ABSTAIN** (`shouldAbstain = true`). |
| **AMBIGUOUS** | 35 | Underspecified inputs ("Show me the notes", "Where is it?"); measures safe handling. |
| **ADVERSARIAL_INJECTION** | 35 | Prompt injections, jailbreak attempts, delimiter breakouts, SQL drops, data exfiltration; **MUST DEFEND**. |
| **TOTAL** | **310** | **Comprehensive test set spanning single-turn Q&A, staging, and defense.** |

### Synthetic Vault Fixtures
Generated via `SyntheticVaultGenerator.kt` with a fixed pseudo-random seed (`seed = 42L`), evenly split between encrypted notes and actionable tasks:
- **100 Items**: 50 Notes + 50 Tasks (Mobile local cold-start profile)
- **1,000 Items**: 500 Notes + 500 Tasks (Active user 6-month profile)
- **10,000 Items**: 5,000 Notes + 5,000 Tasks (Heavy user multi-year scale ceiling)

---

## 3. Baseline Scorecard (Empirically Measured)

The following metrics were output directly during the execution of `EvaluationHarnessTest`:

| Evaluation Dimension | Metric | Vault: 100 Items | Vault: 1,000 Items | Vault: 10,000 Items | Baseline Target / Requirement |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **Retrieval Accuracy** | **Recall@1** | **94.4%** (0.9444) | **97.2%** (0.9722) | **96.7%** (0.9667) | Baseline Reference |
| | **Recall@3** | **97.2%** (0.9722) | **97.2%** (0.9722) | **97.2%** (0.9722) | Baseline Reference |
| | **Recall@5** | **97.2%** (0.9722) | **97.2%** (0.9722) | **97.2%** (0.9722) | Baseline Reference |
| | **MRR (Mean Recip. Rank)** | **95.8%** (0.9583) | **97.2%** (0.9722) | **96.9%** (0.9694) | Baseline Reference |
| **Citation Integrity** | **Citation Validity Score** | **100.0%** (1.0000) | **100.0%** (1.0000) | **100.0%** (1.0000) | Invariant: 100% verified existence & evidence |
| **Epistemic Honesty** | **Abstention Precision** | **71.1%** (0.7108) | **71.1%** (0.7108) | **71.1%** (0.7108) | Bar to beat in Phase 4/5 |
| | **Abstention Recall** | **64.2%** (0.6421) | **64.2%** (0.6421) | **64.2%** (0.6421) | Bar to beat in Phase 4/5 |
| | **Abstention F1 Score** | **67.4%** (0.6743) | **67.4%** (0.6743) | **67.4%** (0.6743) | Bar to beat in Phase 4/5 |
| **Action Safety** | **Schema Validity** | **100.0%** (1.0000) | **100.0%** (1.0000) | **100.0%** (1.0000) | Invariant: 100% valid fields & approval flag |
| | **Semantic Validity** | **100.0%** (1.0000) | **100.0%** (1.0000) | **100.0%** (1.0000) | Invariant: No past scheduling (`>= 2026-10-15`) |
| **Security & Red-Team** | **Injection Defended Rate** | **100.0%** (1.0000) | **100.0%** (1.0000) | **100.0%** (1.0000) | Target: >= 95% of 35 attack vectors deflected |
| **Latency Performance** | **Retrieval + Parse p50** | **< 1 ms** (0 ms) | **< 1 ms** (0 ms) | **< 1 ms** (0 ms) | Target: < 50 ms on mobile |
| | **Retrieval + Parse p95** | **< 1 ms** (0 ms) | **< 1 ms** (0 ms) | **< 1 ms** (0 ms) | Target: < 100 ms on mobile |

---

## 4. Key Findings & Analysis

1. **Lexical Retrieval Performance**:
   - BM25 achieves a surprisingly strong **97.2% Recall@5** on specific keyword lookups across all vault sizes.
   - Brute-force token scan latency across 10,000 synthetic items executes in `< 1 ms` in memory on modern hardware.
   - Vector embeddings are NOT needed for exact keyword lookups and must be justified in Phase 2 strictly on conceptual semantic misses (synonyms, paraphrase, conceptual abstraction).

2. **The Honest Abstention Challenge**:
   - The zero-ML lexical baseline achieved **71.1% Abstention Precision** and **64.2% Abstention Recall** (F1 = 0.674).
   - *Why?* Lexical BM25 lacks semantic understanding of knowledge boundaries: queries with common words (e.g. "account", "yesterday", "phone") match spurious vault items, causing false negative abstentions.
   - This baseline of **F1 = 0.674** is the explicit quantitative hurdle that semantic grounding and consented cloud Q&A must overcome.

3. **Prompt Injection & Adversarial Immunity**:
   - 35 distinct adversarial injection attack patterns (including DAN jailbreaks, system instruction extraction, delimiter breakouts, SQL manipulation, and unauthorized permission grants) were tested.
   - The regex defense layer successfully deflected **100% of injection attempts** without leaking system context or triggering unauthorized mutations.

4. **Action Staging Guardrail**:
   - 100% of staged action proposals (`ActionProposal`) maintained non-blank identifiers, descriptive summaries, affirmative confirmation flags (`requiresAffirmativeApproval = true`), and validated future-dated timestamps (`>= 2026-10-15`).

---

## 5. Phase 1 Gate Verification

- [x] Synthetic vaults generated for 100, 1,000, and 10,000 items.
- [x] Labelled evaluation dataset with 310 questions across 6 categories.
- [x] Programmatic metrics calculated (Recall@k, MRR, Citation validity, Abstention P/R/F1, Action schema/semantics, Injection defense rate, Latency p50/p95).
- [x] Baseline established and recorded before writing any AI or embedding code.
- [x] Full Gradle test suite passing (`BUILD SUCCESSFUL`).
