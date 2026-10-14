# YAAN — Kotlin Multiplatform Project Scaffold & UI Shell

> **Your Autonomous Action Navigator**  
> *Remember less. Do more. Live better.*

## Architecture Overview

YAAN is organized as a modular Kotlin Multiplatform project using Compose Multiplatform for shared UI across Android, iOS, and Desktop.

```
yaan/
├── composeApp/            # App shell hosting Compose Multiplatform UI & Design System
│   └── src/
│       ├── androidMain/   # Android MainActivity, Application & manifest
│       ├── iosMain/       # iOS UI entry point
│       ├── desktopMain/   # Desktop target entry
│       └── commonMain/    # Shared Compose screens, navigation & design tokens
│           └── kotlin/com/yaan/app/
│               ├── App.kt                      # Shell host with animated destination switching
│               ├── navigation/                 # YaanDestination navigation state machine
│               ├── ui/
│               │   ├── theme/                  # Executive Kinetic Minimalism tokens (Color, Type, Shape, Theme)
│               │   ├── components/             # YaanTopBar, BottomNav, YaanCard, YaanButton, YaanChip, QuickCapture
│               │   └── screens/                # Home, Plan, Ask YAAN, Live Voice Modal, Notes, You
├── shared/                # KMP platform bridge (expect/actual declarations)
├── core/
│   ├── common/            # Dispatchers, Result wrappers, common extensions
│   ├── domain/            # Entities, use cases, repository interfaces (zero platform deps)
│   └── data/              # SQLDelight database, DataStore, Ktor network clients
├── feature/               # Vertical feature modules
│   ├── capture/           # Instant capture & auto-triage inbox
│   ├── reminders/         # Defense-in-depth notification & alarm stack
│   ├── notes/             # Searchable knowledge base & OCR
│   ├── tasks/             # Task trees, routines & auto-closing loops
│   ├── briefing/          # Morning brief & proactive assistance
│   ├── memory/            # Epistemic memory store with TTLs & citation tracking
│   ├── voice/             # On-device voice input & Edge SLM parsing
│   └── actions/           # External tool coordination & approval gates
├── iosApp/                # Native iOS Xcode wrapper & Podfile
└── gradle/                # Gradle wrapper and version catalog (libs.versions.toml)
```

## Stitch Design System Integration

The mobile experience faithfully implements the **Executive Kinetic Minimalism** design system fetched from Stitch MCP project `829891685103373380`:

- **Design System Spec**: [`docs/design/STITCH_DESIGN_SYSTEM.md`](docs/design/STITCH_DESIGN_SYSTEM.md)
- **Downloaded HTML & Mockups**: [`docs/stitch_designs/`](docs/stitch_designs/)
- **Implementation Plan**: [`docs/STITCH_DESIGN_IMPLEMENTATION_PLAN.md`](docs/STITCH_DESIGN_IMPLEMENTATION_PLAN.md)

### Implemented UI Surfaces:
1. **Home Command Center** (`HomeScreen.kt`): Executive greeting, dismissible Daily Briefing card, explainable conflict detection banner with 1-tap reschedule, next commitment card, interactive priorities checklist, and quick capture dock.
2. **Plan & Reminders** (`PlanScreen.kt`): Timeline agenda, recurring weekly commitments, geofence alarm cards, and active streams project progress accordion.
3. **Ask YAAN Assistant** (`AskYaanScreen.kt`): Transparent "Thinking Process" disclosure, executive dispatch cards, grounded vault citations carousel, proposed executive actions with affirmative confirmation, and floating prompt composer.
4. **Live Voice Mode** (`LiveVoiceModal.kt`): Multi-ring pulsating voice orb, multi-bar waveform visualizer, natural duplex conversation state indicator, and playback speed controls.
5. **Notes & Personal Knowledge** (`NotesScreen.kt`): Semantic vault search, horizontal filter tags, semantic query banner, audio memo player with scrubber waveform, and pinned encrypted vault notes.
6. **You / Privacy & Control** (`YouScreen.kt`): Notification engine health, battery/wakelock telemetry, visual cadence switcher (Warm Golden vs Dark Obsidian), and AES-GCM-256 vault export/purge.

## Tech Stack

- **Language**: Kotlin 2.0.21
- **UI Framework**: Compose Multiplatform 1.7.0 (Material 3)
- **Database**: SQLDelight 2.0.2 (Local SQLite persistence)
- **Networking**: Ktor 3.0.1
- **Dependency Injection**: Koin 4.0.0
- **Coroutines & DateTime**: Kotlinx Coroutines 1.9.0, Kotlinx DateTime 0.6.1
- **Build System**: Gradle 8.9 with Type-Safe Project Accessors and Version Catalog

## Documentation & Execution Tracking

- **Institutional Progress Tracker**: [`PROGRESS_TRACKER.md`](PROGRESS_TRACKER.md) *(Continuously updated execution registry)*
- **Jury Decision Ledger & Audit Registry**: [`docs/JURY_DECISION_LEDGER.md`](docs/JURY_DECISION_LEDGER.md)
- **Product Vision (Jury-Hardened)**: [`YAAN_PRODUCT_VISION_v2.md`](YAAN_PRODUCT_VISION_v2.md)
- **AI Backend Architecture Plan (v2.0 Gold Master)**: [`docs/YAAN_AI_BACKEND_ARCHITECTURE_PLAN.md`](docs/YAAN_AI_BACKEND_ARCHITECTURE_PLAN.md)
- **Minimum-Cost Launch Plan**: [`docs/YAAN_MINIMUM_COST_LAUNCH_PLAN.md`](docs/YAAN_MINIMUM_COST_LAUNCH_PLAN.md)
- **10,000-User Multi-Cohort Simulation**: [`docs/YAAN_10000_USER_SIMULATION.md`](docs/YAAN_10000_USER_SIMULATION.md)
- **Architecture Decision Records**: [`docs/adr/`](docs/adr/)
- **Stitch Design System Spec**: [`docs/design/STITCH_DESIGN_SYSTEM.md`](docs/design/STITCH_DESIGN_SYSTEM.md)
