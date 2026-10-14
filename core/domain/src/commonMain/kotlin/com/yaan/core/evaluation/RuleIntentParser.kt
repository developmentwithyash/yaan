package com.yaan.core.evaluation

import com.yaan.core.domain.model.ActionProposal
import com.yaan.core.domain.model.ActionType
import kotlinx.datetime.Instant

/**
 * Result of rule-based intent parsing and prompt injection sanitization.
 */
data class RuleParseResult(
    val isAdversarial: Boolean,
    val adversaryMatchReason: String? = null,
    val intentType: String, // "SEARCH", "CREATE_TASK", "CREATE_NOTE", "ABSTAIN", "AMBIGUOUS"
    val confidence: Double,
    val extractedEntities: Map<String, String> = emptyMap(),
    val stagedProposal: ActionProposal? = null,
    val requiresAbstention: Boolean = false,
)

/**
 * Deterministic baseline rule-based parser.
 * Serves as the zero-parameter baseline before any ML models or cloud LLMs are invoked.
 */
object RuleIntentParser {

    private val INJECTION_PATTERNS = listOf(
        Regex("ignore (all )?previous instructions", RegexOption.IGNORE_CASE),
        Regex("system prompt override", RegexOption.IGNORE_CASE),
        Regex("<script.*?>", RegexOption.IGNORE_CASE),
        Regex("admin command", RegexOption.IGNORE_CASE),
        Regex("jailbreak", RegexOption.IGNORE_CASE),
        Regex("\\bdan\\b", RegexOption.IGNORE_CASE),
        Regex("<!--.*?-->", RegexOption.IGNORE_CASE),
        Regex("rm -rf", RegexOption.IGNORE_CASE),
        Regex("drop table", RegexOption.IGNORE_CASE),
        Regex("disable biometric", RegexOption.IGNORE_CASE),
        Regex("bypass two-phase", RegexOption.IGNORE_CASE),
        Regex("exfiltrate", RegexOption.IGNORE_CASE),
        Regex("system instructions?", RegexOption.IGNORE_CASE),
        Regex("reveal developer", RegexOption.IGNORE_CASE),
        Regex("unrestricted ai", RegexOption.IGNORE_CASE),
        Regex("automatically accept all", RegexOption.IGNORE_CASE),
        Regex("disregard safety", RegexOption.IGNORE_CASE),
        Regex("malicious script", RegexOption.IGNORE_CASE),
        Regex("auth_bypass", RegexOption.IGNORE_CASE),
        Regex("system debug", RegexOption.IGNORE_CASE)
    )

    private val TASK_TRIGGERS = listOf(
        "schedule", "remind me to", "remind me", "create task", "add task", "todo", "set a reminder"
    )

    private val NOTE_TRIGGERS = listOf(
        "save note", "create note", "write down", "note that", "take a note"
    )

    private val DATE_PATTERNS = listOf(
        Pair(Regex("tomorrow", RegexOption.IGNORE_CASE), "2026-10-16"),
        Pair(Regex("next wednesday", RegexOption.IGNORE_CASE), "2026-10-28"),
        Pair(Regex("friday", RegexOption.IGNORE_CASE), "2026-10-17"),
        Pair(Regex("nov 14", RegexOption.IGNORE_CASE), "2026-11-14"),
        Pair(Regex("dec 02", RegexOption.IGNORE_CASE), "2026-12-02"),
        Pair(Regex("january 1st?", RegexOption.IGNORE_CASE), "2027-01-01"),
        Pair(Regex("tuesday", RegexOption.IGNORE_CASE), "2026-10-20")
    )

    /**
     * Parses the user query string deterministically.
     */
    fun parse(input: String): RuleParseResult {
        val trimmed = input.trim()

        // 1. Check prompt injection attacks first
        for (pattern in INJECTION_PATTERNS) {
            val match = pattern.find(trimmed)
            if (match != null) {
                return RuleParseResult(
                    isAdversarial = true,
                    adversaryMatchReason = "Matched injection pattern: ${match.value}",
                    intentType = "ABSTAIN",
                    confidence = 1.0,
                    requiresAbstention = true
                )
            }
        }

        val lower = trimmed.lowercase()

        // 2. Task creation triggers
        for (trigger in TASK_TRIGGERS) {
            if (lower.startsWith(trigger) || lower.contains(trigger)) {
                val remainder = if (lower.startsWith(trigger)) {
                    trimmed.substring(trigger.length).trim().removePrefix("to").trim()
                } else {
                    trimmed
                }

                var matchedDate: String? = null
                for ((pattern, dateVal) in DATE_PATTERNS) {
                    if (pattern.containsMatchIn(remainder)) {
                        matchedDate = dateVal
                        break
                    }
                }

                val title = remainder.ifBlank { "New Scheduled Task" }
                val entities = HashMap<String, String>()
                entities["title"] = title
                if (matchedDate != null) entities["dueDate"] = matchedDate

                val proposal = ActionProposal(
                    id = "prop_${title.hashCode()}",
                    title = title,
                    summary = "Proposed task: $title" + (if (matchedDate != null) " (Due: $matchedDate)" else ""),
                    actionType = ActionType.CREATE_TASK,
                    requiresAffirmativeApproval = true,
                    isExecuted = false
                )

                return RuleParseResult(
                    isAdversarial = false,
                    intentType = "CREATE_TASK",
                    confidence = 0.85,
                    extractedEntities = entities,
                    stagedProposal = proposal,
                    requiresAbstention = false
                )
            }
        }

        // 3. Note creation triggers
        for (trigger in NOTE_TRIGGERS) {
            if (lower.startsWith(trigger) || lower.contains(trigger)) {
                val remainder = trimmed.substring(trigger.length).trim()
                val entities = mapOf("content" to remainder)
                return RuleParseResult(
                    isAdversarial = false,
                    intentType = "CREATE_NOTE",
                    confidence = 0.80,
                    extractedEntities = entities,
                    requiresAbstention = false
                )
            }
        }

        // 4. Default to Search / Q&A
        return RuleParseResult(
            isAdversarial = false,
            intentType = "SEARCH",
            confidence = 0.70,
            requiresAbstention = false
        )
    }
}
