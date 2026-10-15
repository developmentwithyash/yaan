package com.yaan.core.domain.tool

import kotlinx.serialization.Serializable

/**
 * Execution authority for tools.
 * Invariant: Tools may ONLY be executed by the client. The model or backend server can only PROPOSE calls.
 */
enum class ToolExecutionAuthority {
    CLIENT
}

/**
 * Confirmation policy determining whether an action requires explicit user confirmation
 * or can be auto-approved when the user's intent is unambiguous.
 */
enum class ConfirmationPolicy {
    ALWAYS_CONFIRM,
    AUTO_APPROVE_EXPLICIT_CONFIRM_INFERRED,
    AUTO_APPROVE_READ_ONLY
}

@Serializable
data class ToolParameter(
    val name: String,
    val type: String, // "STRING", "LONG", "BOOLEAN", "INT"
    val description: String,
    val isRequired: Boolean,
    val allowedValues: List<String>? = null,
    val minLength: Int = 0,
    val maxLength: Int = 4096
)

@Serializable
data class ToolDefinition(
    val name: String,
    val description: String,
    val authority: ToolExecutionAuthority = ToolExecutionAuthority.CLIENT,
    val confirmationPolicy: ConfirmationPolicy,
    val isDestructive: Boolean = false,
    val parameters: List<ToolParameter>
)

@Serializable
data class ToolCallProposal(
    val idempotencyKey: String,
    val toolName: String,
    val arguments: Map<String, String>,
    val rationale: String = "",
    val confidence: Float = 1.0f,
    val isExplicitUserRequest: Boolean = true
)

@Serializable
data class ToolValidationResult(
    val isValid: Boolean,
    val requiresConfirmation: Boolean,
    val errors: List<String> = emptyList()
)

/**
 * Canonical Tool Registry implementing all 12 YAAN tool specifications.
 * Enforces schema validation, parameter bounds, destructive action confirmation,
 * and client-only execution authority.
 */
object ToolRegistry {

    val CREATE_REMINDER = ToolDefinition(
        name = "create_reminder",
        description = "Schedules an alarm reminder in the user's local vault and device AlarmManager.",
        confirmationPolicy = ConfirmationPolicy.AUTO_APPROVE_EXPLICIT_CONFIRM_INFERRED,
        isDestructive = false,
        parameters = listOf(
            ToolParameter("title", "STRING", "Title or summary of reminder", isRequired = true, minLength = 1, maxLength = 256),
            ToolParameter("triggerTimeEpochMs", "LONG", "Target epoch millisecond timestamp for reminder", isRequired = true),
            ToolParameter("recurrence", "STRING", "Recurrence rule (e.g. NONE, DAILY, WEEKLY)", isRequired = false, allowedValues = listOf("NONE", "DAILY", "WEEKLY", "MONTHLY")),
            ToolParameter("channel", "STRING", "Notification channel (IN_APP, SYSTEM_ALARM)", isRequired = false, allowedValues = listOf("IN_APP", "SYSTEM_ALARM"))
        )
    )

    val UPDATE_REMINDER = ToolDefinition(
        name = "update_reminder",
        description = "Updates an existing reminder trigger time or title.",
        confirmationPolicy = ConfirmationPolicy.ALWAYS_CONFIRM,
        isDestructive = false,
        parameters = listOf(
            ToolParameter("reminderId", "STRING", "ID of the reminder to update", isRequired = true, minLength = 1, maxLength = 64),
            ToolParameter("newTitle", "STRING", "Updated title", isRequired = false, maxLength = 256),
            ToolParameter("newTriggerTimeEpochMs", "LONG", "Updated trigger epoch ms", isRequired = false),
            ToolParameter("newRecurrence", "STRING", "Updated recurrence", isRequired = false)
        )
    )

    val DELETE_REMINDER = ToolDefinition(
        name = "delete_reminder",
        description = "Permanently deletes a reminder from vault and cancels pending alarms.",
        confirmationPolicy = ConfirmationPolicy.ALWAYS_CONFIRM,
        isDestructive = true,
        parameters = listOf(
            ToolParameter("reminderId", "STRING", "ID of the reminder to delete", isRequired = true, minLength = 1, maxLength = 64)
        )
    )

    val LIST_REMINDERS = ToolDefinition(
        name = "list_reminders",
        description = "Retrieves upcoming and scheduled reminders from local vault.",
        confirmationPolicy = ConfirmationPolicy.AUTO_APPROVE_READ_ONLY,
        isDestructive = false,
        parameters = listOf(
            ToolParameter("limit", "INT", "Maximum reminders to return (1..100)", isRequired = false),
            ToolParameter("includeDismissed", "BOOLEAN", "Include past dismissed reminders", isRequired = false)
        )
    )

    val CREATE_NOTE = ToolDefinition(
        name = "create_note",
        description = "Captures an encrypted note in the local SQLDelight vault.",
        confirmationPolicy = ConfirmationPolicy.AUTO_APPROVE_EXPLICIT_CONFIRM_INFERRED,
        isDestructive = false,
        parameters = listOf(
            ToolParameter("title", "STRING", "Title of the note", isRequired = true, minLength = 1, maxLength = 256),
            ToolParameter("content", "STRING", "Plaintext content to encrypt at rest", isRequired = true, minLength = 1, maxLength = 8192),
            ToolParameter("tag", "STRING", "Organizational category tag", isRequired = false, maxLength = 64)
        )
    )

    val UPDATE_NOTE = ToolDefinition(
        name = "update_note",
        description = "Mutates an existing note's title or content.",
        confirmationPolicy = ConfirmationPolicy.ALWAYS_CONFIRM,
        isDestructive = true,
        parameters = listOf(
            ToolParameter("noteId", "STRING", "ID of the note to update", isRequired = true, minLength = 1, maxLength = 64),
            ToolParameter("newTitle", "STRING", "New note title", isRequired = false, maxLength = 256),
            ToolParameter("newContent", "STRING", "New note content", isRequired = false, maxLength = 8192)
        )
    )

    val DELETE_NOTE = ToolDefinition(
        name = "delete_note",
        description = "Permanently shreds and deletes a note from the local vault.",
        confirmationPolicy = ConfirmationPolicy.ALWAYS_CONFIRM,
        isDestructive = true,
        parameters = listOf(
            ToolParameter("noteId", "STRING", "ID of the note to shred", isRequired = true, minLength = 1, maxLength = 64)
        )
    )

    val SEARCH_NOTES = ToolDefinition(
        name = "search_notes",
        description = "Performs lexical FTS5 vault search across encrypted notes.",
        confirmationPolicy = ConfirmationPolicy.AUTO_APPROVE_READ_ONLY,
        isDestructive = false,
        parameters = listOf(
            ToolParameter("query", "STRING", "Search query terms", isRequired = true, minLength = 1, maxLength = 256),
            ToolParameter("limit", "INT", "Maximum notes to return", isRequired = false)
        )
    )

    val CREATE_TASK = ToolDefinition(
        name = "create_task",
        description = "Stages and saves a commitment task to the local database.",
        confirmationPolicy = ConfirmationPolicy.AUTO_APPROVE_EXPLICIT_CONFIRM_INFERRED,
        isDestructive = false,
        parameters = listOf(
            ToolParameter("title", "STRING", "Task description", isRequired = true, minLength = 1, maxLength = 256),
            ToolParameter("dueTime", "STRING", "Formatted due date or time (e.g. YYYY-MM-DD)", isRequired = false),
            ToolParameter("priority", "STRING", "Priority level", isRequired = false, allowedValues = listOf("LOW", "MEDIUM", "HIGH", "URGENT"))
        )
    )

    val UPDATE_TASK = ToolDefinition(
        name = "update_task",
        description = "Updates task metadata, title, or due time.",
        confirmationPolicy = ConfirmationPolicy.ALWAYS_CONFIRM,
        isDestructive = false,
        parameters = listOf(
            ToolParameter("taskId", "STRING", "ID of the task to update", isRequired = true, minLength = 1, maxLength = 64),
            ToolParameter("newTitle", "STRING", "Updated title", isRequired = false, maxLength = 256),
            ToolParameter("newDueTime", "STRING", "Updated due time", isRequired = false)
        )
    )

    val COMPLETE_TASK = ToolDefinition(
        name = "complete_task",
        description = "Toggles task completion status in local vault.",
        confirmationPolicy = ConfirmationPolicy.AUTO_APPROVE_EXPLICIT_CONFIRM_INFERRED,
        isDestructive = false,
        parameters = listOf(
            ToolParameter("taskId", "STRING", "ID of the task to complete", isRequired = true, minLength = 1, maxLength = 64)
        )
    )

    val GET_PREFERENCES = ToolDefinition(
        name = "get_preferences",
        description = "Retrieves user preferences and configuration settings.",
        confirmationPolicy = ConfirmationPolicy.AUTO_APPROVE_READ_ONLY,
        isDestructive = false,
        parameters = listOf(
            ToolParameter("keys", "STRING", "Comma-separated preference keys", isRequired = false)
        )
    )

    private val ALL_TOOLS: Map<String, ToolDefinition> = listOf(
        CREATE_REMINDER,
        UPDATE_REMINDER,
        DELETE_REMINDER,
        LIST_REMINDERS,
        CREATE_NOTE,
        UPDATE_NOTE,
        DELETE_NOTE,
        SEARCH_NOTES,
        CREATE_TASK,
        UPDATE_TASK,
        COMPLETE_TASK,
        GET_PREFERENCES
    ).associateBy { it.name }

    fun getTool(name: String): ToolDefinition? = ALL_TOOLS[name]

    fun isToolPermitted(name: String): Boolean = ALL_TOOLS.containsKey(name)

    fun listToolNames(): Set<String> = ALL_TOOLS.keys

    /**
     * Validates a proposed tool call against schema bounds, parameter types,
     * and evaluates whether explicit user confirmation is mandatory.
     */
    fun validate(proposal: ToolCallProposal, currentEpochMs: Long = 1792022400000L): ToolValidationResult {
        val tool = getTool(proposal.toolName)
            ?: return ToolValidationResult(
                isValid = false,
                requiresConfirmation = true,
                errors = listOf("Tool '${proposal.toolName}' is not permitted in the YAAN tool registry.")
            )

        val errors = mutableListOf<String>()

        // 1. Validate required parameters
        for (param in tool.parameters) {
            val value = proposal.arguments[param.name]
            if (param.isRequired && (value == null || value.isBlank())) {
                errors.add("Missing required parameter '${param.name}' for tool '${tool.name}'.")
                continue
            }
            if (value != null) {
                if (value.length < param.minLength) {
                    errors.add("Parameter '${param.name}' length ${value.length} is shorter than minimum allowed (${param.minLength}).")
                }
                if (value.length > param.maxLength) {
                    errors.add("Parameter '${param.name}' length ${value.length} exceeds maximum allowed (${param.maxLength}).")
                }
                if (param.allowedValues != null && value !in param.allowedValues) {
                    errors.add("Parameter '${param.name}' value '$value' is not in permitted set: ${param.allowedValues}.")
                }
                if (param.type == "LONG" && value.toLongOrNull() == null) {
                    errors.add("Parameter '${param.name}' must be a valid integer timestamp (got '$value').")
                }
                if (param.type == "INT" && value.toIntOrNull() == null) {
                    errors.add("Parameter '${param.name}' must be an integer (got '$value').")
                }
            }
        }

        // 2. Temporal validation for reminders (prevent past time unless explicit)
        val triggerTimeStr = proposal.arguments["triggerTimeEpochMs"]
        if (triggerTimeStr != null) {
            val triggerTime = triggerTimeStr.toLongOrNull()
            val allowPast = proposal.arguments["allowPast"] == "true"
            if (triggerTime != null && triggerTime < currentEpochMs && !allowPast) {
                errors.add("Cannot schedule reminder for a timestamp in the past ($triggerTime < $currentEpochMs).")
            }
        }

        // 3. Evaluate confirmation requirement
        val requiresConfirmation = when (tool.confirmationPolicy) {
            ConfirmationPolicy.ALWAYS_CONFIRM -> true
            ConfirmationPolicy.AUTO_APPROVE_READ_ONLY -> false
            ConfirmationPolicy.AUTO_APPROVE_EXPLICIT_CONFIRM_INFERRED -> {
                !proposal.isExplicitUserRequest || proposal.confidence < 0.85f
            }
        }

        return ToolValidationResult(
            isValid = errors.isEmpty(),
            requiresConfirmation = requiresConfirmation,
            errors = errors
        )
    }
}
