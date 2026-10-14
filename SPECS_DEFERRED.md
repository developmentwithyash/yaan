# YAAN — Deferred Technical Specifications & Training Gate (SPECS_DEFERRED.md)

**Document Status**: ACTIVE & BINDING  
**Governing Policy**: Phase 7 Training Gate (Staff ML & Architecture Review)  
**Effective Date**: 2026-10-15  

---

## 1. Iron Rule: The Empirical Training Gate

> **IRON LAW**: **Train nothing until the evaluation harness proves a performance gap that prompt engineering, grammar constraints, and deterministic validators cannot close.**

On the Ask YAAN 310-sample evaluation harness (`EvaluationHarnessTest`):
- **Retrieval Accuracy**: FTS5 + Recency achieves **97.22% Recall@5** and **0.9722 MRR** at **< 1 ms latency**.
- **Action Schema Validity**: **100.0%** via `ActionSemanticValidator`.
- **Action Semantic Validity**: **100.0%** (zero past-date scheduling, 100% entity existence verification).
- **Adversarial Injection Defense**: **100.0%** deflected across 35 distinct attack vectors.

Because prompt engineering, GBNF grammar constraints, and deterministic validators satisfy all Phase 1–5 gates, **no custom fine-tuning or training runs are permitted for the v1 closed beta.**

---

## 2. Deferred Specifications & Technical Rationale

| Specification / Proposal | Initial Hypothesis | Empirical Audit Finding | Deferred Reason & Risk Analysis | Re-evaluation Trigger |
| :--- | :--- | :--- | :--- | :--- |
| **Custom Cross-Encoder Reranker** | Claimed +18% MRR boost over lexical search. | FTS5 alone achieves **0.9722 MRR** at 0 ms latency and 0 MB RAM. | **No Telemetry**: Training a neural cross-encoder requires thousands of in-domain user search click logs. Training on synthetic queries creates distribution shift and risks ranking degradation. Adds 80+ MB RAM and 50ms latency. | Re-evaluate if post-launch beta telemetry shows MRR drops below 0.80 on real queries. |
| **Persona DPO on Hosted Model** | Claimed fine-tuning Groq Llama 70B via Direct Preference Optimization. | **Contradicted**: Third-party hosted API endpoints (Groq/Fireworks) do NOT expose model weight gradients or backward passes. | **Technically Impossible on Hosted APIs**: DPO requires full backpropagation on model logits ($10k+ GPU clusters). Persona alignment is achieved reliably via system prompting and few-shot formatting. | Re-evaluate if self-hosting 70B models on dedicated bare-metal infrastructure (deferred). |
| **Custom On-Device NER Model** | Proposed 50MB token classification model for PII redaction. | Rule-based regex (`VaultRedactor`) achieves **100% swap-back fidelity** on emails, phones, card numbers, amounts, and PINs. | **Unnecessary Bloat**: Adding an on-device NER model adds 40–80 MB storage, 50 MB resident RAM, and 30ms latency on low-end mobile devices without measured recall benefit on structured PII. | Re-evaluate only if unstructured PII leak rates exceed 1% in red-team audit. |
| **70B Parameter Full / LoRA Fine-Tuning** | Fine-tuning a proprietary 70B base model for personal assistant Q&A. | Frontier hosted models (Llama 3.3 70B) with context minimisation follow prompt instructions accurately. | **Prohibitive Cost & Maintenance**: A single 70B fine-tuning run costs $12,000–$25,000 on H100 clusters and creates model drift whenever Meta releases updated checkpoints (e.g. Llama 3.4). | Re-evaluate only at Series A scale ($1M+ ARR). |
| **Action-Extraction LoRA (SmolLM2-360M)** | Fine-tune SmolLM2-360M for edge JSON action extraction. | Deterministic `RuleIntentParser` + `ActionSemanticValidator` achieves 100% schema and semantic validity. | **Eval Proves No Gap**: Rules and GBNF grammars currently achieve 100% compliance on action creation without training or downloading 220 MB weights. | Re-evaluate if user beta tests produce complex unparsed multi-sentence natural language action intents. |

---

## 3. Training Run Governance Protocol

If an action-extraction LoRA training run is approved in the future, the following metrics **MUST** be recorded in this document prior to code integration:
1. **Dataset Provenance & Size**: Full accounting of training samples (target: >= 2,000 founder-approved samples).
2. **Held-out Test Evaluation**: Evaluated against a separate held-out test split of >= 500 samples.
3. **Regression Test**: Proving zero regression against the lexical baseline (`Recall@5 >= 0.9722`).
4. **License Compliance**: Proving base weights and dataset permit unrestricted commercial distribution under `LICENSES.md`.
