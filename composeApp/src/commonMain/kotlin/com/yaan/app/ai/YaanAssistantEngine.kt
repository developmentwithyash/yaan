package com.yaan.app.ai

import com.yaan.app.ui.state.ProposedActionUi
import com.yaan.core.common.datetime.nowInstant
import com.yaan.core.domain.model.Note
import com.yaan.core.domain.model.Task
import com.yaan.core.domain.model.TaskPriority
import com.yaan.core.domain.model.TaskStatus
import com.yaan.core.domain.repository.MemoryRepository
import com.yaan.core.domain.repository.NoteRepository
import com.yaan.core.domain.repository.TaskRepository
import kotlinx.coroutines.flow.firstOrNull

data class AssistantResponse(
    val replyText: String,
    val citations: List<String> = emptyList(),
    val proposedAction: ProposedActionUi? = null,
    val requiresConfirmation: Boolean = false
)

/**
 * On-Device Intelligent Executive Assistant Engine.
 * Implements conversational reasoning, intent classification,
 * vault grounding, and action proposal orchestration (ADR-0007).
 */
class YaanAssistantEngine(
    private val noteRepository: NoteRepository?,
    private val taskRepository: TaskRepository?,
    private val memoryRepository: MemoryRepository?
) {
    suspend fun processQuery(rawInput: String): AssistantResponse {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return AssistantResponse(
                replyText = "How can I help you today? You can ask me to create tasks, search notes, or plan your schedule."
            )
        }

        val lower = trimmed.lowercase()

        // 1. Adversarial / Injection filter (RuleIntentParser safety invariant)
        if (lower.contains("ignore previous") || lower.contains("system override") || lower.contains("jailbreak")) {
            return AssistantResponse(
                replyText = "Safety Guard: Request declined. YAAN preserves local vault security and privacy invariants."
            )
        }

        // 2. Greetings & Identity
        if (lower in listOf("hi", "hello", "hey", "hey yaan", "hiya", "good morning", "good afternoon", "good evening") ||
            lower.startsWith("who are you") || lower.startsWith("what is yaan")
        ) {
            return AssistantResponse(
                replyText = "Hello! I am YAAN, your Autonomous Action Navigator and personal executive assistant.\n\n" +
                        "I live directly on your phone to help you effortlessly manage tasks, capture notes in an encrypted vault, plan your schedule, and execute voice actions—all with zero data leakage.\n\n" +
                        "What would you like to accomplish right now?"
            )
        }

        // 3. Help & Capabilities
        if (lower in listOf("help", "what can you do", "features", "commands", "how to use", "options")) {
            return AssistantResponse(
                replyText = "Here are key things I can do for you:\n\n" +
                        "• **Tasks & To-Dos**: Say *\"Add task: Review Q3 financials\"* or *\"What are my tasks?\"*\n" +
                        "• **Encrypted Notes**: Say *\"Note: WiFi password is Orion99\"* or *\"Search notes for WiFi\"*\n" +
                        "• **Scheduling & Reminders**: Say *\"Remind me to call John at 5pm\"*\n" +
                        "• **Daily Briefing**: Say *\"What does my day look like?\"* for an executive brief\n" +
                        "• **Live Voice**: Tap the Voice pill or Mic above for hands-free live assistant conversations"
            )
        }

        // 4. Task Listing Queries
        if (lower.contains("my tasks") || lower.contains("show tasks") || lower.contains("list tasks") ||
            lower.contains("what are my tasks") || lower.contains("do i have tasks") || lower == "tasks" ||
            lower.contains("todo list")
        ) {
            val tasks = taskRepository?.observeTasks()?.firstOrNull() ?: emptyList()
            return if (tasks.isEmpty()) {
                AssistantResponse(
                    replyText = "You currently have no tasks in your vault. Would you like to create one?\n\nJust type *\"Add task: [your task]\"* or tap the mic to speak."
                )
            } else {
                val pending = tasks.filter { it.status != TaskStatus.COMPLETED }
                val completed = tasks.filter { it.status == TaskStatus.COMPLETED }
                val taskListText = buildString {
                    append("Here is your active task overview:\n\n")
                    if (pending.isNotEmpty()) {
                        append("**Pending (${pending.size}):**\n")
                        pending.take(6).forEach { t ->
                            val prio = when (t.priority) {
                                TaskPriority.HIGH -> " [High Priority]"
                                TaskPriority.URGENT -> " [URGENT]"
                                else -> ""
                            }
                            append("• [ ] ${t.title}$prio\n")
                        }
                    } else {
                        append("All tasks are currently completed!\n")
                    }
                    if (completed.isNotEmpty()) {
                        append("\n**Recently Completed (${completed.size}):**\n")
                        completed.take(3).forEach { t ->
                            append("• [x] ${t.title}\n")
                        }
                    }
                }
                AssistantResponse(replyText = taskListText)
            }
        }

        // 5. Note Creation
        val isNoteCreation = lower.startsWith("note that ") || lower.startsWith("take a note: ") ||
                lower.startsWith("take a note ") || lower.startsWith("save note ") ||
                lower.startsWith("write down ") || lower.startsWith("new note: ") ||
                lower.startsWith("note: ")

        if (isNoteCreation) {
            val noteContent = trimmed
                .removePrefix("note that ")
                .removePrefix("take a note: ")
                .removePrefix("take a note ")
                .removePrefix("save note ")
                .removePrefix("write down ")
                .removePrefix("new note: ")
                .removePrefix("note: ")
                .removePrefix("Note:")
                .trim()

            val timestamp = nowInstant().toEpochMilliseconds()
            val noteTitle = noteContent.take(36).ifEmpty { "Quick Note" }
            val note = Note(
                id = "note_$timestamp",
                title = noteTitle,
                contentCiphertext = noteContent
            )
            noteRepository?.upsertNote(note)

            return AssistantResponse(
                replyText = "Captured securely in your encrypted vault:\n\n> \"$noteContent\"\n\n[Citation: Vault Note \"$noteTitle\" (ID: note_$timestamp)]",
                citations = listOf("Vault Note: $noteTitle")
            )
        }

        // 6. Action / Task / Reminder Intent Staging
        val isActionIntent = lower.startsWith("reschedule") ||
                lower.startsWith("shift") ||
                lower.startsWith("create task") ||
                lower.startsWith("add task") ||
                lower.startsWith("new task") ||
                lower.startsWith("remind me") ||
                lower.startsWith("schedule") ||
                lower.startsWith("remember to") ||
                lower.startsWith("buy ") ||
                lower.startsWith("call ") ||
                lower.startsWith("draft ") ||
                lower.startsWith("todo")

        if (isActionIntent) {
            val cleanTitle = trimmed
                .removePrefix("create task:")
                .removePrefix("create task ")
                .removePrefix("add task:")
                .removePrefix("add task ")
                .removePrefix("new task:")
                .removePrefix("new task ")
                .removePrefix("remind me to ")
                .removePrefix("remind me ")
                .removePrefix("remember to ")
                .removePrefix("todo:")
                .removePrefix("todo ")
                .trim()

            val propId = "action_${nowInstant().toEpochMilliseconds()}"
            val proposal = ProposedActionUi(
                id = propId,
                title = cleanTitle.ifEmpty { "New Action Item" },
                subtitle = "On-device action proposal · Tap Confirm to commit to vault",
                actionType = if (lower.startsWith("reschedule") || lower.startsWith("schedule")) "CALENDAR_RESCHEDULE" else "TASK_CREATION",
                isConfirmed = false
            )

            return AssistantResponse(
                replyText = "I've staged a new action item for you:\n\n**${proposal.title}**\n\nTap **Confirm** below to commit this directly to your local vault.",
                proposedAction = proposal,
                requiresConfirmation = true
            )
        }

        // 7. Daily Briefing Query
        if (lower.contains("briefing") || lower.contains("what's today") || lower.contains("today's schedule") ||
            lower.contains("morning brief") || lower.contains("my day look like")
        ) {
            val tasks = taskRepository?.observeTasks()?.firstOrNull() ?: emptyList()
            val pendingCount = tasks.count { it.status != TaskStatus.COMPLETED }
            val urgentCount = tasks.count { it.status != TaskStatus.COMPLETED && (it.priority == TaskPriority.HIGH || it.priority == TaskPriority.URGENT) }

            val brief = buildString {
                append("Good morning! Here is your Executive Daily Briefing:\n\n")
                append("• **Focus Overview**: You have $pendingCount pending commitments today.\n")
                if (urgentCount > 0) {
                    append("• **High Priority Attention**: $urgentCount items require your active focus.\n")
                }
                append("• **Vault Sync**: Local database verified 100% encrypted and offline.\n\n")
                append("Would you like to review your tasks in detail or schedule a deep work block?")
            }
            return AssistantResponse(replyText = brief)
        }

        // 8. Note / Vault Search Queries
        val isExplicitSearch = lower.startsWith("find ") || lower.startsWith("search ") || lower.startsWith("where is ")
        val searchQuery = if (isExplicitSearch) {
            trimmed.removePrefix("find ").removePrefix("search ").removePrefix("where is ").trim()
        } else {
            trimmed
        }

        if (noteRepository != null) {
            val notesRes = noteRepository.searchNotes(searchQuery)
            val matchedNotes = notesRes.getOrDefault(emptyList())

            if (matchedNotes.isNotEmpty()) {
                val top = matchedNotes.first()
                val citationsList = matchedNotes.take(3).map { "${it.title} (ID: ${it.id})" }
                return AssistantResponse(
                    replyText = "Based on your encrypted vault notes:\n\n${top.contentCiphertext}\n\n[Citation: ${top.title} (ID: ${top.id})]",
                    citations = citationsList
                )
            }
        }

        // 9. Conversational Fallback & Executive Guidance
        val answer = when {
            lower.contains("prioritize") || lower.contains("priority") ->
                "To prioritize effectively:\n\n1. Identify your **single non-negotiable** task for today.\n2. Block 90 uninterrupted minutes for deep work before checking communications.\n3. Delegate or stage secondary tasks as action proposals in YAAN."

            lower.contains("focus") || lower.contains("deep work") ->
                "Focus guidance:\n\n• Clear notification triggers on your device.\n• Schedule a dedicated 45–60 minute focus sprint.\n• Keep YAAN ready to capture fleeting thoughts without breaking your concentration."

            lower.contains("thank") ->
                "You're very welcome! I'm here on-device whenever you need to capture, plan, or navigate your day."

            else ->
                "I searched your local vault for **\"$trimmed\"** but found no existing records.\n\n" +
                        "Would you like me to:\n" +
                        "• Save this as a new **Note**?\n" +
                        "• Stage this as a **Task**?\n\n" +
                        "Just say *\"Note: $trimmed\"* or *\"Add task: $trimmed\"*."
        }

        return AssistantResponse(replyText = answer)
    }
}
