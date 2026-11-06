# ADR 0008: Supabase Managed Auth, Online Database Schema & Zero-Knowledge Sync Relay

## Status
**ACCEPTED & RATIFIED**

---

## Context
YAAN requires managed cloud user authentication (email/magic links, OAuth, passkeys) and an online database for:
1. Managing user profiles and DPDP Act 2023 compliance status (18+ age declaration).
2. Registering and coordinating paired user devices (Android, iOS, Desktop) for E2EE key distribution.
3. Serving as a blind synchronization relay for encrypted CRDT deltas across devices.

While cloud synchronization is introduced, YAAN's core architectural invariants must remain inviolable:
- **Invariant 1**: Deterministic state decoupled from probabilistic AI (100% offline local SQLite execution).
- **Invariant 2**: Zero-Knowledge Privacy (AES-GCM-256 local vault encryption; the cloud database is a blind relay with no decryption capability).
- **Invariant 3**: Sub-150ms interaction ceiling (local operations are 0ms; cloud sync is entirely asynchronous).

---

## Decision

### 1. Identity & Auth Architecture (Supabase GoTrue)
- Supabase GoTrue Auth handles identity verification, email confirmation, magic links, and passkey registration.
- Successful authentication yields JWT tokens containing `sub` (user UUID).
- JWTs are validated by the Ktor backend and directly by Supabase PostgreSQL via Row Level Security (RLS).
- Vault encryption keys are derived on-device (Argon2id + biometric master key) and NEVER sent to Supabase Auth.

### 2. PostgreSQL Schema & Data Model
The online database consists of three relational tables within the `public` schema:

1. **`public.profiles`**:
   - `id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE`
   - `email TEXT`
   - `display_name TEXT`
   - `tier TEXT DEFAULT 'FREE'` ('FREE', 'PRO', 'TEAM')
   - `age_confirmed_18_plus BOOLEAN DEFAULT FALSE`
   - `created_at TIMESTAMPTZ DEFAULT now()`, `updated_at TIMESTAMPTZ DEFAULT now()`
   - Managed automatically via PostgreSQL trigger `on_auth_user_created` listening to `auth.users`.

2. **`public.devices`**:
   - `device_id TEXT PRIMARY KEY`
   - `user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE`
   - `device_name TEXT NOT NULL`
   - `platform TEXT NOT NULL CHECK (platform IN ('android', 'ios', 'desktop', 'web'))`
   - `public_key_der TEXT` (Public key for Noise Protocol XX handshake)
   - `last_seen_at TIMESTAMPTZ DEFAULT now()`, `created_at TIMESTAMPTZ DEFAULT now()`

3. **`public.encrypted_sync_envelopes`**:
   - `id TEXT PRIMARY KEY`
   - `user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE`
   - `device_id TEXT NOT NULL REFERENCES public.devices(device_id) ON DELETE CASCADE`
   - `entity_type TEXT NOT NULL CHECK (entity_type IN ('TASK', 'NOTE', 'REMINDER', 'MEMORY', 'SETTING'))`
   - `ciphertext TEXT NOT NULL` (AES-GCM-256 encrypted payload base64)
   - `nonce TEXT NOT NULL`
   - `lamport_clock BIGINT NOT NULL DEFAULT 0`
   - `epoch BIGINT NOT NULL DEFAULT 1`
   - `timestamp_ms BIGINT NOT NULL`
   - `created_at TIMESTAMPTZ DEFAULT now()`

### 3. Row Level Security (RLS) Policies
Every table enforces PostgreSQL Row Level Security:
- `profiles`: Users can select and update only their own profile (`auth.uid() = id`).
- `devices`: Users can select, insert, update, and delete only their own registered devices (`auth.uid() = user_id`).
- `encrypted_sync_envelopes`: Users can insert and select only their own sync envelopes (`auth.uid() = user_id`). Cross-user reading is impossible at the database engine level.

### 4. Migration & Deployment Strategy
- All DDL is stored in version-controlled SQL files under `supabase/migrations/`.
- Deployment is executed via Supabase CLI (`supabase db push`) or direct SQL execution in the Supabase Dashboard.
- Zero local build breakage: CI/CD test suites run against mock session providers without requiring live cloud connectivity.

---

## Consequences
- **Positive**: Standardized, production-grade cloud authentication; instant cross-device multi-platform support; zero server maintenance burden for user tables.
- **Security**: Invariant 2 is preserved because only ciphertext envelopes touch Supabase. Compromise of the Supabase database reveals zero plaintext user data.
- **Compliance**: DPDP Act 2023 age confirmation recorded explicitly; account deletion (`auth.users` deletion) cascades to all profiles, devices, and envelopes (crypto-shredding).
