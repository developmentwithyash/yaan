# Contributing to YAAN

Thank you for your interest in contributing to **YAAN (Your Autonomous Action Navigator)**! We welcome contributions from developers, designers, and researchers committed to advancing sovereign, privacy-first local computing and AI systems.

---

## ✦ Code of Conduct

All contributors and maintainers are expected to adhere to our [Code of Conduct](CODE_OF_CONDUCT.md). Please report unacceptable behavior to `security@yaan.ai`.

---

## ✦ Core Architectural Invariants

Before writing code or opening a pull request, understand our non-negotiable system invariants:

1. **Deterministic State Decoupled from AI** — Creating notes, ticking tasks, and triggering alarms must succeed 100% offline with zero cloud dependency.
2. **Zero-Knowledge Privacy** — All vault content is encrypted with AES-GCM-256 on-device before any transmission. Cloud servers operate as blind relays.
3. **Sub-150ms Interaction Ceiling** — User interactions must render immediately with 0ms baseline.
4. **Two-Phase Action Execution** — The AI assistant may propose actions, but nothing modifies user state without affirmative user confirmation.

---

## ✦ Development Environment & Setup

### Prerequisites
* **JDK 17 or JDK 21** (Temurin or Corretto recommended)
* **Android Studio Ladybug (2024.2+)** or **IntelliJ IDEA Ultimate**
* **Xcode 16+** (for macOS/iOS targets)
* **Kotlin 2.0+**
* **Git**

### Clone and Verify Build
```bash
git clone https://github.com/developmentwithyash/yaan.git
cd yaan

# Run local multiplatform unit tests
./gradlew test

# Run Android lint
./gradlew lint
```

---

## ✦ Pull Request Workflow

We follow standard GitHub flow with Conventional Commits:

1. **Fork the repository** and create a feature branch from `main`:
   ```bash
   git checkout -b feat/your-feature-name
   ```
2. **Strict Test-Driven Development (TDD)**:
   * Write failing tests (Red) covering your expected behavior before writing production code.
   * Implement minimal, clean code to pass the tests (Green).
   * Refactor cleanly while maintaining test green status.
3. **Run Verification Before Submission**:
   ```bash
   ./gradlew check
   ```
4. **Commit Conventions**:
   Use structured commit messages:
   * `feat(notes): add Markdown AST exporter`
   * `fix(parser): handle ambiguous timezone in relative dates`
   * `docs(readme): update quickstart steps for iOS build`
   * `perf(vault): optimize SQLDelight FTS5 token index`
5. **Open a Pull Request**:
   * Fill out the structured PR template.
   * Reference any related open issue (e.g. `Fixes #42`).
   * Include automated test output or screenshot evidence.

---

## ✦ Reporting Security Issues

Please **do not** open public GitHub issues for security vulnerabilities. Instead, refer to our [Security Policy](SECURITY.md) and report vulnerabilities confidentially to `security@yaan.ai`.
