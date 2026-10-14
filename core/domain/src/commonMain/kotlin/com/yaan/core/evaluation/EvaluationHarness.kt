package com.yaan.core.evaluation

import kotlin.math.roundToInt

/**
 * End-to-end programmatic evaluation harness for Ask YAAN.
 * Executes evaluations without external network dependencies, ensuring 100% CI reproducibility.
 */
class EvaluationHarness(
    val vault: SyntheticVault,
    private val retriever: LexicalBaselineSearch = LexicalBaselineSearch(vault),
    private val minConfidenceThreshold: Double = 0.50
) {

    /**
     * Evaluates a full test suite of [questions].
     */
    fun evaluate(questions: List<EvaluationQuestion>): MetricReport {
        var recallAt1Hits = 0
        var recallAt3Hits = 0
        var recallAt5Hits = 0
        var totalReciprocalRank = 0.0
        var retrievalQueryCount = 0

        var trueAbstain = 0
        var falseAbstain = 0
        var falseNegativeAbstain = 0
        var trueNegativeAbstain = 0

        var validCitationsCount = 0
        var totalCitationsCount = 0

        var actionProposalsEmitted = 0
        var actionSchemaValidCount = 0
        var actionSemanticValidCount = 0

        var adversarialTotal = 0
        var adversarialDefended = 0

        val latencies = ArrayList<Long>(questions.size)

        for (q in questions) {
            val startTime = currentTimeNanos()

            // 1. Rule / intent parse
            val parseResult = RuleIntentParser.parse(q.query)

            // Check adversarial defense
            if (q.category == EvaluationCategory.ADVERSARIAL_INJECTION) {
                adversarialTotal++
                if (parseResult.isAdversarial) {
                    adversarialDefended++
                }
            }

            // 2. Retrieval stage
            val retrievalResult = retriever.search(q.query, limit = 5)

            // 3. Simulated Answer / Staging pipeline based on baseline rules
            val isAbstained: Boolean
            val citedIds: List<String>

            if (parseResult.requiresAbstention) {
                isAbstained = true
                citedIds = emptyList()
            } else if (retrievalResult.scores.isEmpty() || (retrievalResult.scores.firstOrNull() ?: 0.0) < minConfidenceThreshold) {
                // If query is an action trigger like "schedule...", it doesn't need retrieval hits to propose
                if (parseResult.intentType == "CREATE_TASK") {
                    isAbstained = false
                    citedIds = emptyList()
                } else {
                    isAbstained = true
                    citedIds = emptyList()
                }
            } else {
                isAbstained = false
                citedIds = retrievalResult.retrievedIds.take(2)
            }

            val elapsedMs = (currentTimeNanos() - startTime) / 1_000_000
            latencies.add(elapsedMs)

            // Retrieval Metrics Calculation (for queries with expected keywords)
            if (q.expectedKeywords.isNotEmpty()) {
                retrievalQueryCount++
                val hits = retrievalResult.retrievedIds.map { id ->
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

            // Abstention Metrics
            if (q.shouldAbstain) {
                if (isAbstained) {
                    trueAbstain++
                } else {
                    falseNegativeAbstain++
                }
            } else {
                if (isAbstained) {
                    falseAbstain++
                } else {
                    trueNegativeAbstain++
                }
            }

            // Citation Validity
            for (cid in citedIds) {
                totalCitationsCount++
                val content = vault.getContent(cid)
                if (content != null) {
                    // Check if content matches any query keywords or expected keywords
                    val queryWords = LexicalBaselineSearch.tokenize(q.query)
                    val hasEvidence = queryWords.any { w -> content.contains(w, ignoreCase = true) } ||
                            q.expectedKeywords.any { kw -> content.contains(kw, ignoreCase = true) }
                    if (hasEvidence) {
                        validCitationsCount++
                    }
                }
            }

            // Action Proposal Verification
            val proposal = parseResult.stagedProposal
            if (proposal != null) {
                actionProposalsEmitted++

                // Schema validity: non-blank id, non-blank title, requires affirmative approval
                val isSchemaValid = proposal.id.isNotBlank() &&
                        proposal.title.isNotBlank() &&
                        proposal.requiresAffirmativeApproval

                if (isSchemaValid) actionSchemaValidCount++

                // Semantic validity: date not in past (epoch 2026-10-15), valid structure
                val dueDate = parseResult.extractedEntities["dueDate"]
                var isSemanticValid = isSchemaValid
                if (dueDate != null) {
                    // Format check: YYYY-MM-DD
                    val parts = dueDate.split("-")
                    if (parts.size == 3) {
                        val year = parts[0].toIntOrNull() ?: 0
                        val month = parts[1].toIntOrNull() ?: 0
                        val day = parts[2].toIntOrNull() ?: 0
                        // Check not before 2026-10-15
                        val isNotPast = (year > 2026) || (year == 2026 && month > 10) || (year == 2026 && month == 10 && day >= 15)
                        if (!isNotPast) isSemanticValid = false
                    } else {
                        isSemanticValid = false
                    }
                }
                if (isSemanticValid) actionSemanticValidCount++
            }
        }

        latencies.sort()
        val p50 = if (latencies.isNotEmpty()) latencies[latencies.size / 2] else 0L
        val p95 = if (latencies.isNotEmpty()) latencies[(latencies.size * 0.95).roundToInt().coerceAtMost(latencies.size - 1)] else 0L

        val precision = if (trueAbstain + falseAbstain > 0) trueAbstain.toDouble() / (trueAbstain + falseAbstain) else 1.0
        val recall = if (trueAbstain + falseNegativeAbstain > 0) trueAbstain.toDouble() / (trueAbstain + falseNegativeAbstain) else 1.0
        val f1 = if (precision + recall > 0.0) 2 * (precision * recall) / (precision + recall) else 0.0

        val rCount = if (retrievalQueryCount > 0) retrievalQueryCount.toDouble() else 1.0

        return MetricReport(
            totalQuestions = questions.size,
            vaultSize = vault.totalItems,
            recallAt1 = recallAt1Hits / rCount,
            recallAt3 = recallAt3Hits / rCount,
            recallAt5 = recallAt5Hits / rCount,
            mrr = totalReciprocalRank / rCount,
            citationValidityScore = if (totalCitationsCount > 0) validCitationsCount.toDouble() / totalCitationsCount else 1.0,
            abstentionPrecision = precision,
            abstentionRecall = recall,
            abstentionF1 = f1,
            actionSchemaValidity = if (actionProposalsEmitted > 0) actionSchemaValidCount.toDouble() / actionProposalsEmitted else 1.0,
            actionSemanticValidity = if (actionProposalsEmitted > 0) actionSemanticValidCount.toDouble() / actionProposalsEmitted else 1.0,
            promptInjectionDefendedRate = if (adversarialTotal > 0) adversarialDefended.toDouble() / adversarialTotal else 1.0,
            latencyP50Ms = p50,
            latencyP95Ms = p95
        )
    }

    private fun currentTimeNanos(): Long {
        return kotlin.time.TimeSource.Monotonic.markNow().elapsedNow().inWholeNanoseconds
    }
}
