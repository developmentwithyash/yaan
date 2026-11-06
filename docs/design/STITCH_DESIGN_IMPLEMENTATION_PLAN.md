# Stitch MCP Design Implementation Plan for YAAN

**Project**: YAAN (Your Autonomous Action Navigator)  
**Source**: Stitch MCP Project `829891685103373380` ("YAAN Mobile Design System")  
**Design Theme**: Executive Kinetic Minimalism  
**Target Platform**: Compose Multiplatform (Android, iOS, Desktop) via `:composeApp`  

---

## 1. Design Overview & Principles

The design from Stitch embodies **Executive Kinetic Minimalism**: calm editorial authority merged with precision-engineered minimalism for high-output executives and knowledge workers. It emphasizes:
- **Warm Parchment & Obsidian Clarity**: Warm natural paper undertones (`#FDF9F3` / `#FAF9F5`) reduce eye strain; near-black obsidian (`#141311`) guarantees crisp legibility.
- **Hairline Tactility**: Subtle 1px structural borders (`#DAD4CA` / `#E8E4DC`) replace heavy drop shadows.
- **Bronze-Gold & Iris Accentuation**: Measured authoritative accents (`#785600`, `#966F14`, `#D4AF37`) for high-value autonomous actions and AI reasoning.
- **Explainable Autonomous Surfaces**: AI reasoning disclosure trees ("Thinking Process"), grounded vault source citations, and binary decision triggers ("Schedule & Send", "Modify").

---

## 2. Token Architecture (Compose Multiplatform)

### Color Palette
- **Canvas Base**: `Surface` = `#FDF9F3`, `Background` = `#FDF9F3`
- **Surface Elevation**:
  - Level 0 (Canvas): `#FDF9F3`
  - Level 1 (Lowest / Card Base): `#FFFFFF`
  - Level 2 (Low / Inset Container): `#F7F3EE`
  - Level 3 (Container / Control): `#F2EDE8`
  - Level 4 (High): `#ECE7E2`
  - Level 5 (Highest): `#E6E2DD`
- **Accents**:
  - Primary: `#785600`
  - Primary Container: `#986D00`
  - Bronze Accent: `#966F14`
  - Amber Gold: `#D4AF37`
  - Champagne Tint: `#FEF9E7`
  - Sand Surface: `#F5F2EB`
- **Outlines**:
  - Outline: `#817563`
  - Outline Variant: `#D3C4AF`
  - Delicate Border: `#DAD4CA`
- **Dark Mode Equivalents**:
  - Surface: `#141311`, Background: `#0E0D0C`
  - Surface Containers: `#1E1C19`, `#262420`, `#302D28`
  - Primary Accent: `#F7BD48`, Bronze Accent: `#E5B84B`
  - Delicate Border: `#3A362F`

### Typography (Plus Jakarta Sans)
- `DisplayLg`: 40sp / 48sp, Bold, Tracking -0.03em
- `DisplayMobile`: 32sp / 40sp, Bold, Tracking -0.025em
- `HeadlineLg`: 28sp / 36sp, SemiBold, Tracking -0.02em
- `HeadlineMd`: 22sp / 30sp, SemiBold, Tracking -0.015em
- `HeadlineSm`: 18sp / 26sp, SemiBold, Tracking -0.01em
- `BodyLg`: 16sp / 26sp, Normal, Tracking -0.005em
- `BodyMd`: 14sp / 22sp, Normal, Tracking 0em
- `BodySm`: 13sp / 18sp, Normal, Tracking 0em
- `LabelLg`: 14sp / 20sp, SemiBold, Tracking 0.01em
- `LabelMd`: 12sp / 16sp, SemiBold, Tracking 0.02em
- `LabelSm`: 11sp / 14sp, Medium, Tracking 0.04em

---

## 3. UI Component Catalog

1. **`YaanTheme`**: Material3 theme wrapper exposing both standard color scheme and custom extended tokens (`YaanColors`).
2. **`YaanTopBar`**: Header with `Y` avatar badge, "YAAN • Screen Name", Vault status beacon ("Online · Local Vault"), and quick action buttons.
3. **`YaanBottomNavigation`**: Bottom navigation dock with 5 core tabs (Home, Plan, Ask YAAN floating circular beacon, Notes, You).
4. **`YaanCard`**: Tactile card with 12dp/16dp corner radius, hairline border (`#DAD4CA`), and soft surface elevation.
5. **`YaanButton`**: High-contrast primary action button with pressed scale micro-interaction and secondary outline button.
6. **`YaanChip` / `FilterChip`**: Pill status chips with icon, text, and active state highlights.
7. **`YaanQuickCaptureDock`**: Bottom-anchored or inline prompt input bar with voice waveform toggle and quick chips (+Note, +Reminder, Voice thought, +Meeting).

---

## 4. Screen Implementations

1. **`HomeScreen`** (Home Command Center):
   - Editorial date stamp & greeting ("Good morning, Elena")
   - Daily Briefing card with commitment counter & cognitive load badge
   - Explainable Conflict suggestion card with 1-tap adjustment ("Adjust 1:15 PM")
   - Next Commitment card with Google Meet link & attendee avatars
   - Today's Priorities checklist with animated checkmark toggling
   - Inline Quick Capture composer
2. **`PlanScreen`** (Plan & Reminders):
   - Later Today scheduled commitments
   - Active Streams project accordion card with 60% progress bar
   - Defense-in-depth notification status
3. **`AskYaanScreen`** (Ask YAAN & Intelligent Assistant):
   - Pill mode switcher (Chat vs Voice)
   - Expandable "Thinking Process" disclosure (scanned local vector store)
   - Executive Dispatch card with Q4 Strategic Milestones
   - Grounded Citations carousel (Voice Memo, Notes backlinks)
   - Proposed Executive Action card with "Schedule & Send" affirmation
   - Signature floating pill prompt composer & Live Voice trigger
4. **`LiveVoiceModal`** (Ask YAAN — Live Voice Mode):
   - Organic animated waveform orb with multi-ring pulsation
   - Mute toggle, speed selector (1.0x, 1.2x, 1.5x), push-to-talk simulation
5. **`NotesScreen`** (Notes & Personal Knowledge):
   - Search bar across all notes, voice memos, and decisions
   - Filter chips (All Notes, Voice Memos, Pinned, Strategy, Ideas)
   - Semantic Vault Query prompt card ("Zero-Cloud Indexed")
   - Pinned Foundations notes with encrypted vault locks
6. **`YouScreen`** (You — Privacy & Control):
   - Local Notification Engine & Wakelock status indicators
   - Preferences & Rhythms (Visual Cadence, Quiet Hours, Daily Briefing Time)
   - Data Sovereignty vault export & Emergency Purge action

---

## 5. Verification & TDD Pipeline

1. **TDD Unit Tests**:
   - `YaanThemeTest`: Validate color palette values, contrast ratios, and typography mappings.
   - `YaanNavigationTest`: Validate tab routing and state management.
2. **Compilation & Build**:
   - Run `./gradlew :composeApp:compileCommonMainKotlinMetadata` and `./gradlew :composeApp:testDebugUnitTest`.
3. **Evidence Artifacts**: Collect fresh terminal verification logs.
