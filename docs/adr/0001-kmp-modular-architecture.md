# ADR 0001: Kotlin Multiplatform Modular Architecture for YAAN

## Status
Accepted

## Context
YAAN is designed as a personal AI operating layer for everyday life. Following the 12-juror adversarial trial and appellate retrial certification, YAAN requires:
1. **Cross-platform capability**: Supporting Android, iOS, and desktop without duplicating business and domain logic.
2. **Strict separation of concerns**: Isolating deterministic storage and scheduling from probabilistic AI interpretation.
3. **Modular boundary enforcement**: Independent feature modules to allow selective loading, rapid build times, and zero coupling.

## Decision
We adopted a multi-module Kotlin Multiplatform (KMP 2.0) project architecture:

- **App Shell**: `:composeApp` (Compose Multiplatform UI host for Android, iOS, Desktop)
- **Shared Bridge**: `:shared` (Platform expect/actual bindings)
- **Core Layer**:
  - `:core:common`: Cross-cutting utilities, concurrency dispatchers, result monads
  - `:core:domain`: Pure Kotlin business logic, domain entities, repository interfaces
  - `:core:data`: Local SQLite (SQLDelight) database, DataStore, Ktor network clients
- **Feature Modules** (Independent vertical slices):
  - `:feature:capture`: Zero-friction capture & auto-triage
  - `:feature:reminders`: 5-layer notification & alarm management
  - `:feature:notes`: Personal knowledge base, semantic search & OCR
  - `:feature:tasks`: Task lifecycle, project trees & auto-closing loops
  - `:feature:briefing`: Daily/weekly executive digests & proactive assistance
  - `:feature:memory`: Epistemic memory store with TTLs & citation backlinks
  - `:feature:voice`: Voice-first capture & Edge SLM parsing
  - `:feature:actions`: Action engine, external tool integrations & audit log

## Consequences
- Clean architecture guaranteed across all mobile platforms.
- Deterministic data operations remain intact even if network or AI models are offline.
- Features can be developed and tested in isolation.
