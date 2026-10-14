# YAAN Design System: Executive Kinetic Minimalism
**Imported from Stitch MCP Project `829891685103373380`**

---

## 1. Aesthetic Foundation & Philosophy

The YAAN design system embodies **Executive Kinetic Minimalism**: calm editorial authority merged with precision-engineered minimalism. Designed for high-output knowledge workers, entrepreneurs, and executives, the aesthetic eliminates cognitive drag:

- **Warm Editorial Grounding**: A soft, unbleached paper/parchment ground (`#FDF9F3` / `#FAF9F5`) reduces digital fatigue compared to stark pure white.
- **Obsidian Black Typography**: Near-black charcoal typography (`#141311`) provides sharp, commanding legibility.
- **Authoritative Bronze-Gold & Iris Accents**: Measured accents (`#785600`, `#966F14`, `#D4AF37`) replace neon distractions, signaling calculated executive prioritization.
- **Hairline Structural Boundaries**: Crisp micro-borders (`#DAD4CA` / `#E8E4DC`) provide tactile, architectural definition with minimal drop shadows.

---

## 2. Core Color Tokens

### Light Theme (Default)
| Role | Hex | Compose Token | Usage |
| :--- | :--- | :--- | :--- |
| **Canvas / Base** | `#FDF9F3` | `SurfaceLight` | Warm unbleached paper backdrop |
| **Card Surface** | `#FFFFFF` | `SurfaceContainerLowestLight` | Focal cards, briefing sheets, modals |
| **Inset Surface** | `#F7F3EE` | `SurfaceContainerLowLight` | Task items, inset containers |
| **Neutral Container** | `#F2EDE8` | `SurfaceContainerLight` | Passive toolbars, segmented chips |
| **Sand Surface** | `#F5F2EB` | `SandSurfaceLight` | Audio scrubber cards, attendee blocks |
| **Champagne Tint** | `#FEF9E7` | `ChampagneTintLight` | AI reasoning, conflict warning tints |
| **Primary Accent** | `#785600` | `PrimaryLight` | High-value actions, focal primary buttons |
| **Bronze Accent** | `#966F14` | `BronzeAccentLight` | Micro-indicators, tag icons |
| **Amber Gold** | `#D4AF37` | `AmberGoldLight` | High-priority idea tags |
| **Delicate Border** | `#DAD4CA` | `DelicateBorderLight` | Hairline structural card outlines |
| **Text Primary** | `#141311` | `OnSurfaceLight` | Obsidian black headings and body |
| **Text Secondary** | `#4A4845` | `OnSurfaceVariantLight` | Dark graphite metadata & timestamps |

### Dark Theme (Obsidian Vault)
| Role | Hex | Compose Token | Usage |
| :--- | :--- | :--- | :--- |
| **Canvas / Base** | `#141311` | `SurfaceDark` | Deep obsidian backdrop |
| **Card Surface** | `#1B1A17` | `SurfaceContainerLowestDark` | Elevated card surfaces |
| **Inset Surface** | `#22201D` | `SurfaceContainerLowDark` | Inset blocks |
| **Delicate Border** | `#3A362F` | `DelicateBorderDark` | Low-key crisp border definitions |
| **Primary Accent** | `#F7BD48` | `PrimaryDark` | Luminous amber-gold focal actions |
| **Text Primary** | `#F4F0EB` | `OnSurfaceDark` | High-clarity off-white typography |

---

## 3. Typographic Hierarchy (Plus Jakarta Sans)

| Style | Size | Line Height | Weight | Tracking | Usage |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `DisplayLg` | 40sp | 48sp | Bold (700) | -0.03em | Primary display statements |
| `DisplayMobile` | 32sp | 40sp | Bold (700) | -0.025em | Mobile hero headers |
| `HeadlineLg` | 28sp | 36sp | SemiBold (600) | -0.02em | Major section headers |
| `HeadlineMd` | 22sp | 30sp | SemiBold (600) | -0.015em | Screen greetings, prominent cards |
| `HeadlineSm` | 18sp | 26sp | SemiBold (600) | -0.01em | Card titles, modal headers |
| `BodyLarge` | 16sp | 26sp | Normal (400) | -0.005em | Longform synthesized briefs |
| `BodyMedium` | 14sp | 22sp | Normal (400) | 0.0em | Core paragraph text & inputs |
| `BodySmall` | 13sp | 18sp | Normal (400) | 0.0em | Secondary descriptions, subtext |
| `LabelLarge` | 14sp | 20sp | SemiBold (600) | 0.01em | Primary button labels |
| `LabelMedium` | 12sp | 16sp | SemiBold (600) | 0.02em | Filter chips, table column tags |
| `LabelSmall` | 11sp | 14sp | Medium (500) | 0.04em | Status pills, uppercase badges |

---

## 4. Implemented Screens from Stitch

1. **Home Command Center** (`HomeScreen.kt`):
   - Executive date & time stamp with live pulsating "Vault Sync Active" beacon
   - Dismissible Daily Briefing card with commitment counter & cognitive load telemetry
   - Calendar Conflict Detection card with instant 1-tap reschedule ("Adjust 1:15 PM")
   - Next Commitment card with Google Meet integration & attendee headshots
   - Interactive Today's Priorities checklist with inline completion strike-through
   - Inline Quick Capture dock with voice capture and quick tags

2. **Plan & Reminders** (`PlanScreen.kt`):
   - Notification Defense Stack status card (5 primed alarms, foreground scheduler)
   - Later Today scheduled commitments (recurring weekly, PR review, geofenced pharmacy alarm)
   - Active Streams project accordion card with 60% progress indicator

3. **Ask YAAN & Intelligent Assistant** (`AskYaanScreen.kt`):
   - Chat vs Live Voice mode selector
   - Transparent, expandable "Thinking Process" disclosure (vector store search, calendar checks)
   - Executive Dispatch card with Q4 Strategic Milestones
   - Grounded Citations carousel linking back to Voice Memos & Notes
   - Proposed Executive Action card with "Schedule & Send" affirmation
   - Signature floating pill prompt composer with Live Voice trigger

4. **Ask YAAN — Live Voice Mode** (`LiveVoiceModal.kt`):
   - Multi-ring pulsating voice orb with organic multi-bar animated waveform
   - Natural duplex duplex state indicators ("Speaking calmly" / "Listening naturally")
   - Speed controls (`1.0x`, `1.2x`, `1.5x`), mic mute toggle, push-to-talk simulation

5. **Notes & Personal Knowledge** (`NotesScreen.kt`):
   - Semantic vault search across all notes, voice memos, and decisions
   - Horizontal filter chips (All Notes, Voice Memos, Pinned, Strategy, Ideas)
   - Semantic Vault Query prompt banner ("Zero-Cloud Indexed")
   - Audio waveform scrubber player card for voice memos
   - Pinned Foundations notes with encrypted vault locks

6. **You — Privacy & Control** (`YouScreen.kt`):
   - Profile banner with instant theme switcher (Warm Golden vs Dark Obsidian)
   - Operational health metrics (Foreground scheduler, wakelock, OEM status)
   - Preference configuration (Visual Cadence, Quiet Hours, Briefing Time)
   - Data Sovereignty controls: Full Vault Export & Emergency Purge / Backup
