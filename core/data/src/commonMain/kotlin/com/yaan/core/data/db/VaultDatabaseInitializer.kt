package com.yaan.core.data.db

import com.yaan.core.common.datetime.nowInstant

/**
 * Initializes and seeds the SQLite vault database with authentic executive fixtures
 * on first cold-launch if tables are empty.
 */
object VaultDatabaseInitializer {

    fun seedIfEmpty(database: YaanDatabase) {
        val queries = database.yaanDatabaseQueries
        val existingTasks = queries.selectAllTasks().executeAsList()
        if (existingTasks.isNotEmpty()) return

        val now = nowInstant().toEpochMilliseconds()

        // 1. Seed Initial Tasks
        queries.insertTask(
            id = "task_01",
            title = "Send revised terms to Raj (Stripe partner)",
            due_time = "Due 12:00 PM",
            is_completed = true,
            tag = "HIGH",
            is_high_priority = true,
            created_at = now - 3600_000,
            updated_at = now
        )
        queries.insertTask(
            id = "task_02",
            title = "Review Q3 Cloud Infra spend anomaly note",
            due_time = "Due 3:30 PM",
            is_completed = false,
            tag = "MEDIUM",
            is_high_priority = false,
            created_at = now - 7200_000,
            updated_at = now
        )
        queries.insertTask(
            id = "task_03",
            title = "Draft YAAN MVP release notes v0.9",
            due_time = "Flexible",
            is_completed = false,
            tag = "LOW",
            is_high_priority = false,
            created_at = now - 10800_000,
            updated_at = now
        )

        // 2. Seed Initial Encrypted Notes
        queries.insertNote(
            id = "note_01",
            title = "Executive Architecture Decision: Asymmetric Hybrid Cloud",
            content = "All deterministic CRUD operates at 0ms locally via SQLite. Speculative cloud stream offloads to Groq LPUs only when confidence < 0.75.",
            tag = "Decisions",
            is_pinned = true,
            is_encrypted = true,
            audio_duration = null,
            created_at = now - 7200_000,
            updated_at = now - 7200_000
        )
        queries.insertNote(
            id = "note_02",
            title = "Voice Memo: Post-Sync Architecture Alignment with Alex",
            content = "Discussed the cellular radio hibernation cycle (45s idle socket timeout) and QUIC 0-RTT resumption benefits.",
            tag = "Voice Memos",
            is_pinned = false,
            is_encrypted = true,
            audio_duration = "02:45",
            created_at = now - 86400_000,
            updated_at = now - 86400_000
        )
        queries.insertNote(
            id = "note_03",
            title = "Q3 Infrastructure Spend Root Cause Analysis",
            content = "Kafka multi-AZ replication was driving $800/mo ingress/egress. Migrated to PostgreSQL Outbox + Redis Streams to achieve $0.05/user/mo.",
            tag = "AWS Infra",
            is_pinned = false,
            is_encrypted = true,
            audio_duration = null,
            created_at = now - 259200_000,
            updated_at = now - 259200_000
        )

        // 3. Seed Initial Daily Briefing
        queries.insertBriefing(
            id = "briefing_today",
            headline = "Good morning, Elena",
            summary = "You have 3 primary commitments today. Your next action is preparing the Stripe partnership term sheet, and 1 conflict requires rescheduling.",
            date = "2026-10-15",
            is_listened = false,
            conflict_title = "Dentist Appointment vs Q3 Cloud Spend Review",
            conflict_slot = "3:00 PM – 4:00 PM",
            conflict_resolution = "Shift Dentist prep alert to 2:15 PM (25 min transit buffer)"
        )

        // 4. Seed Initial Reminders
        queries.insertReminder(
            id = "rem_01",
            title = "Dentist Appointment",
            trigger_time = now + 14400_000,
            recurrence = "NONE",
            is_dismissed = false,
            channel = "ALARM",
            created_at = now
        )
        queries.insertReminder(
            id = "rem_02",
            title = "Q3 Cloud Spend Review",
            trigger_time = now + 18000_000,
            recurrence = "NONE",
            is_dismissed = false,
            channel = "NOTIFICATION",
            created_at = now
        )

        // 5. Seed Initial Memories
        queries.insertMemory(
            id = "mem_01",
            content = "Elena's Dentist Appointment | Scheduled for 3:00 PM today with 25m transit buffer",
            category = "Schedule",
            confidence = 0.95,
            source_id = "note_01",
            created_at = now
        )
        queries.insertMemory(
            id = "mem_02",
            content = "Stripe Partnership Contact | Raj is leading the partnership term sheet review",
            category = "Partnerships",
            confidence = 0.98,
            source_id = "task_01",
            created_at = now
        )
    }
}
