package com.yaan.app.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TokenBudgetGuardTest {

    @Test
    fun testEstimateTokensHeuristic() {
        assertEquals(0, TokenBudgetGuard.estimateTokens(""))
        assertEquals(1, TokenBudgetGuard.estimateTokens("a"))
        assertEquals(1, TokenBudgetGuard.estimateTokens("abcd"))
        assertEquals(2, TokenBudgetGuard.estimateTokens("abcde"))
        assertEquals(25, TokenBudgetGuard.estimateTokens("A".repeat(100)))
    }

    @Test
    fun testPruneWithinBudgetPreservesAll() {
        val items = listOf(
            ContextItem("sys", "System instructions", ContextItemPriority.SYSTEM_PROMPT),
            ContextItem("query", "What are my tasks?", ContextItemPriority.CURRENT_USER_QUERY)
        )
        val pruned = TokenBudgetGuard.pruneToBudget(items, maxTokens = 100)
        assertEquals(2, pruned.size)
        assertEquals("sys", pruned[0].id)
        assertEquals("query", pruned[1].id)
    }

    @Test
    fun testPruneEnforcesPriorityRetention() {
        // Create context that exceeds budget
        // 1000 tokens each (~4000 chars)
        val chunk = "X".repeat(400) // ~100 tokens
        val sysPrompt = ContextItem("sys", chunk, ContextItemPriority.SYSTEM_PROMPT) // 100 tokens
        val userQuery = ContextItem("query", chunk, ContextItemPriority.CURRENT_USER_QUERY) // 100 tokens
        val note = ContextItem("note", chunk, ContextItemPriority.RETRIEVED_NOTE) // 100 tokens
        val history1 = ContextItem("h1", chunk, ContextItemPriority.HISTORICAL_MESSAGE) // 100 tokens
        val history2 = ContextItem("h2", chunk, ContextItemPriority.HISTORICAL_MESSAGE) // 100 tokens

        // Budget of 250 tokens should retain: sys (100) + query (100) -> 200 tokens
        // and cannot fit note (100), history1 (100), history2 (100)
        val items = listOf(sysPrompt, history1, history2, note, userQuery)
        val pruned = TokenBudgetGuard.pruneToBudget(items, maxTokens = 250)

        assertEquals(2, pruned.size)
        // Original order preserved
        assertEquals("sys", pruned[0].id)
        assertEquals("query", pruned[1].id)
    }

    @Test
    fun testPrunePreservesRecentHistoryOverOldHistory() {
        val chunk = "Y".repeat(40) // ~10 tokens
        val hOld = ContextItem("h_old", chunk, ContextItemPriority.HISTORICAL_MESSAGE)
        val hNew = ContextItem("h_new", chunk, ContextItemPriority.HISTORICAL_MESSAGE)

        val pruned = TokenBudgetGuard.pruneToBudget(listOf(hOld, hNew), maxTokens = 15)
        assertEquals(1, pruned.size)
        assertEquals("h_new", pruned[0].id)
    }
}
