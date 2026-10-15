# YAAN — Your Autonomous Action Navigator

> *Remember less. Do more. Live better.*

**YAAN** is a privacy-first, AI-powered personal operating system for high-output professionals. It consolidates tasks, notes, reminders, voice memos, and proactive daily briefings into a single sovereign local-first application — with optional cloud-assisted intelligence that never sees your data in plaintext.

Built with **Kotlin Multiplatform** and **Compose Multiplatform**, YAAN runs natively on Android and iOS from a single shared codebase with zero-knowledge encryption at rest and hardware-backed key management.

---

## ✦ What YAAN Does

| Capability | How It Works |
|---|---|
| **Instant Capture** | Tap, type, or speak — thoughts are triaged into tasks, notes, or reminders in < 150ms using on-device NLP intent detection. |
| **Encrypted Notes Vault** | Every note is encrypted with AES-GCM-256 before touching disk. Search, tag, pin, and export your knowledge base. |
| **Smart Reminders** | Natural language scheduling with typo tolerance ("tommoroww 2pm"), relative dates ("tonight"), and auto-rescheduling. |
| **Ask YAAN Assistant** | Grounded Q&A over your encrypted vault — YAAN answers from *your* data with citations, not hallucinated guesses. |
| **Live Voice Mode** | Push-to-talk with real-time speech synthesis in 3 voice profiles (British, American, Australian) and speed control. |
| **Daily Briefing** | Pre-computed overnight, rendered at 0ms on wake-up: today's commitments, conflicts, and cognitive load score. |
| **Two-Phase Actions** | YAAN proposes actions (reschedule, draft, create) — nothing executes without your explicit tap. Undo is always available. |
| **Privacy by Architecture** | Deterministic operations work 100% offline. Cloud is opt-in, consented, and zero-knowledge. |

---

## ✦ Architecture

YAAN follows a clean modular architecture with strict separation between deterministic on-device operations and probabilistic cloud intelligence:

```
yaan/
├── composeApp/                   # Compose Multiplatform UI + AI Engine
│   └── src/
│       ├── commonMain/           # Shared screens, theme, components, AI engine
│       ├── androidMain/          # Android Activity & manifest
│       └── iosMain/              # iOS MainViewController bridge
├── core/
│   ├── common/                   # Dispatchers, Result<T>, datetime, logging
│   ├── domain/                   # Entities, use cases, repository interfaces (zero deps)
│   └── data/                     # SQLDelight DB, AES-GCM cipher, CRDT, Ktor
├── feature/
│   ├── capture/                  # Instant capture & auto-triage inbox
│   ├── reminders/                # Defense-in-depth alarm stack
│   ├── notes/                    # Encrypted vault CRUD & search
│   ├── tasks/                    # Task trees & auto-closing loops
│   ├── briefing/                 # Morning briefing & proactive assistance
│   ├── memory/                   # Epistemic memory with confidence scoring
│   ├── voice/                    # On-device voice & Edge SLM parsing
│   └── actions/                  # External tool connectors & approval gates
├── backend/                      # Ktor server: CRDT relay, inference mesh, vault backup
├── shared/                       # KMP platform bridge (expect/actual)
├── iosApp/                       # Native iOS Xcode wrapper
└── gradle/                       # Version catalog & wrapper
```

### Architectural Invariants (Non-Negotiable)

1. **Deterministic State Decoupled from AI** — Saving notes, toggling tasks, scheduling reminders always succeed 100% offline without LLM availability.
2. **Zero-Knowledge Privacy** — All vault content is AES-GCM-256 encrypted on-device before any cloud transmission. The server is a blind relay.
3. **Sub-150ms Interaction Ceiling** — On-device processing at 0ms baseline; cloud-assisted queries stream with TTFT < 150ms.
4. **Pre-computed Async Intelligence** — Daily briefings are synthesized overnight and delivered as pre-computed payloads for 0ms wake-up rendering.

---

## ✦ Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin 2.0.21 |
| **UI Framework** | Compose Multiplatform 1.7.0 (Material 3) |
| **Database** | SQLDelight 2.0.2 (Local SQLite, reactive Flow observers) |
| **Encryption** | AES-GCM-256 with hardware-backed key management (Android Keystore / iOS Secure Enclave sandbox) |
| **Networking** | Ktor 3.0.1 (HTTP/3 QUIC, 0-RTT connection pooling) |
| **DI** | Koin 4.0.0 |
| **Serialization** | Kotlinx Serialization 1.7.3 (JSON + Protobuf) |
| **Async** | Kotlinx Coroutines 1.9.0, Kotlinx DateTime 0.6.1 |
| **CRDT** | Multi-Value Register with Lamport timestamps & vector clocks |
| **Backend** | Ktor Server 3.0.1 on Netty (JVM 21) |
| **Build** | Gradle 8.9 with Version Catalog & Type-Safe Project Accessors |
| **Design System** | Executive Kinetic Minimalism (Stitch MCP) |

---

## ✦ Design System: Executive Kinetic Minimalism

The UI implements a bespoke **Executive Kinetic Minimalism** design language built with [Stitch MCP](docs/design/STITCH_DESIGN_SYSTEM.md):

- **Light Mode (Warm Golden Editorial)**: `#FDF9F3` surface, `#785600` primary, `#DAD4CA` borders — warm, quiet, editorial.
- **Dark Mode (Dark Obsidian)**: `#141311` surface, `#F7BD48` gold accents, `#3A362F` containers — deep, premium, focused.
- **Typography**: Plus Jakarta Sans editorial font stack with calibrated weight/size hierarchy.
- **Motion**: Spring-based micro-animations, pulse orbs, and kinetic transitions.
- **Components**: `YaanCard`, `YaanButton`, `YaanChip`, `YaanTopBar`, `YaanBottomNavigation`, `ActionProposalCard`, branded canvas-drawn monogram.

---

## ✦ Implemented Screens

| Screen | Description |
|---|---|
| **Brand Splash** | Centered architectural monogram with radial aura glow and Dark ↔ Light theme switch |
| **Home Command Center** | Executive greeting, Daily Briefing card, conflict detection with 1-tap reschedule, priorities checklist, quick capture dock |
| **Plan & Reminders** | Schedule timeline, recurring commitments, geofence alarm cards, project progress accordion |
| **Ask YAAN** | Transparent reasoning disclosure, vault-grounded Q&A with citations, action proposals, floating prompt bar |
| **Live Voice Mode** | Multi-ring pulsating orb, waveform visualizer, duplex state indicator, 3 voice accents, speed control |
| **Notes & Vault** | Semantic search, filter chips, audio memo player with scrubber waveform, pinned encrypted vault notes |
| **You / Privacy** | Notification engine health, battery telemetry, biometric vault lock, AES-GCM vault export/purge, Passkey registration |
| **Onboarding** | DPDP Act 2023 age declaration, permissions rationale, privacy-first first-run experience |

---

## ✦ Security Posture

- **AES-GCM-256** authenticated encryption at rest for all vault notes (hardware-backed keys on Android Keystore / iOS sandbox).
- **Biometric vault lock** gate with Touch ID / Face ID before vault access.
- **Passkey** Secure Enclave registration for cloud escrow.
- **Zero plaintext logging** — no vault content ever appears in logs, crash reports, or network traces.
- **Prompt injection defense** — `VaultUntrustedContentGuard` with 35 red-team injection tests.
- **BOLA defense** — AuthGuard enforced across all backend endpoints.
- **ADB backup disabled**, TLS enforced in AndroidManifest.
- **GDPR Article 17** — atomic crypto-shredding with SHA-256 verifiable certificates.

---

## ✦ AI Engine

YAAN's on-device AI engine (`YaanAssistantEngine`) runs entirely locally with zero cloud dependency for core operations:

- **Intent Detection**: Automatic classification of user input into tasks, notes, reminders, vault searches, or briefing queries using rule-based NLP with typo tolerance and temporal marker parsing.
- **Vault-Grounded Q&A**: Searches encrypted local notes and returns answers with citations — never generates information that isn't in your vault.
- **Two-Phase Action Commitment**: All proposed actions (reminders, tasks, reschedules) require explicit user confirmation. Every action is undoable.
- **Cloud-Assisted Intelligence** (opt-in): When consented, routes to Groq LPU (< 100ms TTFT) with Fireworks AI / Cerebras fallback via thread-safe circuit breaker. PII is redacted client-side before transmission.

---

## ✦ Backend Services

The backend (`/backend`) is a Ktor 3.0.1 server on Netty implementing 4 zero-knowledge microservices:

1. **Blind CRDT State Relay** — Multi-tenant delta sync with Lamport ordering, SHA-256 deduplication, and epoch compaction. Server never decrypts payloads.
2. **Dynamic Inference Mesh** — Primary Groq LPU router with circuit breaker failover to Fireworks AI / Cerebras. Streaming responses with < 100ms TTFT.
3. **Cold Vault Backup** — Zero-knowledge encrypted snapshot escrow with GDPR Article 17 crypto-shredding.
4. **Nightly Briefing Worker** — Asynchronous batch briefing synthesis with schedule conflict analysis and cognitive load scoring.

---

## ✦ Project Stats

| Metric | Value |
|---|---|
| **Kotlin Source Files** | 192 |
| **Lines of Kotlin** | ~20,000 |
| **Gradle Modules** | 16 (3 core + 8 feature + composeApp + shared + backend + iOS + root) |
| **Test Files** | 30 |
| **Passing Tests** | 82+ |
| **ADRs** | 7 (all jury-acquitted) |
| **Jury Dockets** | 7 tried, 7 acquitted (12-juror adversarial trial) |
| **Documentation** | ~5,400 lines across 25+ markdown documents |
| **Design System Tokens** | Color, Typography, Shape, Spacing, Elevation, Motion |
| **Platforms** | Android + iOS (verified on physical iPhone 15) |

---

## ✦ Governance

Every architectural, product, and commercial decision passes through a **12-Juror Adversarial Trial** before adoption. Each decision is presumed GUILTY until stress-tested across 12 institutional dimensions (Seed Investor, ICP Customer, Staff Engineer, Product Designer, CFO, Legal, Competitor, QA, SRE, etc.).

- **Decision Ledger**: [`docs/JURY_DECISION_LEDGER.md`](docs/JURY_DECISION_LEDGER.md)
- **Progress Tracker**: [`PROGRESS_TRACKER.md`](PROGRESS_TRACKER.md)
- **Product Vision**: [`YAAN_PRODUCT_VISION_v2.md`](YAAN_PRODUCT_VISION_v2.md)

---

## ✦ Documentation Index

| Document | Purpose |
|---|---|
| [`PROGRESS_TRACKER.md`](PROGRESS_TRACKER.md) | Canonical execution registry & task audit log |
| [`YAAN_PRODUCT_VISION_v2.md`](YAAN_PRODUCT_VISION_v2.md) | Jury-hardened product vision & positioning |
| [`docs/YAAN_AI_BACKEND_ARCHITECTURE_PLAN.md`](docs/YAAN_AI_BACKEND_ARCHITECTURE_PLAN.md) | Gold Master AI backend architecture |
| [`docs/YAAN_MINIMUM_COST_LAUNCH_PLAN.md`](docs/YAAN_MINIMUM_COST_LAUNCH_PLAN.md) | Bootstrap launch strategy ($22.50/mo burn) |
| [`docs/YAAN_10000_USER_SIMULATION.md`](docs/YAAN_10000_USER_SIMULATION.md) | 10,000-user Monte Carlo stress simulation |
| [`docs/JURY_DECISION_LEDGER.md`](docs/JURY_DECISION_LEDGER.md) | All jury trial records & verdicts |
| [`docs/design/STITCH_DESIGN_SYSTEM.md`](docs/design/STITCH_DESIGN_SYSTEM.md) | Executive Kinetic Minimalism design spec |
| [`CLAIMS_REGISTER.md`](CLAIMS_REGISTER.md) | Architectural claims audit & verification |
| [`COST_MODEL.md`](COST_MODEL.md) | Unit economics & gross margin model |
| [`THREAT_MODEL.md`](THREAT_MODEL.md) | STRIDE threat analysis & data flow security |
| [`RELEASE_READINESS.md`](RELEASE_READINESS.md) | Staged rollout, kill switch, device budgets |
| [`EVAL_REPORT.md`](EVAL_REPORT.md) | AI evaluation harness & baseline metrics |
| [`docs/adr/`](docs/adr/) | 7 Architecture Decision Records |

---

## ✦ Getting Started

```bash
# Clone and build
git clone <repo-url>
cd yaan

# Android
./gradlew :composeApp:assembleDebug
# APK at composeApp/build/outputs/apk/debug/

# iOS
open iosApp/iosApp.xcodeproj
# Select target device → Build & Run (⌘R)

# Backend
./gradlew :backend:run

# Tests
./gradlew test
```

---

## ✦ License

Proprietary. All rights reserved.  
© 2026 Yash Engineering Systems.
