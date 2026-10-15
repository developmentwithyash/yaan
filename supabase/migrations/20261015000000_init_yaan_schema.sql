-- ==============================================================================
-- YAAN — Online Database Schema Initialization
-- Migration: 20261015000000_init_yaan_schema.sql
-- Invariants Preserved:
--   1. Deterministic local SQLite remains client source-of-truth.
--   2. Zero-Knowledge: All vault payloads stored in encrypted_sync_envelopes
--      are AES-GCM-256 ciphertexts. Cloud database is a blind relay.
--   3. Row Level Security (RLS) enabled on all tables (BOLA defense).
-- ==============================================================================

-- 1. Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 2. Profiles Table (Extends Supabase auth.users)
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT,
    display_name TEXT NOT NULL DEFAULT '',
    tier TEXT NOT NULL DEFAULT 'FREE' CHECK (tier IN ('FREE', 'PRO', 'TEAM')),
    age_confirmed_18_plus BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- 3. Registered Devices Table (Multi-Device E2EE & Noise Protocol Handshake)
CREATE TABLE IF NOT EXISTS public.devices (
    device_id TEXT PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    device_name TEXT NOT NULL,
    platform TEXT NOT NULL CHECK (platform IN ('android', 'ios', 'desktop', 'web')),
    public_key_der TEXT,
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- 4. Blind Encrypted Sync Relay Table (Zero-Knowledge CRDT Deltas)
CREATE TABLE IF NOT EXISTS public.encrypted_sync_envelopes (
    id TEXT PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    device_id TEXT NOT NULL REFERENCES public.devices(device_id) ON DELETE CASCADE,
    entity_type TEXT NOT NULL CHECK (entity_type IN ('TASK', 'NOTE', 'REMINDER', 'MEMORY', 'SETTING')),
    ciphertext TEXT NOT NULL,
    nonce TEXT NOT NULL,
    lamport_clock BIGINT NOT NULL DEFAULT 0,
    epoch BIGINT NOT NULL DEFAULT 1,
    timestamp_ms BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- 5. Performance Indexes
CREATE INDEX IF NOT EXISTS idx_devices_user_id 
    ON public.devices (user_id);

CREATE INDEX IF NOT EXISTS idx_sync_envelopes_user_lamport 
    ON public.encrypted_sync_envelopes (user_id, lamport_clock ASC);

CREATE INDEX IF NOT EXISTS idx_sync_envelopes_user_entity 
    ON public.encrypted_sync_envelopes (user_id, entity_type);

CREATE INDEX IF NOT EXISTS idx_sync_envelopes_created_at 
    ON public.encrypted_sync_envelopes (created_at DESC);

-- 6. Row Level Security (RLS) Enablement
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.devices ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.encrypted_sync_envelopes ENABLE ROW LEVEL SECURITY;

-- 7. RLS Policies: Profiles
DROP POLICY IF EXISTS "Users can view own profile" ON public.profiles;
CREATE POLICY "Users can view own profile" 
    ON public.profiles 
    FOR SELECT 
    USING (auth.uid() = id);

DROP POLICY IF EXISTS "Users can update own profile" ON public.profiles;
CREATE POLICY "Users can update own profile" 
    ON public.profiles 
    FOR UPDATE 
    USING (auth.uid() = id);

DROP POLICY IF EXISTS "Users can insert own profile" ON public.profiles;
CREATE POLICY "Users can insert own profile" 
    ON public.profiles 
    FOR INSERT 
    WITH CHECK (auth.uid() = id);

-- 8. RLS Policies: Devices
DROP POLICY IF EXISTS "Users can view own devices" ON public.devices;
CREATE POLICY "Users can view own devices" 
    ON public.devices 
    FOR SELECT 
    USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can register devices" ON public.devices;
CREATE POLICY "Users can register devices" 
    ON public.devices 
    FOR INSERT 
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can update own devices" ON public.devices;
CREATE POLICY "Users can update own devices" 
    ON public.devices 
    FOR UPDATE 
    USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can delete own devices" ON public.devices;
CREATE POLICY "Users can delete own devices" 
    ON public.devices 
    FOR DELETE 
    USING (auth.uid() = user_id);

-- 9. RLS Policies: Encrypted Sync Envelopes (Blind Relay)
DROP POLICY IF EXISTS "Users can read own sync envelopes" ON public.encrypted_sync_envelopes;
CREATE POLICY "Users can read own sync envelopes" 
    ON public.encrypted_sync_envelopes 
    FOR SELECT 
    USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can insert own sync envelopes" ON public.encrypted_sync_envelopes;
CREATE POLICY "Users can insert own sync envelopes" 
    ON public.encrypted_sync_envelopes 
    FOR INSERT 
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can delete own sync envelopes" ON public.encrypted_sync_envelopes;
CREATE POLICY "Users can delete own sync envelopes" 
    ON public.encrypted_sync_envelopes 
    FOR DELETE 
    USING (auth.uid() = user_id);

-- 10. Automatic Profile Creation on User Signup
CREATE OR REPLACE FUNCTION public.handle_new_user() 
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, email, display_name, age_confirmed_18_plus)
    VALUES (
        new.id,
        new.email,
        COALESCE(new.raw_user_meta_data->>'display_name', split_part(new.email, '@', 1)),
        COALESCE((new.raw_user_meta_data->>'age_confirmed_18_plus')::boolean, FALSE)
    )
    ON CONFLICT (id) DO NOTHING;
    RETURN new;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();
