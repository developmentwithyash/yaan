# YAAN End-to-End AI System Architecture Specification
## Hybrid Edge-Cloud Inference Mesh, Epistemic Memory & Action Guardrails (v2.0 Gold Master)

> **Document Identifier**: `YAAN-ARCH-AI-2026-006`  
> **Author**: Yash Engineering Systems (`ai-ml-office` & `architecture-office`)  
> **Governing Docket**: `JURY-2026-10-YAAN-006`  
> **Status**: RATIFIED & COMMITTED (Unanimous 12-Juror Acquittal)  
> **Effective Date**: 2026-10-14  

---

## 1. Executive Summary & Core Architectural Thesis

YAAN (*Your Autonomous Action Navigator*) departs fundamentally from legacy "wrapper" AI assistants. Instead of piping all user interactions to centralized cloud LLMs, YAAN implements an **Asymmetric Edge-First Cognitive Architecture**:

1. **0ms Edge Baseline**: 95% of daily interactions (voice capture, note retrieval, intent extraction, commitment tracking, routine gardening) resolve entirely on-device via quantized Small Language Models (SLMs) and on-device vector retrieval at **$0 marginal cloud compute**.
2. **Zero-Knowledge Privacy Invariant**: All personal knowledge graphs, notes, and vector embeddings remain on-device or encrypted with client-derived keys (`AES-GCM-256 + Argon2id`). Plaintext context is NEVER logged, cached, or shared with external model providers without explicit user approval.
3. **Sub-150ms Interaction Ceiling**: When cloud assistance is required, the system utilizes an 80ms debounced speculative HTTP/3 stream to Groq LPUs, delivering sub-100ms Time-to-First-Token (TTFT) and 300+ tok/s streaming.
4. **Pre-computed Async Nightly Intelligence**: Deep cross-calendar synthesis and morning briefing generation execute asynchronously inside hardware-isolated AWS Nitro Confidential Enclaves during off-peak hours (04:00 AM local time) at a 50% API discount, delivering instant 0ms rendering upon user wake-up.
5. **Deterministic Decoupling & Affirmative Action Guardrails**: Probabilistic model outputs are strictly quarantined from direct database mutation or external API execution. All autonomous actions require explicit single-tap user confirmation.

---

## 2. End-to-End Cognitive Topology

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                          EDGE DEVICE (Kotlin Multiplatform App)                         │
│                                                                                         │
│   ┌────────────────────┐      ┌─────────────────────────┐      ┌────────────────────┐   │
│   │ 1. Audio Capture   │ ───> │ 2. On-Device SLM Parser │ ───> │ 3. Epistemic Store │   │
│   │ Whisper-Tiny INT8  │      │ SmolLM2-360M / Gemma-2  │      │ Vector + Relational│   │
│   │ Silero VAD (0ms)   │      │ Grammar Constrained     │      │ BGE-micro INT8     │   │
│   └────────────────────┘      └───────────┬─────────────┘      └────────────────────┘   │
│                                           │ Confidence Score θ                          │
│                                           ▼                                             │
│                       ┌───────────────────────────────────────┐                         │
│                       │ Confidence Evaluator & Cascade Router │                         │
│                       └───────────────────┬───────────────────┘                         │
│                                           │                                             │
│                     ┌─────────────────────┴─────────────────────┐                       │
│                     ▼ [θ ≥ 0.75]                                ▼ [θ < 0.75]            │
│         ┌───────────────────────┐                   ┌───────────────────────┐           │
│         │ Local Deterministic   │                   │ Speculative Stream    │           │
│         │ Repository Execution  │                   │ ConnectRPC / HTTP/3   │           │
│         │ (SQLDelight Local DB) │                   │ 80ms Client Debounce  │           │
│         └───────────────────────┘                   └───────────┬───────────┘           │
└─────────────────────────────────────────────────────────────────┼───────────────────────┘
                                                                  │
                                      ┌───────────────────────────┴───────────────────────────┐
                                      │                                                       │
                                      ▼                                                       ▼
┌────────────────────────────────────────────────────────┐ ┌──────────────────────────────────────────────┐
│       YAAN REAL-TIME INFERENCE MESH (Interactive)      │ │   AWS NITRO CONFIDENTIAL ENCLAVE (Nightly)   │
│                                                        │ │                                              │
│  ┌──────────────────┐        ┌──────────────────────┐  │ │  ┌────────────────────────────────────────┐  │
│  │ Primary: Groq LPU│        │ Fallback: Fireworks  │  │ │  │ 04:00 AM Batch Briefing Generator      │  │
│  │ Llama-3.3-70B    │ <====> │ Cerebras / vLLM      │  │ │  │ Claude 3.5 Sonnet (50% Batch API)      │  │
│  │ TTFT < 100ms     │        │ Circuit Breaker      │  │ │  │ Client Public-Key Payload Encryption   │  │
│  └──────────────────┘        └──────────────────────┘  │ │  └───────────────────┬────────────────────┘  │
│                                                        │ └──────────────────────┼───────────────────────┘
└────────────────────────────────────────────────────────┘                        │ Silent Push
                                                                                  ▼
                                                           ┌──────────────────────────────────────────────┐
                                                           │ Instant 0ms Morning Wake-Up Rendering        │
                                                           └──────────────────────────────────────────────┘
```

---

## 3. The Five Core Architectural Layers

### Layer 1: On-Device Audio & Voice Pipeline (`:feature:voice`)

- **Low-Power Voice Activity Detection (VAD)**:
  - Utilizes Silero VAD (0.5MB quantized ONNX runtime).
  - Evaluates 30ms audio chunks in a circular buffer with sub-1% CPU load.
  - Automatically isolates speech boundaries and prevents background audio from triggering the speech recognizer.
- **Quantized Speech-to-Text (STT)**:
  - On-Device Model: `Whisper-Tiny` INT8 (39MB) executing on Android NNAPI / Qualcomm NPU and Apple Neural Engine via CoreML.
  - Latency: Real-time factor (RTF) of 0.18x (processes 1 second of speech in 180ms on mobile NPU).
  - Zero Cloud Egress: Raw audio buffers are processed entirely in ephemeral RAM and never transmitted to external cloud servers.
- **Duplex Interaction Loop**:
  - `LiveVoiceModal` features instant acoustic echo cancellation and barge-in detection. When the user speaks while YAAN is vocalizing, speech synthesis pauses within 40ms.

---

### Layer 2: On-Device Edge SLM & Grammar-Constrained Parser (`:core:domain`, `:feature:capture`)

- **Quantized Model Stack**:
  - Primary Mobile SLM: `SmolLM2-360M-Instruct` INT4 (210MB storage footprint, < 280MB RAM overhead).
  - High-Tier Mobile / Desktop Option: `Gemma-2-2B-IT` INT4 (1.4GB storage footprint, downloaded conditionally over Wi-Fi).
  - Runtime: `llama.cpp` JNI bindings for Android ARM64 and iOS Metal.
- **Grammar-Constrained Decoding (GBNF)**:
  - Eliminates JSON hallucination and syntax errors by enforcing strict context-free grammars during token generation:
    ```bnf
    root ::= IntentEnvelope
    IntentEnvelope ::= "{" ws "\"intent\":" ws IntentType "," ws "\"confidence\":" ws Number "," ws "\"entities\":" ws EntityMap "}"
    IntentType ::= "\"TASK\"" | "\"NOTE\"" | "\"REMINDER\"" | "\"BRIEFING\"" | "\"QUERY\"" | "\"ACTION\""
    ```
- **Confidence Scoring & Local Dispatch**:
  - The model computes token probability confidence $\theta = \exp(\frac{1}{N} \sum_{i=1}^N \ln P(t_i))$.
  - If $\theta \ge 0.75$: Dispatched immediately to local SQLite repositories (`0ms cloud roundtrip`).
  - If $\theta < 0.75$: Handed over to Layer 3 Cascade Router.

---

### Layer 3: Speculative Cloud Cascade & Multi-Provider Mesh (`:backend:inference`)

- **80ms Client Debounce**:
  - When speech or typing begins, the client buffers user input for 80ms before issuing speculative cloud requests, allowing the edge SLM to resolve simple intents without triggering cloud billing.
- **Speculative Multiplexed ConnectRPC (HTTP/3 QUIC)**:
  - If confidence stabilizes below threshold ($\theta < 0.75$), an HTTP/3 bidirectional stream opens with 0-RTT TLS resumption.
  - If edge processing finishes first with an unambiguous resolution, the client issues a QUIC `RESET_STREAM`, canceling the cloud stream before token generation fees are incurred.
- **Multi-Provider Mesh & Circuit Breaker**:
  - Primary Provider: **Groq LPUs** running `llama-3.3-70b-versatile` ($0.05 / 1M input tokens, TTFT < 100ms, 320 tokens/sec).
  - Secondary Provider: **Fireworks AI** / **Cerebras** running `llama-3.1-70b` ($0.20 / 1M tokens).
  - Tertiary Emergency Fallback: Dedicated self-hosted vLLM cluster on Hetzner GPU instances.
  - Thread-Safe Circuit Breaker Policy:
    - Trips from `CLOSED` to `OPEN` if error rate > 2% or p99 latency > 300ms over 5 consecutive requests.
    - `HALF-OPEN` probe every 30 seconds with 1 test request before traffic restoration.

---

### Layer 4: Epistemic Knowledge Graph & Vector Retrieval (`:feature:memory`, `:core:data`)

- **On-Device Embedding Generation**:
  - Model: `BGE-micro-v2` / `all-MiniLM-L6-v2` INT8 (25MB weights, 384-dimensional dense vectors).
  - Computation: Sub-25ms embedding generation on mobile CPU/NPU.
- **Local Hybrid Vector + Relational Store**:
  - SQLite table with cosine distance extension:
    ```sql
    CREATE TABLE memory_vectors (
        memory_id TEXT PRIMARY KEY,
        subject TEXT NOT NULL,
        predicate TEXT NOT NULL,
        confidence REAL NOT NULL,
        vector BLOB NOT NULL,
        created_at INTEGER NOT NULL,
        expires_at INTEGER NOT NULL
    );
    ```
- **Epistemic Confidence Decay & TTL Pruning**:
  - Every memory fact stores an initial confidence $c_0 \in [0.0, 1.0]$ and an evidence citation backlink (audio memo URI or note ID).
  - Half-life decay formula:
    $$c(t) = c_0 \cdot 2^{-\frac{t - t_0}{\tau}}$$
    - Default $\tau = 30\text{ days}$ for general preferences; $\tau = 7\text{ days}$ for transient travel context.
  - Background daemon prunes memory items when $c(t) < 0.20$ or when $t > \text{expires\_at}$, eliminating hallucinated long-term context drift.

---

### Layer 5: Pre-computed Nightly Enclave Worker & Action Guardrails (`:backend:briefing`, `:feature:actions`)

- **Hardware-Isolated Confidential Compute (AWS Nitro Enclaves)**:
  - Executes nightly at 04:00 AM user local time.
  - Enclave memory is hardware-isolated with cryptographic attestation; external cloud admins cannot inspect plaintext memory.
  - Ingests external calendar updates via OAuth tokens decrypted strictly inside the enclave.
- **50% Discount Batch API**:
  - Utilizes Anthropic Claude 3.5 Sonnet Batch API or DeepSeek-V3 Batch API ($0.0015 / daily briefing).
  - Generates executive synthesis: headline, commitments summary, schedule conflict analysis, cognitive load assessment.
  - Encrypts payload with user device public key (`X25519 + AES-GCM-256`) and dispatches via silent high-priority FCM message.
  - Waking up in flight mode or underground subways renders the briefing instantly at **0ms** with temporal badging (`"Prepared at 4:00 AM • Offline"`).
- **Affirmative Action Guardrails**:
  - Probabilistic AI is strictly quarantined: it can draft proposals (`ActionProposal`), but NEVER execute them directly.
  - State mutations (modifying calendar events, sending emails, deleting tasks) require an explicit, single-tap affirmative user confirmation card with full preview diff.

---

## 4. Evaluation Suite & Performance SLOs

| Metric | Target SLO | Fallback Behavior |
| :--- | :---: | :--- |
| **On-Device VAD Latency** | **< 15ms** | Fallback to threshold energy detector |
| **Whisper-Tiny STT RTF** | **< 0.20x** | Cloud streaming STT fallback if online |
| **SmolLM2 Intent Extraction** | **< 90ms** | Speculative Groq stream takes over |
| **Groq LPU Cloud TTFT** | **< 100ms** | Circuit Breaker routes to Fireworks AI |
| **Local Vector Search (k=5)** | **< 20ms** | SQLite FTS5 lexical keyword match |
| **Morning Briefing Render** | **0ms** | Precomputed payload stored in local DB |
| **Local Memory Footprint** | **< 350MB** | Unload model when app enters background |

---

## 5. Technology Matrix

```
┌───────────────────────────┬───────────────────────────────────┬───────────────────────────────────────────┐
│ Component                 │ Technology Selected               │ Strategic Rationale                       │
├───────────────────────────┼───────────────────────────────────┼───────────────────────────────────────────┤
│ Speech Recognition        │ Whisper-Tiny INT8 (ONNX/CoreML)   │ 39MB, air-gapped on-device transcription. │
│ Voice Activity Detection  │ Silero VAD v4                     │ 0.5MB, <1% CPU, sub-15ms chunk analysis.  │
│ Edge SLM Engine           │ SmolLM2-360M / Gemma-2-2B (GGUF)  │ 210MB–1.4GB INT4, 45 tok/s on mobile NPU. │
│ Embeddings & Vector Search│ BGE-micro-v2 INT8 + SQLite Vector │ 25MB, 384-dim, sub-20ms cosine retrieval. │
│ Grammar Constrained Parser│ llama.cpp GBNF Grammar Engine     │ 100% deterministic JSON output syntax.    │
│ Cloud Inference Mesh      │ Groq LPU (Llama-3.3-70B) + FW     │ 320 tok/s, <100ms TTFT, $0.05/1M tokens.  │
│ Nightly Batch Synthesizer │ Claude 3.5 Sonnet Batch API       │ 50% discount, deep reasoning for briefs.  │
│ Confidential Enclave      │ AWS Nitro Enclaves                │ Hardware attestation, zero cloud leakage. │
│ Action Guardrail Gateway  │ Two-Phase Affirmative Commit      │ Eliminates rogue autonomous side-effects. │
└───────────────────────────┴───────────────────────────────────┴───────────────────────────────────────────┘
```
