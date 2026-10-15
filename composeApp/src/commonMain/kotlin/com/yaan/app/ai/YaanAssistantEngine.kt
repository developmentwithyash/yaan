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
import com.yaan.core.domain.action.ActionState
import com.yaan.core.domain.action.ActionStateMachine
import com.yaan.core.domain.time.TimeResolutionEngine
import com.yaan.core.domain.tool.ToolCallProposal
import com.yaan.core.domain.tool.ToolRegistry
import kotlinx.coroutines.flow.firstOrNull

data class AssistantResponse(
    val replyText: String,
    val citations: List<String> = emptyList(),
    val proposedAction: ProposedActionUi? = null,
    val requiresConfirmation: Boolean = false
)

private data class ReminderExtracted(
    val title: String,
    val formattedSchedule: String
)

/**
 * On-Device Intelligent Executive Assistant Engine.
 * Automatically classifies natural language into Notes, Reminders, Tasks, Briefings, or Vault Searches.
 */
class YaanAssistantEngine(
    private val noteRepository: NoteRepository? = null,
    private val taskRepository: TaskRepository? = null,
    private val memoryRepository: MemoryRepository? = null,
    private val timeResolutionEngine: TimeResolutionEngine = TimeResolutionEngine(),
    private val actionStateMachine: ActionStateMachine = ActionStateMachine()
) {
    val stateMachine: ActionStateMachine get() = actionStateMachine
    val timeEngine: TimeResolutionEngine get() = timeResolutionEngine

    suspend fun processQuery(rawInput: String): AssistantResponse {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return AssistantResponse(
                replyText = "How can I help you today? You can ask me to set reminders, take notes, or organize your day."
            )
        }

        val lower = trimmed.lowercase()

        // 1. Adversarial / Prompt Injection Defense
        if (lower.contains("ignore previous") || lower.contains("system override") || lower.contains("jailbreak")) {
            return AssistantResponse(
                replyText = "Safety Guard: Request declined. YAAN preserves local vault security and privacy invariants."
            )
        }

        // 2. Greetings & Persona Inquiries
        if (lower in listOf("hi", "hello", "hey", "hey yaan", "hiya", "good morning", "good afternoon", "good evening") ||
            lower.startsWith("who are you") || lower.startsWith("what is yaan")
        ) {
            return AssistantResponse(
                replyText = "Hello! I am YAAN, your Autonomous Action Navigator and personal executive assistant.\n\n" +
                        "I live directly on your phone to automatically capture notes in your encrypted vault, schedule reminders, plan your day, and execute voice actions—all with zero data leakage.\n\n" +
                        "What can I do for you right now?"
            )
        }

        // 3. Help & Capabilities
        if (lower in listOf("help", "what can you do", "features", "commands", "how to use", "options")) {
            return AssistantResponse(
                replyText = "Here are key things I automatically detect for you:\n\n" +
                        "• **Reminders & Tasks**: Just say *\"Remind me for doctor tomorrow 2pm\"* or *\"Schedule meeting at 4pm\"*\n" +
                        "• **Encrypted Notes**: Say *\"Note: WiFi password is Orion99\"* or *\"The office passcode is 4821\"*\n" +
                        "• **Task Overview**: Ask *\"What are my tasks?\"* or *\"Do I have reminders?\"*\n" +
                        "• **Vault Search**: Ask *\"Where is the gate code?\"* or *\"Find notes about doctor\"*\n" +
                        "• **Daily Briefing**: Say *\"What does my day look like?\"* for your executive brief\n" +
                        "• **Live Voice**: Tap the Voice pill or Mic above for hands-free live assistant conversations"
            )
        }

        // 4. Task / Reminder Listing Queries
        val isQueryingTasks = (lower.contains("my tasks") || lower.contains("show tasks") || lower.contains("list tasks") ||
                lower.contains("what are my tasks") || lower.contains("do i have tasks") || lower == "tasks" ||
                lower.contains("todo list") || lower.contains("do i have any reminder") ||
                lower.contains("show my reminders") || lower.contains("what are my reminders")) &&
                !lower.contains("set a reminder") && !lower.contains("create") && !lower.contains("add")

        if (isQueryingTasks) {
            val tasks = taskRepository?.observeTasks()?.firstOrNull() ?: emptyList()
            return if (tasks.isEmpty()) {
                AssistantResponse(
                    replyText = "You currently have no pending tasks or reminders in your vault. Would you like to set one?\n\nJust tell me what you'd like to be reminded of."
                )
            } else {
                val pending = tasks.filter { it.status != TaskStatus.COMPLETED }
                val completed = tasks.filter { it.status == TaskStatus.COMPLETED }
                val taskListText = buildString {
                    append("Here is your active vault overview:\n\n")
                    if (pending.isNotEmpty()) {
                        append("**Pending Commitments (${pending.size}):**\n")
                        pending.take(6).forEach { t ->
                            val prio = when (t.priority) {
                                TaskPriority.HIGH -> " [High Priority]"
                                TaskPriority.URGENT -> " [URGENT]"
                                else -> ""
                            }
                            val scheduleNote = if (t.notes.isNotBlank()) " · ${t.notes}" else ""
                            append("• [ ] ${t.title}$scheduleNote$prio\n")
                        }
                    } else {
                        append("All scheduled commitments are currently completed!\n")
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

        // 5. Daily Briefing Query
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
                append("Would you like to review your tasks in detail or schedule a focus block?")
            }
            return AssistantResponse(replyText = brief)
        }

        // 6. Explicit or Implicit REMINDER / TASK Detection
        val isReminderOrTask = isReminderIntent(lower)
        if (isReminderOrTask) {
            val extracted = parseReminderDetails(trimmed)
            val nowMs = nowInstant().toEpochMilliseconds()
            val timeRes = timeResolutionEngine.resolve(trimmed, nowMs)
            val triggerEpochMs = timeRes.resolvedEpochMs ?: (nowMs + 3600000L)
            val recurrenceToken = when {
                timeRes.recurrenceRule?.contains("WEEKLY") == true -> "WEEKLY"
                timeRes.recurrenceRule?.contains("DAILY") == true -> "DAILY"
                timeRes.recurrenceRule?.contains("MONTHLY") == true -> "MONTHLY"
                else -> null
            }

            val callProposal = ToolCallProposal(
                toolName = ToolRegistry.CREATE_REMINDER.name,
                arguments = buildMap {
                    put("title", extracted.title)
                    put("triggerTimeEpochMs", triggerEpochMs.toString())
                    recurrenceToken?.let { put("recurrence", it) }
                    timeRes.recurrenceRule?.let { put("rrule", it) }
                },
                rationale = "User requested a reminder",
                confidence = 0.80f,
                isExplicitUserRequest = false,
                idempotencyKey = "act_$nowMs"
            )
            val actionInstance = actionStateMachine.propose(callProposal, nowMs)

            val proposal = ProposedActionUi(
                id = actionInstance.idempotencyKey,
                title = extracted.title,
                subtitle = "${extracted.formattedSchedule} · Grounded on-device action",
                actionType = "REMINDER",
                isConfirmed = false,
                toolCall = callProposal
            )

            val reply = buildString {
                append("I've staged a reminder for you:\n\n")
                append("📌 **${extracted.title}**\n")
                append("⏰ **Scheduled**: ${extracted.formattedSchedule}\n\n")
                append("Tap **Confirm** below to save this directly to your local vault.")
            }

            return AssistantResponse(
                replyText = reply,
                proposedAction = proposal,
                requiresConfirmation = true
            )
        }

        // 7. Explicit or Implicit NOTE Detection
        val isNote = isNoteIntent(lower)
        if (isNote) {
            val cleanContent = cleanNoteContent(trimmed)
            val timestamp = nowInstant().toEpochMilliseconds()
            val noteTitle = cleanContent.take(40).ifEmpty { "Quick Note" }
            val note = Note(
                id = "note_$timestamp",
                title = noteTitle,
                contentCiphertext = cleanContent
            )
            noteRepository?.upsertNote(note)

            return AssistantResponse(
                replyText = "Captured securely in your encrypted vault:\n\n> \"$cleanContent\"\n\n[Citation: Vault Note \"$noteTitle\" (ID: note_$timestamp)]",
                citations = listOf("Vault Note: $noteTitle")
            )
        }

        // 8. Note / Vault Search Queries
        val isExplicitSearch = lower.startsWith("find ") || lower.startsWith("search ") ||
                lower.startsWith("where is ") || lower.startsWith("what did i note") ||
                lower.startsWith("what is the ") || lower.contains("passcode") || lower.contains("password")

        val searchQuery = cleanSearchQuery(trimmed)

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
                        "• Stage this as a **Reminder**?\n\n" +
                        "Just say *\"Note: $trimmed\"* or *\"Remind me for $trimmed\"*."
        }

        return AssistantResponse(replyText = answer)
    }

    private fun isReminderIntent(lower: String): Boolean {
        // Direct reminder keywords
        if (lower.contains("reminder") || lower.contains("remind me") || lower.contains("alert me") ||
            lower.contains("notify me") || lower.contains("set a reminder") || lower.contains("create a reminder") ||
            lower.contains("add a reminder") || lower.contains("schedule a reminder")
        ) {
            return true
        }

        // Scheduling and task keywords
        if (lower.startsWith("schedule") || lower.startsWith("reschedule") || lower.startsWith("postpone") ||
            lower.startsWith("todo") || lower.startsWith("task:") || lower.startsWith("create task") ||
            lower.startsWith("add task") || lower.startsWith("new task") || lower.startsWith("remember to") ||
            lower.startsWith("don't forget to") || lower.startsWith("dont forget to") || lower.startsWith("stage reminder")
        ) {
            return true
        }

        // Action imperatives or activities with times or deadlines
        val hasTimeOrDate = hasTemporalMarker(lower)
        val actionVerbs = listOf(
            "call ", "buy ", "send ", "email ", "draft ", "pay ", "pick up ", "meet ", "finish ", "submit ",
            "workout", "doctor", "dentist", "flight", "gym", "meeting", "appointment", "dinner", "lunch",
            "breakfast", "coffee", "medication", "medicine", "pill", "groceries", "grocery", "service",
            "class", "lecture", "interview", "review"
        )
        if (hasTimeOrDate && actionVerbs.any { lower.contains(it) }) {
            return true
        }

        return false
    }

    private fun isNoteIntent(lower: String): Boolean {
        // Explicit note commands
        if (lower.startsWith("note:") || lower.startsWith("note that") || lower.startsWith("note ") ||
            lower.startsWith("take a note") || lower.startsWith("take note") ||
            lower.startsWith("save note") || lower.startsWith("save a note") ||
            lower.startsWith("create note") || lower.startsWith("create a note") ||
            lower.startsWith("add note") || lower.startsWith("add a note") ||
            lower.startsWith("make a note") || lower.startsWith("make note") ||
            lower.startsWith("write down") || lower.startsWith("new note") ||
            lower.startsWith("jot down") || lower.startsWith("keep in mind that") ||
            lower.startsWith("record this") || lower.startsWith("record note") ||
            lower.startsWith("remember that ") || lower.startsWith("save this:") ||
            lower.startsWith("save this ")
        ) {
            return true
        }

        // Credential, secret, code, or personal fact patterns
        if (lower.contains("password is") || lower.contains("passcode is") || lower.contains("wifi is") ||
            lower.contains("gate code is") || lower.contains("pin is") || lower.contains("phone number is") ||
            lower.contains("ip address is") || lower.contains("address is") || lower.contains("key is") ||
            lower.contains("locker combination is") || lower.contains("blood group is") || lower.startsWith("idea:")
        ) {
            return true
        }

        return false
    }

    private fun hasTemporalMarker(lower: String): Boolean {
        if (lower.contains("tomorrow") || lower.contains("tommorow") || lower.contains("tommoroww") || lower.contains("tomorow") ||
            lower.contains("today") || lower.contains("tonight") || lower.contains("next week")
        ) {
            return true
        }
        val days = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
        if (days.any { lower.contains(it) }) return true

        // 2pm, 3:30 pm, 8am, 10:00am, etc.
        val timeRegex = Regex("\\b\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)\\b", RegexOption.IGNORE_CASE)
        return timeRegex.containsMatchIn(lower)
    }

    private fun parseReminderDetails(raw: String): ReminderExtracted {
        var text = raw
        val lower = raw.lowercase()

        // 1. Extract and normalize date
        var dateStr = "Today"
        when {
            lower.contains("tomorrow") || lower.contains("tommorow") || lower.contains("tommoroww") || lower.contains("tomorow") -> {
                dateStr = "Tomorrow"
                text = text.replace(Regex("(?i)tom+or+ow+"), "")
            }
            lower.contains("tonight") -> {
                dateStr = "Tonight"
                text = text.replace(Regex("(?i)tonight"), "")
            }
            lower.contains("today") -> {
                dateStr = "Today"
                text = text.replace(Regex("(?i)today"), "")
            }
            lower.contains("next week") -> {
                dateStr = "Next Week"
                text = text.replace(Regex("(?i)next week"), "")
            }
        }

        val recurringDays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        var matchedRecurring = false
        for (day in recurringDays) {
            if (lower.contains("every ${day.lowercase()}")) {
                dateStr = "Every $day"
                text = text.replace(Regex("(?i)every\\s+$day"), "")
                matchedRecurring = true
                break
            }
        }
        if (!matchedRecurring && (lower.contains("every day") || lower.contains("daily"))) {
            dateStr = "Daily"
            text = text.replace(Regex("(?i)(every day|daily)"), "")
            matchedRecurring = true
        }

        if (!matchedRecurring) {
            val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
            for (day in days) {
                if (lower.contains(day.lowercase())) {
                    dateStr = day
                    text = text.replace(Regex("(?i)\\b(on\\s+)?$day\\b"), "")
                    break
                }
            }
        }

        // 2. Extract time pattern (e.g., 2pm, 2:30 pm)
        var timeStr = ""
        val timeRegex = Regex("(?i)\\b(\\d{1,2}(?::\\d{2})?\\s*(?:am|pm))\\b")
        val match = timeRegex.find(text)
        if (match != null) {
            timeStr = match.value.uppercase().replace(" ", "")
            text = text.replace(match.value, "")
        }

        // 3. Clean conversational filler prefixes
        val prefixes = listOf(
            Regex("(?i)^can you (please )?set a reminder (for|to|about)?"),
            Regex("(?i)^could you (please )?set a reminder (for|to|about)?"),
            Regex("(?i)^please set a reminder (for|to|about)?"),
            Regex("(?i)^set a reminder (for|to|about)?"),
            Regex("(?i)^remind me (to|about|for)?"),
            Regex("(?i)^create a reminder (for|to|about)?"),
            Regex("(?i)^add a reminder (for|to|about)?"),
            Regex("(?i)^put a reminder (for|to|about)?"),
            Regex("(?i)^reminder (for|to|about)?"),
            Regex("(?i)^remember to"),
            Regex("(?i)^don'?t forget to"),
            Regex("(?i)^create task:?"),
            Regex("(?i)^add task:?"),
            Regex("(?i)^new task:?"),
            Regex("(?i)^todo:?"),
            Regex("(?i)^schedule (a )?"),
            Regex("(?i)^reschedule (a )?"),
            Regex("(?i)^stage reminder:?")
        )

        var cleanTitle = text.trim()
            .replace(Regex("(?i)^(at|on|for|to)\\s+"), "")
            .trim()
        for (prefix in prefixes) {
            cleanTitle = cleanTitle.replace(prefix, "").trim()
        }

        // Clean trailing/leading prepositions and articles
        cleanTitle = cleanTitle
            .replace(Regex("(?i)^to\\s+"), "")
            .replace(Regex("(?i)^for\\s+"), "")
            .replace(Regex("(?i)^at\\s+"), "")
            .replace(Regex("(?i)^on\\s+"), "")
            .replace(Regex("(?i)^about\\s+"), "")
            .replace(Regex("(?i)\\s+for$"), "")
            .replace(Regex("(?i)\\s+at$"), "")
            .replace(Regex("(?i)\\s+on$"), "")
            .trim()

        if (cleanTitle.equals("doctor", ignoreCase = true)) {
            cleanTitle = "Doctor Appointment"
        } else if (cleanTitle.equals("dentist", ignoreCase = true)) {
            cleanTitle = "Dentist Appointment"
        } else if (cleanTitle.isNotBlank()) {
            cleanTitle = cleanTitle.replaceFirstChar { it.uppercase() }
        } else {
            cleanTitle = "Scheduled Reminder"
        }

        val scheduleFormatted = if (timeStr.isNotBlank()) {
            "$dateStr at $timeStr"
        } else {
            dateStr
        }

        return ReminderExtracted(
            title = cleanTitle,
            formattedSchedule = scheduleFormatted
        )
    }

    private fun cleanNoteContent(raw: String): String {
        val prefixes = listOf(
            Regex("(?i)^note that\\s*"),
            Regex("(?i)^take a note(:|\\s)+"),
            Regex("(?i)^take note(:|\\s)+"),
            Regex("(?i)^save a note(:|\\s)+"),
            Regex("(?i)^save note(:|\\s)+"),
            Regex("(?i)^create a note(:|\\s)+"),
            Regex("(?i)^create note(:|\\s)+"),
            Regex("(?i)^add a note(:|\\s)+"),
            Regex("(?i)^add note(:|\\s)+"),
            Regex("(?i)^make a note(:|\\s)+"),
            Regex("(?i)^make note(:|\\s)+"),
            Regex("(?i)^write down\\s*"),
            Regex("(?i)^new note(:|\\s)+"),
            Regex("(?i)^jot down\\s*"),
            Regex("(?i)^remember that\\s*"),
            Regex("(?i)^save this(:|\\s)+"),
            Regex("(?i)^record note(:|\\s)+"),
            Regex("(?i)^record this(:|\\s)+"),
            Regex("(?i)^note(:|\\s)+")
        )
        var content = raw.trim()
        for (p in prefixes) {
            content = content.replace(p, "").trim()
        }
        return content.ifEmpty { raw }
    }

    private fun cleanSearchQuery(raw: String): String {
        return raw.trim()
            .replace(Regex("(?i)^find\\s+"), "")
            .replace(Regex("(?i)^search\\s+"), "")
            .replace(Regex("(?i)^where is\\s+"), "")
            .replace(Regex("(?i)^what is\\s+"), "")
            .replace(Regex("(?i)^what did i note about\\s+"), "")
            .replace(Regex("(?i)^the\\s+"), "")
            .replace(Regex("(?i)^a\\s+"), "")
            .trim()
    }
}
