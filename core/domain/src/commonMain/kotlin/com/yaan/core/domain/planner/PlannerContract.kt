package com.yaan.core.domain.planner

import com.yaan.core.domain.tool.ToolCallProposal
import kotlinx.serialization.Serializable

/**
 * Strict 5-state response taxonomy for the stateless cloud planner.
 * Invariant: Every planner response MUST be exactly one of these five types.
 */
@Serializable
enum class PlannerResponseType {
    ANSWER,
    CLARIFY,
    PROPOSAL,
    REFUSE,
    ERROR
}

@Serializable
data class PlannerRequest(
    val prompt: String,
    val topKContextSnippets: List<String> = emptyList(),
    val userTimeZone: String = "Asia/Kolkata",
    val currentEpochMs: Long = 0L,
    val idempotencyKey: String? = null
)

@Serializable
data class PlannerResponse(
    val type: PlannerResponseType,
    val answerText: String? = null,
    val clarificationQuestion: String? = null,
    val proposedAction: ToolCallProposal? = null,
    val refuseReason: String? = null,
    val errorMessage: String? = null,
    val providerUsed: String = "none",
    val latencyMs: Long = 0L
)
