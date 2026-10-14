package com.yaan.core.domain.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AiArchitectureTest {

    private val classifier = HybridEdgeIntentClassifier(confidenceThreshold = 0.75)
    private val guardrailGate = StrictActionGuardrailGate()

    @Test
    fun testHighConfidenceTaskResolvesLocallyWithoutCloudCascade() = kotlinx.coroutines.test.runTest {
        val input = "TODO: Review the Q3 Product Architecture Deck"
        val result = classifier.classifyIntent(input)

        assertEquals(AiIntentType.TASK, result.type)
        assertTrue(result.confidence >= 0.75, "Confidence should exceed threshold for unambiguous task")
        assertFalse(result.requiresCloudCascade, "High-confidence task must resolve locally at 0ms")
        assertEquals("Review the Q3 Product Architecture Deck", result.entities["title"])
    }

    @Test
    fun testDailyBriefingQueryResolvesLocally() = kotlinx.coroutines.test.runTest {
        val input = "Show me today's morning briefing schedule"
        val result = classifier.classifyIntent(input)

        assertEquals(AiIntentType.BRIEFING_QUERY, result.type)
        assertTrue(result.confidence >= 0.75)
        assertFalse(result.requiresCloudCascade)
    }

    @Test
    fun testComplexAmbiguousInputTriggersCloudCascade() = kotlinx.coroutines.test.runTest {
        val complexInput = "Can you compare the trade-offs between our current vector indexing strategy and an on-device HNSW implementation for low memory mobile devices?"
        val result = classifier.classifyIntent(complexInput)

        assertTrue(result.confidence < 0.75, "Complex query should have lower local confidence")
        assertTrue(result.requiresCloudCascade, "Low confidence query must trigger speculative cloud cascade")
    }

    @Test
    fun testEpistemicDecayFormulaOverTime() {
        val nowMs = 1728900000000L
        val thirtyDaysMs = 30 * 86_400_000L
        val ninetyDaysMs = 90 * 86_400_000L

        val fact = EpistemicFact(
            id = "fact_01",
            subject = "Sarah",
            predicate = "prefers morning 9am meetings",
            initialConfidence = 0.90,
            citationSourceId = "memo_101",
            createdAtMs = nowMs,
            halfLifeDays = 30.0
        )

        // Day 0: No decay
        val c0 = EpistemicDecayCalculator.calculateDecayedConfidence(fact, nowMs)
        assertEquals(0.90, c0, 0.001)
        assertFalse(EpistemicDecayCalculator.shouldPrune(fact, nowMs))

        // Day 30: 1 half-life elapsed -> 0.90 * 0.5 = 0.45
        val c30 = EpistemicDecayCalculator.calculateDecayedConfidence(fact, nowMs + thirtyDaysMs)
        assertEquals(0.45, c30, 0.01)
        assertFalse(EpistemicDecayCalculator.shouldPrune(fact, nowMs + thirtyDaysMs))

        // Day 90: 3 half-lives elapsed -> 0.90 * 0.125 = 0.1125 (< 0.20 threshold)
        val c90 = EpistemicDecayCalculator.calculateDecayedConfidence(fact, nowMs + ninetyDaysMs)
        assertTrue(c90 < EpistemicDecayCalculator.PRUNE_CONFIDENCE_THRESHOLD)
        assertTrue(EpistemicDecayCalculator.shouldPrune(fact, nowMs + ninetyDaysMs))
    }

    @Test
    fun testEpistemicFactWithExplicitExpirationZeroesImmediately() {
        val nowMs = 1728900000000L
        val expiresAtMs = nowMs + 3600_000L // 1 hour

        val transientFact = EpistemicFact(
            id = "fact_flight",
            subject = "Gate",
            predicate = "B22",
            initialConfidence = 0.95,
            citationSourceId = "sms_flight",
            createdAtMs = nowMs,
            halfLifeDays = 7.0,
            expiresAtMs = expiresAtMs
        )

        // Before expiry
        val cBefore = EpistemicDecayCalculator.calculateDecayedConfidence(transientFact, nowMs + 1800_000L)
        assertTrue(cBefore > 0.8)
        assertFalse(EpistemicDecayCalculator.shouldPrune(transientFact, nowMs + 1800_000L))

        // After expiry
        val cAfter = EpistemicDecayCalculator.calculateDecayedConfidence(transientFact, expiresAtMs + 1000L)
        assertEquals(0.0, cAfter)
        assertTrue(EpistemicDecayCalculator.shouldPrune(transientFact, expiresAtMs + 1000L))
    }

    @Test
    fun testActionGuardrailBlocksUnapprovedMutations() {
        val proposal = ActionProposalPayload(
            id = "prop_cancel_sync",
            title = "Reschedule 2:00 PM Meeting",
            actionType = "CALENDAR_RESCHEDULE",
            parameters = mapOf("eventId" to "evt_123", "newTime" to "3:00 PM"),
            affirmativeApprovalRequired = true,
            isAffirmativelyApproved = false,
            stagedAtMs = 1728900000000L
        )

        // Blocked when unapproved
        assertFalse(guardrailGate.canExecute(proposal), "Unapproved action proposal must be blocked by guardrail")

        // Allowed once user affirmatively approves
        val approved = proposal.copy(isAffirmativelyApproved = true)
        assertTrue(guardrailGate.canExecute(approved), "Affirmatively approved proposal must be permitted to execute")
    }
}
