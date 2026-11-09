# 🤝 Contributing to YAAN

Thank you for your interest in contributing to **YAAN (Your Autonomous Action Navigator)**! We welcome contributions from developers, researchers, and designers committed to advancing private, sovereign, local-first computing and edge AI systems.

---

## 🧭 Table of Contents

- [Code of Conduct](#-code-of-conduct)
- [System Architecture & Invariants](#-system-architecture--invariants)
- [Repository Structure](#-repository-structure)
- [Development Setup (Step-by-Step)](#-development-setup-step-by-step)
  - [Prerequisites](#1-prerequisites)
  - [IDE & Recommended Plugins](#2-ide--recommended-plugins)
  - [Cloning and Building](#3-cloning-and-building)
- [Running the Applications](#-running-the-applications)
  - [Compose Desktop (JVM)](#1-compose-desktop-jvm)
  - [Android (Emulator / Device)](#2-android-emulator--physical-device)
  - [Backend CRDT Sync Relay](#3-backend-crdt-sync-relay)
- [Testing & Quality Verification](#-testing--quality-verification)
- [How to Pick or Claim an Issue](#-how-to-pick-or-claim-an-issue)
- [Pull Request Workflow](#-pull-request-workflow)
- [Security Vulnerability Reporting](#-security-vulnerability-reporting)

---

## ✦ Code of Conduct

All contributors and maintainers are expected to uphold our [Code of Conduct](CODE_OF_CONDUCT.md). Please report unacceptable behavior to `work.developmentwithyash@gmail.com`.

---

## ✦ System Architecture & Invariants

Before writing code or opening a pull request, understand our four non-negotiable system invariants:

1. **Deterministic State Decoupled from AI** — Creating notes, ticking tasks, and scheduling alarms must succeed 100% offline with zero cloud dependency. AI enhances productivity; it is never in the critical path of raw data persistence.
2. **Zero-Knowledge Privacy** — All vault content (notes, memory vectors, tasks) is encrypted with hardware-backed AES-GCM-256 on-device before hitting disk or any sync relay. Cloud servers act as blind relays.
3. **Sub-150ms Interaction Ceiling** — User interactions must render immediately with 0ms perceived baseline latency.
4. **Two-Phase Action Safety** — AI engines may *propose* state changes, but nothing modifies user state without explicit affirmative user confirmation.

```
┌────────────────────────────────────────────────────────┐
│                   composeApp UI                        │
│   (Android App • Desktop JVM App • iOS Shell)         │
└───────────┬────────────────────────────────┬───────────┘
            │                                │
┌───────────▼───────────┐        ┌───────────▼───────────┐
│     feature:*         │        │      core:domain      │
│ capture, notes, tasks │        │ models, use-cases, AI │
│ briefing, voice, etc. │        │ safety engine         │
└───────────┬───────────┘        └───────────┬───────────┘
            │                                │
┌───────────▼────────────────────────────────▼───────────┐
│                      core:data                         │
│   SQLite / SQLDelight  •  AES-GCM Vault Cipher        │
└───────────────────────┬────────────────────────────────┘
                        │ (Optional E2EE Relay)
┌───────────────────────▼────────────────────────────────┐
│                       :backend                         │
│         Ktor Asynchronous CRDT Sync Relay              │
└────────────────────────────────────────────────────────┘
```

---

## ✦ Repository Structure

| Module | Responsibility |
|---|---|
| `:composeApp` | Multiplatform app shell, Compose UI screens, platform entrypoints (`androidMain`, `jvmMain`, `iosMain`). |
| `:core:common` | Shared dispatchers, result types (`YaanResult`), and base extensions. |
| `:core:domain` | Pure Kotlin business models, repository interfaces, and `ActionSafetyEngine`. |
| `:core:data` | SQLDelight database driver factories, repositories, and AES-GCM vault encryption. |
| `:feature:capture` | Quick capture engine and modal interfaces. |
| `:feature:notes` | Encrypted notes vault and rich text models. |
| `:feature:tasks` | Task execution, checklists, and scheduling. |
| `:feature:reminders` | High-priority alarms and system notifications. |
| `:feature:briefing` | Daily morning and evening executive intelligence briefings. |
| `:feature:memory` | Vector memory embeddings and semantic search. |
| `:feature:voice` | Voice transcription contracts and audio evaluation. |
| `:feature:actions` | Two-phase action proposal engine and execution safeguards. |
| `:backend` | Ktor microservice providing E2EE CRDT sync relay and backup endpoints. |

---

## ✦ Development Setup (Step-by-Step)

### 1. Prerequisites

* **JDK 21** (Required: Eclipse Temurin 21 recommended)
  * macOS (Homebrew): `brew install --cask temurin@21`
  * Linux / Windows: via [adoptium.net](https://adoptium.net) or [SDKMAN!](https://sdkman.io): `sdk install java 21.0.5-tem`
  * Verify in terminal: `java -version` (must report `openjdk version "21.x"`)
* **Android Studio Ladybug (2024.2+)** or **IntelliJ IDEA Ultimate / Community (2024.2+)**
* **Git** installed and configured

### 2. IDE & Recommended Plugins

When opening YAAN in IntelliJ IDEA or Android Studio, ensure these plugins are installed:
* **Kotlin Multiplatform Mobile (KMM)**
* **Compose Multiplatform IDE Support**
* **SQLDelight** (for `.sq` and `.sqm` syntax highlighting and code generation)

**IDE Settings Configuration:**
1. Open Project in IDE: Select the root `yaan/` directory.
2. In **Settings / Preferences $\rightarrow$ Build, Execution, Deployment $\rightarrow$ Build Tools $\rightarrow$ Gradle**:
   * Set **Gradle JVM** to **JDK 21**.

### 3. Cloning and Building

```bash
# Clone the repository
git clone https://github.com/developmentwithyash/yaan.git
cd yaan

# Validate the Gradle wrapper
./gradlew wrapper --gradle-version 8.9

# Run multiplatform build checks
./gradlew check
```

---

## ✦ Running the Applications

### 1. Compose Desktop (JVM)

You can launch and test the full desktop application immediately without an emulator:

```bash
./gradlew :composeApp:run
```

**Desktop Keyboard Shortcuts (Issue #21):**
* `Cmd / Ctrl + K`: Trigger Quick Capture
* `Cmd / Ctrl + N`: Jump to Notes Vault
* `Cmd / Ctrl + 1-4`: Switch navigation tabs (Home, Plan, Notes, You)
* `Cmd / Ctrl + T`: Toggle Dark/Light theme mode
* `Esc`: Dismiss active dialogs and modals

### 2. Android (Emulator / Physical Device)

1. Launch an Android Virtual Device (AVD) running Android 10+ (API 29+).
2. Install and launch the debug build:

```bash
./gradlew :composeApp:installDebug
```

### 3. Backend CRDT Sync Relay

To run the optional self-hosted Ktor sync relay:

```bash
# Direct Gradle execution
./gradlew :backend:run

# Or launch containerized with Docker Compose
docker compose up -d
```

---

## ✦ Testing & Quality Verification

YAAN enforces strict test coverage across all modules. Always verify that test suites pass before submitting changes:

```bash
# Run all unit tests across all multiplatform modules
./gradlew test

# Run tests for a specific module
./gradlew :composeApp:testDebugUnitTest
./gradlew :core:data:test
./gradlew :backend:test
```

---

## ✦ How to Pick or Claim an Issue

1. Browse our open issues at [GitHub Issues](https://github.com/developmentwithyash/yaan/issues).
2. If you are new to the codebase, filter by label:
   * **`good first issue`**: Self-contained, approachable starter tasks.
   * **`help wanted`**: Architectural enhancements and platform integrations.
3. Comment on the issue you wish to claim: *"I'd like to work on this!"*
4. A maintainer will assign the issue to you.

---

## ✦ Pull Request Workflow

We follow standard GitHub flow with Conventional Commits and branch protection rules:

1. **Create an isolated feature branch:**
   ```bash
   git checkout -b feat/your-feature-name
   ```
2. **Follow Test-Driven Development (TDD):**
   * Write failing tests (Red) asserting the new capability or bugfix.
   * Write minimal production code to satisfy the tests (Green).
   * Refactor cleanly while maintaining test green status.
3. **Commit Conventions:**
   We follow [Conventional Commits](https://www.conventionalcommits.org/):
   * `feat(desktop): add global shortcut for notes`
   * `fix(vault): prevent race condition during background sync`
   * `docs(readme): add troubleshooting note for JDK 21`
   * `perf(db): add index on notes updatedAtEpochMs`
4. **Push and Open a Pull Request:**
   * Reference the related issue in your PR description: `Closes #42`.
   * Ensure GitHub Actions CI verification completes with all checks green.
   * Maintainers review all PRs promptly (< 12 hours turnaround).

---

## ✦ Security Vulnerability Reporting

Please **do not** open public GitHub issues for security vulnerabilities. Instead, refer to our [Security Policy](SECURITY.md) and report vulnerabilities confidentially to `work.developmentwithyash@gmail.com`.
