package com.yaan.core.domain.ai

import kotlinx.serialization.Serializable

/**
 * High-level Intent Types classified by the Edge SLM and Cascade Router.
 */
@Serializable
enum class AiIntentType {
    TASK,
    NOTE,
    REMINDER,
    BRIEFING_QUERY,
    SEARCH,
    ACTION_PROPOSAL
}

/**
 * Parsed intent result produced by on-device Small Language Models (SmolLM2 / Gemma-2)
 * or the speculative cloud cascade router.
 */
@Serializable
data class ParsedIntent(
    val type: AiIntentType,
    val confidence: Double, // θ in [0.00, 1.00]
    val entities: Map<String, String>,
    val rawInput: String,
    val requiresCloudCascade: Boolean = confidence < 0.75
)

/**
 * Epistemic Knowledge Graph Fact with subjective confidence decay.
 *
 * Implements Layer 4 of ADR-0006:
 * Knowledge decay formula: c(t) = initialConfidence * 2^(-Δt / halfLifeMs)
 */
@Serializable
data class EpistemicFact(
    val id: String,
    val subject: String,
    val predicate: String,
    val initialConfidence: Double, // c0 in [0.00, 1.00]
    val citationSourceId: String,  // Evidence backlink to source voice memo / note
    val createdAtMs: Long,
    val halfLifeDays: Double = 30.0,
    val expiresAtMs: Long? = null
)

/**
 * Action proposal staged by probabilistic AI awaiting single-tap affirmative confirmation.
 *
 * Implements Layer 5 Action Guardrail (ADR-0006):
 * Probabilistic AI NEVER executes state-mutating actions without affirmative user approval.
 */
@Serializable
data class ActionProposalPayload(
    val id: String,
    val title: String,
    val actionType: String,
    val parameters: Map<String, String>,
    val affirmativeApprovalRequired: Boolean = true,
    val isAffirmativelyApproved: Boolean = false,
    val stagedAtMs: Long
)
