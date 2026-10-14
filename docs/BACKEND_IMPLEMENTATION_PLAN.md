# YAAN Backend Engineering Implementation Plan
## Asymmetric Acceleration Hub & Zero-Knowledge State Relay (v1.0)

> **Authority**: `ADR-0002` (Hybrid On-Device & Cloud Backend, Acquitted 12/12) & `ADR-0003` (Ultra-Lean Cost Minimization)  
> **Lead Office**: `backend-office` & `architecture-office`  
> **Status**: APPROVED & IN IMPLEMENTATION  
> **Target Submodule**: `:backend` (Kotlin JVM 21 / Ktor 3.0 Server)  

---

## 1. Objectives & Architectural Invariants

The YAAN backend is an **Asymmetric Acceleration Hub**, strictly obeying the project's iron laws:
1. **Deterministic State Decoupled from Probabilistic AI**: Deterministic client operations (saving notes, triggering alarms, updating task checkboxes) must succeed 100% offline without backend availability.
2. **Zero-Knowledge Privacy**: Personal notes, tasks, and memory vectors are encrypted on-device (AES-GCM-256 + Argon2id). The backend operates as a blind relay storing ciphertext envelopes and Lamport vector clocks without ever holding private keys or plaintext data.
3. **Sub-150ms Interaction Ceiling**: Cloud inference offloads are streamed using HTTP/2 / HTTP/3 with Time-To-First-Token (TTFT) < 150ms.
4. **Pre-Computed Async Intelligence**: Morning briefings and schedule conflicts are pre-computed asynchronously and pushed to the local database for instant 0ms wake-up rendering.

---

## 2. Submodule Architecture (`:backend`)

```
backend/
├── build.gradle.kts
└── src/
    ├── main/
    │   ├── kotlin/com/yaan/backend/
    │   │   ├── Application.kt                     // Ktor Engine Server Entry Point
    │   │   ├── config/
    │   │   │   └── AppConfig.kt                   // Environment, Port, API Keys, Limits
    │   │   ├── plugins/
    │   │   │   ├── Routing.kt                     // Route aggregation
    │   │   │   ├── Serialization.kt               // Kotlinx JSON Content Negotiation
    │   │   │   ├── StatusPages.kt                 // Unified HTTP error handling
    │   │   │   ├── Cors.kt                        // Cross-Origin Resource Sharing
    │   │   │   └── RateLimiting.kt                // Token bucket rate limiting per API key/IP
    │   │   ├── sync/                              // Pillar: Blind E2EE CRDT State Relay
    │   │   │   ├── SyncModels.kt                  // EncryptedSyncEnvelope, VectorClock, SyncDelta
    │   │   │   ├── CrdtRelayService.kt            // In-memory / persistent blind delta queue
    │   │   │   └── SyncRoutes.kt                  // /api/v1/sync/push, /pull, /stream (SSE)
    │   │   ├── inference/                         // Pillar: Speculative Multi-Provider Inference Mesh
    │   │   │   ├── InferenceModels.kt             // PromptRequest, TokenChunk, ProviderMetrics
    │   │   │   ├── InferenceMeshService.kt        // Groq -> Fireworks -> Cerebras -> Local Fallback
    │   │   │   ├── CircuitBreaker.kt              // Sliding window latency & error circuit breaker
    │   │   │   └── InferenceRoutes.kt             // /api/v1/inference/stream, /providers
    │   │   ├── vault/                             // Pillar: Encrypted Cold Vault & Crypto-Shredding
    │   │   │   ├── VaultModels.kt                 // VaultBackupEnvelope, ShreddingCertificate
    │   │   │   ├── VaultBackupService.kt          // Encrypted blob retention & crypto-shredding
    │   │   │   └── VaultRoutes.kt                 // /api/v1/vault/backup, /restore, /purge
    │   │   └── briefing/                          // Pillar: Asynchronous Nightly Pre-Computation
    │   │       ├── BriefingModels.kt              // PrecomputedBriefingEnvelope, ConflictScan
    │   │       ├── BriefingWorkerService.kt       // Async scheduler & briefing synthesis generator
    │   │       └── BriefingRoutes.kt              // /api/v1/briefing/latest, /generate
    │   └── resources/
    │       └── application.conf                   // Ktor configuration
    └── test/
        └── kotlin/com/yaan/backend/
            ├── SyncRelayTest.kt                   // Validates blind delta fanout and vector clocks
            ├── InferenceMeshTest.kt               // Tests provider failover & circuit breaker
            ├── VaultBackupTest.kt                 // Tests encrypted backup and crypto-shredding
            └── BriefingServiceTest.kt             // Tests briefing generation and conflict scanning
```

---

## 3. Core API Contract Specifications

### 3.1 Blind E2EE CRDT State Relay (`/api/v1/sync`)
- **`POST /api/v1/sync/push`**: Ingests encrypted deltas (`EncryptedSyncEnvelope`) containing ciphertext, client device ID, and Lamport vector clock.
- **`GET /api/v1/sync/pull?since={lamport}`**: Pulls all encrypted deltas since a client epoch.
- **`GET /api/v1/sync/stream`**: Server-Sent Events (SSE) real-time channel broadcasting new deltas to active user devices.

### 3.2 Speculative Multi-Provider Inference Mesh (`/api/v1/inference`)
- **`POST /api/v1/inference/stream`**: Streaming chunked response with <150ms TTFT. Routes through Groq LPU primary, with automatic fallback to secondary inference providers if latency > 120ms or errors trigger the circuit breaker.
- **`GET /api/v1/inference/status`**: Health and latency metrics per provider.

### 3.3 Encrypted Cold-Vault Backup & Crypto-Shredding (`/api/v1/vault`)
- **`POST /api/v1/vault/backup`**: Uploads client-encrypted SQLite vault snapshot.
- **`GET /api/v1/vault/backup/latest`**: Downloads latest snapshot header and payload.
- **`POST /api/v1/vault/purge`**: Crypto-shreds all user backups and emits a verifiable SHA-256 erasure certificate.

### 3.4 Asynchronous Briefing & Conflict Worker (`/api/v1/briefing`)
- **`POST /api/v1/briefing/generate`**: Computes next morning's briefing payload asynchronously.
- **`GET /api/v1/briefing/latest`**: Returns pre-computed briefing payload for zero-latency wake-up.

---

## 4. Verification & Testing Strategy
- Unit tests for all routing endpoints via `io.ktor.server.testing.testApplication`.
- Full concurrent simulation of multi-device delta push/pull.
- Mock failover test verifying sub-150ms circuit breaker trip to secondary inference provider.
- Cryptographic erasure verification checking SHA-256 hash generation on purge.
