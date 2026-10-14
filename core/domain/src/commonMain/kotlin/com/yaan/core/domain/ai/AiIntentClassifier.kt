package com.yaan.core.domain.ai

/**
 * Interface for On-Device and Hybrid Intent Classification.
 */
interface AiIntentClassifier {
    suspend fun classifyIntent(rawInput: String): ParsedIntent
}

/**
 * Hybrid Edge Intent Classifier.
 *
 * Implements Layer 2 & 3 of ADR-0006:
 * Evaluates user input using fast local grammar heuristics.
 * If local confidence θ >= confidenceThreshold (default 0.75), execution completes at 0ms.
 * If θ < confidenceThreshold, the intent is flagged for speculative cloud offload.
 */
class HybridEdgeIntentClassifier(
    private val confidenceThreshold: Double = 0.75
) : AiIntentClassifier {

    override suspend fun classifyIntent(rawInput: String): ParsedIntent {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return ParsedIntent(
                type = AiIntentType.NOTE,
                confidence = 0.0,
                entities = emptyMap(),
                rawInput = rawInput,
                requiresCloudCascade = false
            )
        }

        val lower = trimmed.lowercase()

        // 1. Task heuristics: unambiguous verbs, checklist markers, "todo"
        if (lower.startsWith("todo") ||
            trimmed.startsWith("- [ ]") ||
            lower.startsWith("task:") ||
            lower.startsWith("buy ") ||
            lower.startsWith("call ") ||
            lower.startsWith("send ") ||
            lower.startsWith("draft ")
        ) {
            val title = trimmed
                .removePrefix("TODO:")
                .removePrefix("todo:")
                .removePrefix("TODO")
                .removePrefix("- [ ]")
                .trim()

            return ParsedIntent(
                type = AiIntentType.TASK,
                confidence = 0.92,
                entities = mapOf("title" to title.ifEmpty { "New Action Item" }),
                rawInput = rawInput,
                requiresCloudCascade = 0.92 < confidenceThreshold
            )
        }

        // 2. Reminder heuristics: time markers, "remind me", "alert"
        if (lower.contains("remind me") || lower.startsWith("reminder:") || lower.contains("at ") && (lower.contains("am") || lower.contains("pm"))) {
            return ParsedIntent(
                type = AiIntentType.REMINDER,
                confidence = 0.88,
                entities = mapOf("prompt" to trimmed),
                rawInput = rawInput,
                requiresCloudCascade = 0.88 < confidenceThreshold
            )
        }

        // 3. Daily Briefing Query heuristics
        if (lower.contains("briefing") || lower.contains("what's today") || lower.contains("today schedule") || lower.contains("morning brief")) {
            return ParsedIntent(
                type = AiIntentType.BRIEFING_QUERY,
                confidence = 0.95,
                entities = mapOf("scope" to "TODAY"),
                rawInput = rawInput,
                requiresCloudCascade = 0.95 < confidenceThreshold
            )
        }

        // 4. Action proposals: external mutations (e.g. reschedule, email, invite)
        if (lower.startsWith("reschedule ") || lower.startsWith("cancel meeting") || lower.startsWith("invite ")) {
            return ParsedIntent(
                type = AiIntentType.ACTION_PROPOSAL,
                confidence = 0.85,
                entities = mapOf("actionRequest" to trimmed),
                rawInput = rawInput,
                requiresCloudCascade = 0.85 < confidenceThreshold
            )
        }

        // 5. Search queries
        if (lower.startsWith("find ") || lower.startsWith("search ") || lower.startsWith("where is ")) {
            val query = trimmed.removePrefix("find ").removePrefix("search ").removePrefix("where is ").trim()
            return ParsedIntent(
                type = AiIntentType.SEARCH,
                confidence = 0.82,
                entities = mapOf("query" to query),
                rawInput = rawInput,
                requiresCloudCascade = 0.82 < confidenceThreshold
            )
        }

        // 6. Ambiguous / Complex input: lower confidence triggers speculative cloud cascade
        val isComplex = trimmed.split(" ").size > 15 || trimmed.contains("?") || trimmed.contains("compare")
        val confidence = if (isComplex) 0.45 else 0.80

        return ParsedIntent(
            type = AiIntentType.NOTE,
            confidence = confidence,
            entities = mapOf("content" to trimmed),
            rawInput = rawInput,
            requiresCloudCascade = confidence < confidenceThreshold
        )
    }
}
