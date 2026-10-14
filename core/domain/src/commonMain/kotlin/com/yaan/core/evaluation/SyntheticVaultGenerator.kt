package com.yaan.core.evaluation

import com.yaan.core.domain.model.AudioMemoMetadata
import com.yaan.core.domain.model.Note
import com.yaan.core.domain.model.Task
import com.yaan.core.domain.model.TaskPriority
import com.yaan.core.domain.model.TaskStatus
import com.yaan.core.domain.model.VaultTag
import kotlinx.datetime.Instant

/**
 * A synthetic vault container containing notes, tasks, and transcripts.
 */
data class SyntheticVault(
    val size: Int,
    val notes: List<Note>,
    val tasks: List<Task>,
) {
    val totalItems: Int get() = notes.size + tasks.size

    fun findItemById(id: String): Any? {
        return notes.find { it.id == id } ?: tasks.find { it.id == id }
    }

    fun getContent(id: String): String? {
        val note = notes.find { it.id == id }
        if (note != null) return "${note.title}\n${note.contentCiphertext}"
        val task = tasks.find { it.id == id }
        if (task != null) return "${task.title}\n${task.notes}"
        return null
    }
}

/**
 * Deterministic synthetic vault generator.
 * Uses procedural pseudo-random algorithms (seeded) so outputs are 100% reproducible across CI runs.
 */
object SyntheticVaultGenerator {

    private val DOMAINS = listOf(
        "Infrastructure", "Product Roadmap", "Finance & Burn", "Client Deliverables",
        "Executive 1:1", "Personal & Health", "Architecture ADR", "Security & Compliance"
    )

    private val KEYWORD_POOL = listOf(
        "Kubernetes cluster migration scheduled for Nov 14 with 40% memory latency reduction",
        "Dental appointment with Dr. Aris confirmed for Tuesday at 2:15 PM at Metro Dental",
        "Q3 AWS cloud spend analysis: $14,200 total, 28% increase driven by unpruned EFS snapshots",
        "Sarah flight SFO to JFK on United Airlines UA412 departing at 8:45 AM Gate 72",
        "Client contract with Apex Global renewed at $180,000 ARR with 30-day payment net terms",
        "WiFi reset master PIN for office guest network is 9482-Bravo-Hotel",
        "Security incident debrief: OAuth token leak resolved by rotating client secret key-pair",
        "David Vance 1:1 discussion: Phased Tier-1 beta rollout starting Dec 02 with 25 accounts",
        "Quarterly board review presentation drafted in Keynote; feedback due by Friday 5 PM",
        "Blood work lab results: Vitamin D level 24 ng/mL, recommended supplement 2000 IU daily",
        "Apartment lease renewal option: monthly rent increases 3.2% effective January 1st",
        "Hetzner GPU server bare-metal provisioning: 4x RTX 4090 deployed in Falkenstein datacenter",
        "Weekly grocery run checklist: organic almond milk, cold brew coffee, sourdough bread, avocado",
        "Project Orion database schema migration: SQLite FTS5 index added for full-text vault lookup",
        "Car service maintenance reminder: 30,000 mile brake pad inspection scheduled at North Bay Auto"
    )

    /**
     * Generates a deterministic vault with [targetSize] items (evenly split between notes and tasks).
     * [seed] guarantees exact reproducibility.
     */
    fun generateVault(targetSize: Int, seed: Long = 42L): SyntheticVault {
        val notesCount = targetSize / 2
        val tasksCount = targetSize - notesCount

        val notes = ArrayList<Note>(notesCount)
        val tasks = ArrayList<Task>(tasksCount)

        // Base epoch: 2026-10-01T00:00:00Z (1790812800)
        val baseEpochSec = 1790812800L

        // Generate Notes
        for (i in 0 until notesCount) {
            val domain = DOMAINS[(i + seed.toInt()).mod(DOMAINS.size)]
            val bodySample = KEYWORD_POOL[(i * 7 + seed.toInt()).mod(KEYWORD_POOL.size)]
            val id = "note_${i + 1}"
            val title = "$domain Note #${i + 1}: ${bodySample.take(32)}..."
            val content = "$bodySample. Additional context: Record item $i generated for vault test in $domain domain."
            val timestamp = Instant.fromEpochSeconds(baseEpochSec + (i * 3600L))

            notes.add(
                Note(
                    id = id,
                    title = title,
                    contentCiphertext = content,
                    tags = listOf(VaultTag(domain.lowercase().replace(" ", "_"), isEncrypted = false)),
                    audioMemo = if (i % 5 == 0) AudioMemoMetadata("file:///vault/audio_$id.m4a", 45000L) else null,
                    isPinned = i % 20 == 0,
                    createdAt = timestamp,
                    updatedAt = timestamp
                )
            )
        }

        // Generate Tasks
        for (i in 0 until tasksCount) {
            val domain = DOMAINS[(i * 2 + seed.toInt()).mod(DOMAINS.size)]
            val sample = KEYWORD_POOL[(i * 11 + seed.toInt()).mod(KEYWORD_POOL.size)]
            val id = "task_${i + 1}"
            val title = "Action Item #${i + 1}: ${sample.take(40)}"
            val dueTimestamp = Instant.fromEpochSeconds(baseEpochSec + (i * 7200L) + 86400L)

            tasks.add(
                Task(
                    id = id,
                    title = title,
                    notes = "Requirement details from $domain: $sample",
                    priority = TaskPriority.entries[(i).mod(TaskPriority.entries.size)],
                    status = if (i % 3 == 0) TaskStatus.COMPLETED else TaskStatus.PENDING,
                    dueAt = dueTimestamp,
                    createdAt = Instant.fromEpochSeconds(baseEpochSec + (i * 3600L))
                )
            )
        }

        return SyntheticVault(size = targetSize, notes = notes, tasks = tasks)
    }
}
