package com.yaan.core.domain.ai

/**
 * Action Guardrail Gate.
 *
 * Implements Layer 5 of ADR-0006:
 * Strictly quarantines probabilistic AI outputs from mutating state or triggering external side-effects
 * without an explicit, single-tap affirmative user confirmation.
 */
interface ActionGuardrailGate {
    /**
     * Asserts whether the given action proposal is permitted to execute.
     */
    fun canExecute(proposal: ActionProposalPayload): Boolean
}

class StrictActionGuardrailGate : ActionGuardrailGate {
    override fun canExecute(proposal: ActionProposalPayload): Boolean {
        // If affirmative approval is required, the proposal MUST be explicitly approved by user
        if (proposal.affirmativeApprovalRequired) {
            return proposal.isAffirmativelyApproved
        }
        return true
    }
}
