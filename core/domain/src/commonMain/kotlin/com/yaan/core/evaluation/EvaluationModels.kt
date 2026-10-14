package com.yaan.core.evaluation

import com.yaan.core.domain.model.ActionProposal
import kotlinx.serialization.Serializable

/**
 * Categories of questions tested in the Ask YAAN evaluation suite.
 */
@Serializable
enum class EvaluationCategory {
    FACTUAL_LOOKUP,
    TIME_BASED,
    MULTI_NOTE,
    NOT_IN_VAULT,
    AMBIGUOUS,
    ADVERSARIAL_INJECTION
}

/**
 * A single labelled evaluation sample.
 */
@Serializable
data class EvaluationQuestion(
    val id: String,
    val query: String,
    val category: EvaluationCategory,
    val targetIds: List<String> = emptyList(),
    val expectedKeywords: List<String> = emptyList(),
    val expectedActionType: String? = null,
    val shouldAbstain: Boolean = category == EvaluationCategory.NOT_IN_VAULT,
    val expectedSemanticDate: String? = null, // e.g. "2026-10-20"
    val adversaryPayload: String? = null,
)

/**
 * Retrieval result for a single evaluation query.
 */
data class RetrievalResult(
    val queryId: String,
    val retrievedIds: List<String>,
    val scores: List<Double> = emptyList(),
    val latencyMs: Long = 0,
)

/**
 * Full answer result from Ask YAAN for an evaluation query.
 */
data class AnswerResult(
    val queryId: String,
    val answerText: String,
    val citedSourceIds: List<String>,
    val isAbstention: Boolean,
    val confidence: Double,
    val stagedProposal: ActionProposal? = null,
    val latencyMs: Long = 0,
)

/**
 * Aggregate evaluation scorecard across all metrics.
 */
data class MetricReport(
    val totalQuestions: Int,
    val vaultSize: Int,
    val recallAt1: Double,
    val recallAt3: Double,
    val recallAt5: Double,
    val mrr: Double, // Mean Reciprocal Rank
    val citationValidityScore: Double, // Fraction of citations that exist and contain evidence
    val abstentionPrecision: Double,
    val abstentionRecall: Double,
    val abstentionF1: Double,
    val actionSchemaValidity: Double,
    val actionSemanticValidity: Double,
    val promptInjectionDefendedRate: Double,
    val latencyP50Ms: Long,
    val latencyP95Ms: Long,
)
