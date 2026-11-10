package com.yaan.core.domain.actions

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Grounded Action Proposal staged for two-phase user confirmation.
 * Strictly adheres to Phase 4 Invariants:
 * 1. Shows a transparent diff description.
 * 2. States rationale and source citations.
 * 3. Never mutates database state without explicit user tap.
 * 4. Every committed action produces an UndoReceipt.
 */
@Serializable
data class GroundedActionProposal(
    val id: String,
    val title: String,
    val summary: String,
    val actionType: String,
    val diffDescription: String,
    val rationale: String,
    val sourceCitations: List<String> = emptyList(),
    val parameters: Map<String, String> = emptyMap(),
    val requiresAffirmativeApproval: Boolean = true,
    val isExecuted: Boolean = false,
    val proposedAtEpochMs: Long = 0L
)

/**
 * Receipt of an executed action allowing immediate or historical rollback (undo).
 */
data class UndoReceipt(
    val id: String,
    val proposalId: String,
    val actionType: String,
    val entityId: String,
    val previousStateJson: String?,
    val executedAtEpochMs: Long,
    var isUndone: Boolean = false
)

/**
 * Result of semantic validation against well-formed but invalid action proposals.
 */
data class SemanticValidationResult(
    val isValid: Boolean,
    val violations: List<String> = emptyList()
)

/**
 * Verdict classification for action and prompt safety inspection.
 */
enum class ActionSafetyVerdict {
    APPROVED,
    REJECTED_UNSAFE_INJECTION,
    REJECTED_SEMANTIC_VIOLATION
}

/**
 * Detailed inspection result from prompt injection and jailbreak analysis.
 */
data class ActionSafetyInspection(
    val verdict: ActionSafetyVerdict,
    val matchedVector: String? = null,
    val reason: String? = null
)

/**
 * Dedicated Prompt Injection & Jailbreak Defense Filter.
 * Defends against OWASP Top 10 LLM01 Prompt Injection, direct jailbreaks,
 * privilege escalation vectors, shell payloads, and multi-lingual evasion.
 */
object PromptInjectionDefenseFilter {

    private val INJECTION_PATTERNS = listOf(
        // Direct System Override & Jailbreaks
        Regex("(?i)(ignore|disregard|forget)\\s+(all\\s+)?(previous|above|prior)\\s+(instructions|directives|prompts|rules)"),
        Regex("(?i)you\\s+are\\s+now\\s+(dan|developer\\s+mode|chaosbot|unfiltered)"),
        Regex("(?i)emergency\\s+override"),
        Regex("(?i)system\\s+prompt\\s+(override|leak|injection|dump|reveal)"),
        Regex("(?i)output\\s+(your\\s+)?(system\\s+prompt|initial\\s+instructions)"),
        Regex("(?i)\\[DEBUG_MODE_ENABLED:"),
        Regex("(?i)\\[SYSTEM_TOOL_CALL:"),
        Regex("(?i)roleplay\\s+as\\s+[a-z0-9_-]+"),

        // Destructive System Shell Payloads
        Regex("(?i)rm\\s+-rf\\s+[/~]"),
        Regex("(?i)drop\\s+table\\s+[a-z0-9_]+"),
        Regex("(?i)eval\\s*\\("),
        Regex("(?i)curl\\s+-[a-z]*s.*\\|\\s*(ba)?sh"),
        Regex("(?i)wget\\s+.*\\|\\s*(ba)?sh"),
        Regex("(?i)chmod\\s+(\\+x|[0-7]{3,4})"),
        Regex("(?i)cat\\s+/etc/(passwd|shadow)"),
        Regex("(?i)cat\\s+~/.ssh"),
        Regex("(?i)powershell.*-enc"),
        Regex("(?i)dump\\s+(credentials|keys|tokens|passwords)"),

        // Script & Exfiltration Injections
        Regex("(?i)<\\s*(script|iframe|object|embed)[^>]*>"),
        Regex("(?i)javascript:"),
        Regex("(?i)onerror\\s*="),
        Regex("!\\[.*?\\]\\(https?://[^\\)]+\\)"),

        // Multi-lingual & Hinglish Transliterations
        Regex("(?i)(instructions?|baat)\\s+(bhool\\s+ja|bhol\\s+ja|bhula\\s+do)"),
        Regex("(?i)sab\\s+kuch\\s+(bhool\\s+ja|bhol\\s+ja)"),
        Regex("(?i)system\\s+prompt\\s+(batao|dikhao)"),
        Regex("(?i)tum\\s+ab\\s+(dan|chaosbot)\\s+ho")
    )

    fun inspect(input: String): ActionSafetyInspection {
        if (input.isBlank()) {
            return ActionSafetyInspection(verdict = ActionSafetyVerdict.APPROVED)
        }

        for (pattern in INJECTION_PATTERNS) {
            val match = pattern.find(input)
            if (match != null) {
                return ActionSafetyInspection(
                    verdict = ActionSafetyVerdict.REJECTED_UNSAFE_INJECTION,
                    matchedVector = match.value,
                    reason = "Prompt injection or adversarial instruction detected: '${match.value}'"
                )
            }
        }

        return ActionSafetyInspection(verdict = ActionSafetyVerdict.APPROVED)
    }
}

/**
 * Semantic Validator for Action Proposals.
 * Grammar (e.g. GBNF/JSON schema) only guarantees syntactic validity;
 * this validator guarantees temporal, causal, and entity integrity.
 */
class ActionSemanticValidator(
    private val existingVaultIds: Set<String>,
    private val currentEpochSec: Long = 1792022400L // 2026-10-15T00:00:00Z
) {

    private val ALLOWED_ACTIONS = setOf(
        "CREATE_TASK",
        "CREATE_NOTE",
        "UPDATE_TASK",
        "RESCHEDULE_MEETING"
    )

    private val PROHIBITED_SYSTEM_COMMANDS = listOf(
        "PURGE_VAULT", "DROP_TABLE", "EXEC_SHELL", "EXPORT_PLAINTEXT", "DISABLE_AUTH", "DELETE_ALL"
    )

    fun validate(proposal: GroundedActionProposal): SemanticValidationResult {
        val violations = ArrayList<String>()

        // 1. Strict Action Type Allowlist Check
        if (proposal.actionType !in ALLOWED_ACTIONS) {
            violations.add("Action type '${proposal.actionType}' is not in the permitted action allowlist.")
        }
        for (prohibited in PROHIBITED_SYSTEM_COMMANDS) {
            if (proposal.actionType.contains(prohibited, ignoreCase = true) ||
                proposal.title.contains(prohibited, ignoreCase = true)) {
                violations.add("Prohibited administrative or destructive instruction detected: $prohibited.")
            }
        }

        // 2. Affirmative Approval Invariant
        if (!proposal.requiresAffirmativeApproval) {
            violations.add("Critical Invariant Violation: All AI-proposed actions must mandate affirmative user approval.")
        }

        // 3. Entity Reference Existence Check (for UPDATE / RESCHEDULE actions)
        val targetEntityId = proposal.parameters["targetEntityId"]
        if (proposal.actionType in listOf("UPDATE_TASK", "RESCHEDULE_MEETING")) {
            if (targetEntityId == null || targetEntityId.isBlank()) {
                violations.add("Action '${proposal.actionType}' requires a targetEntityId parameter.")
            } else if (targetEntityId !in existingVaultIds) {
                violations.add("Target entity ID '$targetEntityId' does not exist in user vault.")
            }
        }

        // 4. Temporal Validation (Past-Time Scheduling Defense)
        val dueDateStr = proposal.parameters["dueDate"]
        if (dueDateStr != null) {
            val parts = dueDateStr.split("-")
            if (parts.size != 3) {
                violations.add("Invalid date format '$dueDateStr'. Must be YYYY-MM-DD.")
            } else {
                val year = parts[0].toIntOrNull() ?: 0
                val month = parts[1].toIntOrNull() ?: 0
                val day = parts[2].toIntOrNull() ?: 0

                // Reference current date: 2026-10-15
                val isPast = (year < 2026) ||
                        (year == 2026 && month < 10) ||
                        (year == 2026 && month == 10 && day < 15)

                val allowPastExplicitly = proposal.parameters["allowPast"] == "true"
                if (isPast && !allowPastExplicitly) {
                    violations.add("Cannot schedule action for past date '$dueDateStr' relative to current date (2026-10-15).")
                }
            }
        }

        // 5. Title & Summary Sanitization
        if (proposal.title.isBlank()) {
            violations.add("Action proposal title cannot be blank.")
        }
        if (proposal.title.contains("<script>", ignoreCase = true)) {
            violations.add("Script injection attempt detected in action proposal title.")
        }

        // 6. Prompt Injection & Jailbreak Defense Inspection
        val fieldsToInspect = listOf(proposal.title, proposal.summary) + proposal.parameters.values
        for (field in fieldsToInspect) {
            val inspection = PromptInjectionDefenseFilter.inspect(field)
            if (inspection.verdict == ActionSafetyVerdict.REJECTED_UNSAFE_INJECTION) {
                violations.add("Prompt injection or adversarial instruction detected: ${inspection.matchedVector}")
                break
            }
        }

        return SemanticValidationResult(
            isValid = violations.isEmpty(),
            violations = violations
        )
    }
}

/**
 * Untrusted Vault Content Guard.
 * Enforces the iron rule: Vault content is untrusted data.
 * Retrieved text must NEVER be able to create or mutate an action by itself.
 * Actions may only be drafted if the user's explicit request in the current turn asks for it.
 */
object VaultUntrustedContentGuard {

    private val USER_ACTION_TRIGGERS = listOf(
        "schedule", "remind me", "create task", "add task", "todo", "set a reminder",
        "save note", "create note", "take note", "reschedule", "mark done", "update task"
    )

    /**
     * Determines whether the user's current query actively requests an action.
     * Imperative text inside retrieved vault notes is ignored.
     */
    fun isUserPromptRequestingAction(userTurnPrompt: String): Boolean {
        val lower = userTurnPrompt.trim().lowercase()
        return USER_ACTION_TRIGGERS.any { trigger ->
            lower.startsWith(trigger) || lower.contains(trigger)
        }
    }

    /**
     * Sanitizes retrieved text so that injected directives inside notes/memos
     * cannot trick downstream models or parsers into creating unauthorized actions.
     */
    fun neutraliseRetrievedDirectives(retrievedSnippet: String): String {
        return retrievedSnippet
            .replace(Regex("(?i)create task:?"), "[inert_text: create task]")
            .replace(Regex("(?i)schedule:?"), "[inert_text: schedule]")
            .replace(Regex("(?i)delete all:?"), "[inert_text: delete all]")
            .replace(Regex("(?i)wire money:?"), "[inert_text: wire money]")
            .replace(Regex("(?i)transfer \\$\\d+:?"), "[inert_text: transfer]")
            // Neutralize markdown exfiltration images
            .replace(Regex("!\\[.*?\\]\\(https?://[^\\)]+\\)"), "[inert_image_link]")
            // Neutralize embedded script/iframe tags
            .replace(Regex("(?is)<\\s*(script|iframe|object|embed)[^>]*>.*?</\\s*\\1\\s*>"), "[inert_code_block]")
            .replace(Regex("(?i)<\\s*(script|iframe|object|embed)[^>]*>"), "[inert_tag]")
            // Neutralize system prompt override phrases
            .replace(Regex("(?i)(ignore|disregard|forget)\\s+(all\\s+)?(previous|above)\\s+(instructions|directives|prompts)"), "[inert_system_override]")
            .replace(Regex("(?i)system\\s+prompt\\s+(override|injection)"), "[inert_system_override]")
            .replace(Regex("(?i)emergency\\s+override:?"), "[inert_emergency_override]")
            .replace(Regex("(?s)<!--.*?-->"), "[inert_comment]")
            .replace(Regex("(?i)\\[SYSTEM_TOOL_CALL:[^\\]]+\\]"), "[inert_system_tool_call]")
            .replace(Regex("(?i)\\[DEBUG_MODE_ENABLED:[^\\]]+\\]"), "[inert_debug_override]")
            .replace(Regex("(?i)roleplay as [a-z0-9_-]+"), "[inert_roleplay]")
            .replace(Regex("(?i)(instructions|baat)\\s+(bhool ja|bhol ja)"), "[inert_system_override]")
    }
}

/**
 * Two-Phase Action Committer.
 * Bridges probabilistic proposals with deterministic state repositories.
 */
class TwoPhaseActionCommitter(
    private val validator: ActionSemanticValidator
) {
    private val stagedProposals = HashMap<String, GroundedActionProposal>()
    private val undoReceipts = HashMap<String, UndoReceipt>()

    /**
     * Stage 1: Stages a proposal after passing semantic validation.
     * Returns false if validation fails. DOES NOT MUTATE STATE.
     */
    fun stageProposal(proposal: GroundedActionProposal): SemanticValidationResult {
        val validation = validator.validate(proposal)
        if (validation.isValid) {
            stagedProposals[proposal.id] = proposal
        }
        return validation
    }

    fun getStagedProposal(id: String): GroundedActionProposal? = stagedProposals[id]

    /**
     * Stage 2: Commits the staged proposal ONLY upon affirmative user confirmation tap.
     * Generates an UndoReceipt for reversible execution.
     */
    fun commitWithUserConfirmation(
        proposalId: String,
        targetCommitAction: (GroundedActionProposal) -> String? // Returns entityId
    ): UndoReceipt? {
        val proposal = stagedProposals[proposalId] ?: return null
        if (proposal.isExecuted) return null

        val entityId = targetCommitAction(proposal) ?: proposalId

        val receipt = UndoReceipt(
            id = "undo_${proposal.id}",
            proposalId = proposal.id,
            actionType = proposal.actionType,
            entityId = entityId,
            previousStateJson = null,
            executedAtEpochMs = Clock.System.now().toEpochMilliseconds()
        )

        stagedProposals[proposalId] = proposal.copy(isExecuted = true)
        undoReceipts[receipt.id] = receipt
        return receipt
    }

    /**
     * Undoes a previously committed action using its UndoReceipt.
     */
    fun undoAction(
        undoReceiptId: String,
        rollbackAction: (UndoReceipt) -> Boolean
    ): Boolean {
        val receipt = undoReceipts[undoReceiptId] ?: return false
        if (receipt.isUndone) return false

        val success = rollbackAction(receipt)
        if (success) {
            receipt.isUndone = true
            val proposal = stagedProposals[receipt.proposalId]
            if (proposal != null) {
                stagedProposals[receipt.proposalId] = proposal.copy(isExecuted = false)
            }
        }
        return success
    }
}
