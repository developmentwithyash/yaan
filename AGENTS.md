# YAAN — Project Engineering Policy & Governance
# Canonical Policy (`products/yaan/AGENTS.md`)

**Workspace Context**: `products/yaan/` (YAAN — Your Autonomous Action Navigator)  
**Governing Authority**: Yash Engineering Systems & Antigravity Autonomous Orchestrator  
**Status**: ACTIVE & BINDING  
**Effective Date**: 2026-10-14  

---

## 1. Non-Negotiable Core Law: Mandatory 12-Juror Gate

> [!IMPORTANT]
> **IRON LAW**: From this date forward, **NO architectural, product, technical, schema, or commercial decision may be adopted, committed, or implemented in the YAAN project without first taking judgment from the 12-Juror Idea Jury (`idea-jury`) and recording the verdict in the canonical decision ledger.**

Every decision thesis is presumed **GUILTY** of failure until rigorously stress-tested across all twelve institutional dimensions:

```
┌────────────────────────────────────────────────────────┐
│               PROPOSED PROJECT DECISION                │
│    (Architecture, Feature, Schema, Provider, API)      │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│           12-JUROR IDEA JURY ADVERSARIAL TRIAL         │
│   Seed Investor, ICP Customer, Adjacent Customer,      │
│   Staff Systems Engineer, Product Designer, Growth,    │
│   CFO, Legal/Trust, Competitor, Ops, QA, SRE/Year-2    │
└───────────────────────────┬────────────────────────────┘
                            │
               ┌────────────┴────────────┐
               │                         │
         [< 12 Acquittals]         [12/12 Acquitted]
               │                         │
               ▼                         ▼
┌──────────────────────────────┐   ┌─────────────────────┐
│  APPELLATE REMEDIATION (v1.1)│   │  COMMIT & IMPLEMENT │
│  Harden all fatal charges;   │   │  Log in LEDGER.md,  │
│  re-test before adoption.    │   │  publish ADR, build.│
└──────────────────────────────┘   └─────────────────────┘
```

---

## 2. Decision Ledger & Audit Trail Protocol

Every decision must be permanently documented with a designated docket number in the **Jury Decision Ledger**:

1. **Trial Docket Assignment**: Assign sequential identifier `JURY-YYYY-MM-YAAN-NNN`.
2. **Individual Juror Deliberation**: Each of the 12 jurors must output strictly:
   ```text
   VERDICT: GUILTY | NOT GUILTY
   CHARGE: <the single biggest fatal flaw, 8 words max>
   REASON: <why this breaks, 40 words max>
   FIX: <the one pivot or condition that flips your vote, 25 words max>
   ```
3. **Scorecard & Ledger Recording**:
   - Create or update the detailed trial record in `products/yaan/docs/decisions/` or `products/yaan/docs/`.
   - Add the permanent entry to [`products/yaan/docs/JURY_DECISION_LEDGER.md`](file:///Users/yash/Yash-Workspace/products/yaan/docs/JURY_DECISION_LEDGER.md).
   - If architectural, publish the accompanying ADR in `products/yaan/docs/adr/`.
4. **Appellate Hardening Requirement**: If any juror votes `GUILTY`, the decision cannot be executed in code until an appellate remediation pass addresses all fatal charges.

---

## 3. Preserved Architectural Invariants

Every proposal brought to trial must respect YAAN's non-negotiable architectural invariants:

1. **Deterministic State Decoupled from Probabilistic AI**: Deterministic operations (saving notes, triggering alarms, updating task checkboxes) must always succeed 100% offline without network or LLM availability.
2. **Zero-Knowledge Privacy**: Personal notes, tasks, and memory vectors must be encrypted on-device (AES-GCM-256 + Argon2id) before cloud transmission. The server acts as a blind relay.
3. **Sub-150ms Interaction Ceiling**: On-device processing handles the primary interaction loop at 0ms. Any cloud-assisted query must stream with TTFT < 150ms.
4. **Pre-computed Async Intelligence**: Heavy cloud workloads (e.g. Daily Briefings) must run asynchronously in background enclaves and deliver pre-computed payloads to the local database, guaranteeing 0ms wake-up rendering.

---

## 4. Mandatory Progress Tracking Protocol

> [!IMPORTANT]
> **CONTINUOUS PROGRESS AUDIT LAW**:
> The canonical progress tracker [`PROGRESS_TRACKER.md`](PROGRESS_TRACKER.md) is the central source of truth for all ongoing, completed, and queued work.
> 
> **Every agent execution that creates code, modifies architecture, passes a jury docket, or updates project state MUST**:
> 1. Update the corresponding dimension and checkboxes in [`PROGRESS_TRACKER.md`](PROGRESS_TRACKER.md).
> 2. Append an entry to the **Task Audit Log** (Section 11) with the date, task/docket identifier, author/office, and outcome.
> 3. Recalculate and update the Executive Health Matrix when milestone phases advance.
