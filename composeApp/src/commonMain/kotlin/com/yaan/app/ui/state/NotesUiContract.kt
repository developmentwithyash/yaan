package com.yaan.app.ui.state

/**
 * UI State Model for a Note item.
 */
data class NoteItemUi(
    val id: String,
    val title: String,
    val preview: String,
    val tag: String,
    val timestamp: String,
    val isPinned: Boolean = false,
    val isEncrypted: Boolean = true,
    val audioDuration: String? = null
)

/**
 * Pure Presentation State for the Notes and Knowledge Vault screen.
 */
data class NotesUiState(
    val notes: List<NoteItemUi> = defaultNotesList,
    val searchQuery: String = "",
    val selectedFilter: String = "All Notes",
    val availableFilters: List<String> = listOf("All Notes", "Decisions", "AWS Infra", "Voice Memos", "Product Specs", "Pinned"),
    val isPlayingAudio: Boolean = false,
    val isLoading: Boolean = false
)

val defaultNotesList = listOf(
    NoteItemUi(
        id = "1",
        title = "Executive Architecture Decision: Asymmetric Hybrid Cloud",
        preview = "All deterministic CRUD operates at 0ms locally via SQLite. Speculative cloud stream offloads to Groq LPUs only when confidence < 0.75.",
        tag = "Decisions",
        timestamp = "Updated 2h ago",
        isPinned = true,
        isEncrypted = true
    ),
    NoteItemUi(
        id = "2",
        title = "Voice Memo: Post-Sync Architecture Alignment with Alex",
        preview = "Discussed the cellular radio hibernation cycle (45s idle socket timeout) and QUIC 0-RTT resumption benefits.",
        tag = "Voice Memos",
        timestamp = "Recorded yesterday",
        isPinned = false,
        isEncrypted = true,
        audioDuration = "02:45"
    ),
    NoteItemUi(
        id = "3",
        title = "Q3 Infrastructure Spend Root Cause Analysis",
        preview = "Kafka multi-AZ replication was driving $800/mo ingress/egress. Migrated to PostgreSQL Outbox + Redis Streams to achieve $0.05/user/mo.",
        tag = "AWS Infra",
        timestamp = "3 days ago",
        isPinned = false,
        isEncrypted = true
    )
)

/**
 * User Actions and Intents emitted by the NotesScreen.
 */
sealed interface NotesUiAction {
    data class UpdateSearchQuery(val query: String) : NotesUiAction
    data class SelectFilter(val filter: String) : NotesUiAction
    data object ToggleAudioPlayback : NotesUiAction
    data class DeleteNote(val id: String) : NotesUiAction
    data object NavigateToAskYaan : NotesUiAction
}
