package com.yaan.app.ai

/**
 * Priority weighting for context items.
 * Higher priority items are retained first when context truncation occurs.
 */
enum class ContextItemPriority(val weight: Int) {
    SYSTEM_PROMPT(4),
    CURRENT_USER_QUERY(3),
    RETRIEVED_NOTE(2),
    HISTORICAL_MESSAGE(1)
}

/**
 * Encapsulates an atomic context slice with its text, priority, and token estimate.
 */
data class ContextItem(
    val id: String,
    val text: String,
    val priority: ContextItemPriority,
    val estimatedTokens: Int = TokenBudgetGuard.estimateTokens(text)
)

/**
 * Fast, deterministic token budget manager and context truncation guard.
 * Satisfies Issue #45: Enforces max token limits to prevent context overflow.
 */
object TokenBudgetGuard {
    const val DEFAULT_MAX_TOKENS = 4000
    const val CHARS_PER_TOKEN = 4

    /**
     * Fast heuristic token counter (~4 characters per token).
     */
    fun estimateTokens(text: String): Int {
        if (text.isEmpty()) return 0
        return maxOf(1, (text.length + CHARS_PER_TOKEN - 1) / CHARS_PER_TOKEN)
    }

    /**
     * Prunes a list of ContextItems to strictly fit within maxTokens.
     * Retains items based on priority (System Prompt > Current Query > Retrieved Notes > History).
     * Preserves original relative temporal ordering among retained items.
     */
    fun pruneToBudget(
        items: List<ContextItem>,
        maxTokens: Int = DEFAULT_MAX_TOKENS
    ): List<ContextItem> {
        val totalTokens = items.sumOf { it.estimatedTokens }
        if (totalTokens <= maxTokens) return items

        // Pair with index to preserve stability and prefer newer items among equal priority
        val indexed = items.mapIndexed { index, item -> index to item }
        val sortedByImportance = indexed.sortedWith(
            compareByDescending<Pair<Int, ContextItem>> { it.second.priority.weight }
                .thenByDescending { it.first }
        )

        var consumed = 0
        val retainedIndices = mutableSetOf<Int>()

        for ((index, item) in sortedByImportance) {
            if (consumed + item.estimatedTokens <= maxTokens) {
                consumed += item.estimatedTokens
                retainedIndices.add(index)
            }
        }

        return items.filterIndexed { index, _ -> index in retainedIndices }
    }
}
