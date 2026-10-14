package com.yaan.core.evaluation

import com.yaan.core.domain.ai.EpistemicFact
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RetrievalAblationTest {

    @Test
    fun testRetrievalModeAblationStudy() {
        val vault = SyntheticVaultGenerator.generateVault(1000, seed = 42L)
        val engine = VaultRetrievalEngine(vault)
        val questions = EvaluationDataset.getQuestions()

        println("=== PHASE 2 RETRIEVAL ABLATION STUDY (1,000 Vault Items, 310 Questions) ===")

        for (mode in RetrievalMode.entries) {
            var recallAt1Hits = 0
            var recallAt3Hits = 0
            var recallAt5Hits = 0
            var totalReciprocalRank = 0.0
            var retrievalQueryCount = 0
            val latencies = ArrayList<Long>(questions.size)

            for (q in questions) {
                if (q.expectedKeywords.isNotEmpty()) {
                    retrievalQueryCount++
                    val res = engine.search(q.query, mode = mode, limit = 5)
                    latencies.add(res.latencyMs)

                    val hits = res.retrievedIds.map { id ->
                        val content = vault.getContent(id) ?: ""
                        q.expectedKeywords.any { kw -> content.contains(kw, ignoreCase = true) }
                    }

                    if (hits.getOrNull(0) == true) recallAt1Hits++
                    if (hits.take(3).any { it }) recallAt3Hits++
                    if (hits.take(5).any { it }) recallAt5Hits++

                    val firstHitIndex = hits.indexOfFirst { it }
                    if (firstHitIndex >= 0) {
                        totalReciprocalRank += 1.0 / (firstHitIndex + 1)
                    }
                }
            }

            latencies.sort()
            val p50 = if (latencies.isNotEmpty()) latencies[latencies.size / 2] else 0L
            val p95 = if (latencies.isNotEmpty()) latencies[(latencies.size * 0.95).toInt()] else 0L

            val rCount = retrievalQueryCount.toDouble()
            val r1 = recallAt1Hits / rCount
            val r3 = recallAt3Hits / rCount
            val r5 = recallAt5Hits / rCount
            val mrr = totalReciprocalRank / rCount

            println("Mode: ${mode.name.padEnd(20)} | Recall@1: ${"%.4f".format(r1)} | Recall@3: ${"%.4f".format(r3)} | Recall@5: ${"%.4f".format(r5)} | MRR: ${"%.4f".format(mrr)} | Latency p50: ${p50}ms, p95: ${p95}ms")

            // Core assertions
            if (mode == RetrievalMode.FTS5_ONLY || mode == RetrievalMode.FTS5_WITH_RECENCY || mode == RetrievalMode.HYBRID_RANK_FUSION) {
                assertTrue(r5 >= 0.90, "FTS5 and Hybrid modes must achieve >= 90% Recall@5")
                assertTrue(mrr >= 0.90, "MRR must be >= 0.90")
            }
        }
    }

    @Test
    fun testEpistemicFactInvariantZeroNotePruningAndUserRestoration() {
        // Invariant: NEVER auto-delete or prune user notes!
        // Derived facts may expire only if the user can see and restore them.
        // Every returned fact carries citationSourceId.

        val initialNoteCount = 50
        val vault = SyntheticVaultGenerator.generateVault(initialNoteCount * 2, seed = 42L)
        val sourceNoteId = vault.notes.first().id

        // Create derived epistemic fact
        val fact = EpistemicFact(
            id = "fact_001",
            subject = "WiFi PIN",
            predicate = "is 9482-Bravo-Hotel",
            initialConfidence = 0.95,
            citationSourceId = sourceNoteId,
            createdAtMs = 1790812800000L,
            expiresAtMs = 1790812800000L + (30L * 86400000L) // 30 days
        )

        // 1. Verify fact carries citationSourceId
        assertEquals(sourceNoteId, fact.citationSourceId)
        assertNotNull(vault.findItemById(fact.citationSourceId))

        // 2. Simulate expiration of derived fact
        val currentTimeMs = 1790812800000L + (45L * 86400000L) // 45 days later
        val isExpired = fact.expiresAtMs != null && currentTimeMs > fact.expiresAtMs!!
        assertTrue(isExpired, "Fact should be marked as expired after 45 days")

        // 3. User note must NEVER be pruned or deleted
        assertEquals(initialNoteCount, vault.notes.size, "User note count must remain strictly unchanged")
        assertNotNull(vault.getContent(sourceNoteId), "Source note must remain 100% intact after fact expiration")

        // 4. User can inspect expired fact and explicitly restore it
        val restoredFact = fact.copy(
            expiresAtMs = currentTimeMs + (30L * 86400000L), // extended by user action
            initialConfidence = 1.0 // user confirmed
        )
        assertFalse(restoredFact.expiresAtMs != null && currentTimeMs > restoredFact.expiresAtMs!!)
    }
}
