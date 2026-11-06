# ADR 0006: End-to-End Hybrid Edge-Cloud AI Architecture & Inference Mesh (v2.0 Gold Master)

## Status
Accepted (v2.0 Gold Master)

## Context
YAAN (*Your Autonomous Action Navigator*) requires an AI architecture that satisfies 4 non-negotiable invariants:
1. **Deterministic State Decoupled from Probabilistic AI**: Deterministic operations (saving notes, triggering alarms, checking off tasks) must never fail or be gated by network or LLM latency.
2. **Zero-Knowledge Privacy**: Raw audio streams, notes, personal tasks, and epistemic memory vectors must remain encrypted on-device.
3. **Sub-150ms Interaction Ceiling**: On-device interactions must feel instant (0ms). Any speculative cloud query must stream with TTFT < 100ms.
4. **Pre-computed Async Intelligence**: Heavy multi-source synthesis (Daily Briefings) must be computed in the background and delivered pre-computed to local SQLite, guaranteeing 0ms wake-up rendering.

Existing industry architectures either funnel all context to costly cloud LLMs ($3–$10/user/mo), or rely on naive on-device models that suffer from JSON hallucination, memory bloat, and thermal throttling.

Following technical evaluation, this ADR ratifies the comprehensive end-to-end AI system architecture.

## Decision
We adopt the **Hybrid Edge-Cloud Cognitive Architecture (v2.0 Gold Master)**:

1. **On-Device Voice & Audio Ingestion (`:feature:voice`)**:
   - Quantized `Whisper-Tiny` INT8 (39MB) with on-device NPU/ANE acceleration.
   - Low-power `Silero VAD` v4 (0.5MB) evaluating 30ms audio chunks with sub-1% CPU load.
   - Zero raw audio egress to the cloud; ephemeral memory buffers only.

2. **Edge SLM & Grammar-Constrained Parsing (`:core:domain`, `:feature:capture`)**:
   - Primary: `SmolLM2-360M-Instruct` INT4 (210MB storage footprint, < 280MB RAM overhead).
   - High-Tier Option: `Gemma-2-2B-IT` INT4 (1.4GB) downloaded on-demand post-onboarding.
   - GBNF Grammar-Constrained Decoding enforcing 100% deterministic JSON schemas.
   - Token confidence threshold $\theta \ge 0.75$: Resolves locally at 0ms and $0 marginal compute.

3. **Asymmetric Speculative Cloud Cascade (`:backend:inference`)**:
   - 80ms client input debouncing to prevent speculative cloud dispatches during active speech/typing.
   - ConnectRPC over HTTP/3 (QUIC) stream dispatch if confidence $\theta < 0.75$.
   - Primary router: **Groq LPUs** running `llama-3.3-70b-versatile` (<100ms TTFT, 320 tok/s).
   - Fallback router: Fireworks AI / Cerebras governed by a thread-safe `CircuitBreaker`.
   - Client-side Named Entity Recognition (NER) redacting PII before cloud offload.
   - Instant HTTP/3 `RESET_STREAM` cancellation if edge model finishes first, resulting in $0 token billing.

4. **On-Device Epistemic Knowledge Graph (`:feature:memory`)**:
   - Quantized `BGE-micro-v2` / `all-MiniLM-L6-v2` INT8 (25MB, 384-dim embeddings).
   - Local SQLite vector storage with sub-20ms cosine similarity indexing.
   - Temporal half-life confidence decay: $c(t) = c_0 \cdot 2^{-\frac{\Delta t}{\tau}}$ ($\tau=30\text{d}$ for preferences, $\tau=7\text{d}$ for transit context).
   - Bi-directional citation backlinks linking extracted facts to original voice memos/notes.
   - Automated TTL pruning daemon removing stale context ($c < 0.20$).

5. **Pre-computed Nightly Intelligence Worker (`:backend:briefing`)**:
   - AWS Nitro Confidential Enclaves executing at 04:00 AM user local time.
   - Claude 3.5 Sonnet / DeepSeek-V3 Batch API at 50% discount ($0.0015/brief).
   - Generates morning briefing with schedule conflict detection and cognitive load scoring.
   - Pre-computed payload encrypted with user device public key and pushed via silent high-priority FCM message for 0ms wake-up rendering.

6. **Affirmative Action Guardrails (`:feature:actions`)**:
   - Probabilistic AI is strictly quarantined to drafting `ActionProposal` representations.
   - Zero autonomous execution of state-mutating actions without an explicit, single-tap affirmative user confirmation card.

7. **Hardware Profiling & Mobile Lifecycle Management**:
   - App core bundle shipped at 32MB; quantized models downloaded on-demand over Wi-Fi with resume support.
   - `ModelLifecycleManager` unmaps GGUF weights on `onTrimMemory(TRIM_MEMORY_UI_HIDDEN)` within 250ms, eliminating background OOM kills.
   - Devices with < 4GB RAM or low-tier CPUs default to lightweight regex intent routing with cloud cascade.

## Consequences
- **Financial**: Preserves 98%+ gross margins with blended COGS $\le \$0.14/\text{user}/\text{month}$.
- **Performance**: 0ms local response for 95% of queries; sub-100ms TTFT for complex cloud queries; 0ms instant morning briefing display.
- **Privacy & Security**: Zero-knowledge compliance; zero raw audio egress; client-side PII redaction; GDPR Article 17 crypto-shredding compatibility.
- **Reliability**: Dual-provider cloud inference mesh with circuit breaker; 100% offline-functional deterministic core.
