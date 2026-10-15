package com.yaan.core.domain.action

import com.yaan.core.domain.tool.ToolCallProposal
import com.yaan.core.domain.tool.ToolRegistry
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

enum class ActionState {
    PROPOSED,
    AWAITING_CONFIRMATION,
    AUTO_APPROVED,
    EXECUTING,
    VERIFIED_COMPLETED,
    FAILED,
    OUTCOME_UNKNOWN,
    EXPIRED,
    CANCELLED,
    UNDONE
}

enum class ActionErrorTaxonomy {
    VALIDATION_FAILED,
    ENTITY_NOT_FOUND,
    PERMISSION_DENIED,
    STORAGE_ERROR,
    TIMEOUT,
    ILLEGAL_TRANSITION,
    IDEMPOTENCY_CONFLICT,
    UNKNOWN_ERROR
}

@Serializable
data class ActionInstance(
    val idempotencyKey: String,
    val toolCall: ToolCallProposal,
    val state: ActionState,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val executedEntityId: String? = null,
    val verifiedPayloadJson: String? = null,
    val failureReason: String? = null,
    val failureErrorTaxonomy: String? = null,
    val outcomeMessage: String? = null
)

class IllegalActionStateTransitionException(
    val currentState: ActionState,
    val attemptedEvent: String,
    message: String = "Illegal state transition from $currentState via event '$attemptedEvent'."
) : IllegalStateException(message)

/**
 * Deterministic Client-Side Action State Machine.
 * Enforces all legal state transitions, rejects illegal transitions with typed exceptions,
 * ensures replay idempotency, and templates honest outcome messages from verified database state.
 */
class ActionStateMachine(
    private val clock: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) {
    private val actionStore = mutableMapOf<String, ActionInstance>()

    fun getAction(idempotencyKey: String): ActionInstance? = actionStore[idempotencyKey]

    /**
     * Stage 1: Initiates a proposed action.
     * Evaluates confirmation policy and transitions to either AWAITING_CONFIRMATION or AUTO_APPROVED.
     */
    fun propose(proposal: ToolCallProposal, currentEpochMs: Long = clock()): ActionInstance {
        val existing = actionStore[proposal.idempotencyKey]
        if (existing != null) {
            // Idempotent return: do not re-stage or duplicate
            return existing
        }

        val validation = ToolRegistry.validate(proposal, currentEpochMs)
        if (!validation.isValid) {
            val failed = ActionInstance(
                idempotencyKey = proposal.idempotencyKey,
                toolCall = proposal,
                state = ActionState.FAILED,
                createdAtEpochMs = currentEpochMs,
                updatedAtEpochMs = currentEpochMs,
                failureReason = validation.errors.joinToString("; "),
                failureErrorTaxonomy = ActionErrorTaxonomy.VALIDATION_FAILED.name,
                outcomeMessage = "Action validation failed: ${validation.errors.firstOrNull()}"
            )
            actionStore[proposal.idempotencyKey] = failed
            return failed
        }

        val targetState = if (validation.requiresConfirmation) {
            ActionState.AWAITING_CONFIRMATION
        } else {
            ActionState.AUTO_APPROVED
        }

        val instance = ActionInstance(
            idempotencyKey = proposal.idempotencyKey,
            toolCall = proposal,
            state = targetState,
            createdAtEpochMs = currentEpochMs,
            updatedAtEpochMs = currentEpochMs,
            outcomeMessage = if (targetState == ActionState.AWAITING_CONFIRMATION) {
                "Awaiting confirmation to execute ${proposal.toolName}."
            } else {
                "Action auto-approved for execution."
            }
        )
        actionStore[proposal.idempotencyKey] = instance
        return instance
    }

    /**
     * Transitions an action from AWAITING_CONFIRMATION to EXECUTING upon explicit user tap.
     */
    fun confirm(idempotencyKey: String): ActionInstance {
        val current = getRequiredAction(idempotencyKey)
        if (current.state != ActionState.AWAITING_CONFIRMATION) {
            throw IllegalActionStateTransitionException(current.state, "confirm")
        }
        val now = clock()
        val updated = current.copy(
            state = ActionState.EXECUTING,
            updatedAtEpochMs = now,
            outcomeMessage = "Executing action locally in vault..."
        )
        actionStore[idempotencyKey] = updated
        return updated
    }

    /**
     * User or system explicitly cancels a proposed/awaiting action.
     */
    fun cancel(idempotencyKey: String, reason: String = "User dismissed proposal"): ActionInstance {
        val current = getRequiredAction(idempotencyKey)
        if (current.state != ActionState.PROPOSED &&
            current.state != ActionState.AWAITING_CONFIRMATION &&
            current.state != ActionState.AUTO_APPROVED
        ) {
            throw IllegalActionStateTransitionException(current.state, "cancel")
        }
        val now = clock()
        val updated = current.copy(
            state = ActionState.CANCELLED,
            updatedAtEpochMs = now,
            failureReason = reason,
            outcomeMessage = "Action cancelled."
        )
        actionStore[idempotencyKey] = updated
        return updated
    }

    /**
     * Times out a proposal if user did not act within TTL.
     */
    fun expire(idempotencyKey: String): ActionInstance {
        val current = getRequiredAction(idempotencyKey)
        if (current.state != ActionState.PROPOSED && current.state != ActionState.AWAITING_CONFIRMATION) {
            throw IllegalActionStateTransitionException(current.state, "expire")
        }
        val now = clock()
        val updated = current.copy(
            state = ActionState.EXPIRED,
            updatedAtEpochMs = now,
            outcomeMessage = "Proposal expired."
        )
        actionStore[idempotencyKey] = updated
        return updated
    }

    /**
     * Begins local execution for an auto-approved action.
     */
    fun startExecution(idempotencyKey: String): ActionInstance {
        val current = getRequiredAction(idempotencyKey)
        if (current.state != ActionState.AUTO_APPROVED) {
            throw IllegalActionStateTransitionException(current.state, "startExecution")
        }
        val now = clock()
        val updated = current.copy(
            state = ActionState.EXECUTING,
            updatedAtEpochMs = now,
            outcomeMessage = "Executing action locally in vault..."
        )
        actionStore[idempotencyKey] = updated
        return updated
    }

    /**
     * Stage 2: Verified Execution Commit.
     * Takes verified entity ID and re-read database payload.
     * Templates honest outcome message from verified state.
     */
    fun recordSuccess(
        idempotencyKey: String,
        executedEntityId: String,
        verifiedPayloadJson: String
    ): ActionInstance {
        val current = getRequiredAction(idempotencyKey)
        if (current.state != ActionState.EXECUTING && current.state != ActionState.OUTCOME_UNKNOWN) {
            throw IllegalActionStateTransitionException(current.state, "recordSuccess")
        }
        val now = clock()
        val outcomeMsg = templateVerifiedOutcome(current.toolCall, executedEntityId)
        val updated = current.copy(
            state = ActionState.VERIFIED_COMPLETED,
            updatedAtEpochMs = now,
            executedEntityId = executedEntityId,
            verifiedPayloadJson = verifiedPayloadJson,
            outcomeMessage = outcomeMsg
        )
        actionStore[idempotencyKey] = updated
        return updated
    }

    /**
     * Records execution failure with error taxonomy.
     */
    fun recordFailure(
        idempotencyKey: String,
        taxonomy: ActionErrorTaxonomy,
        reason: String
    ): ActionInstance {
        val current = getRequiredAction(idempotencyKey)
        if (current.state != ActionState.EXECUTING && current.state != ActionState.OUTCOME_UNKNOWN) {
            throw IllegalActionStateTransitionException(current.state, "recordFailure")
        }
        val now = clock()
        val updated = current.copy(
            state = ActionState.FAILED,
            updatedAtEpochMs = now,
            failureErrorTaxonomy = taxonomy.name,
            failureReason = reason,
            outcomeMessage = "Execution failed: $reason"
        )
        actionStore[idempotencyKey] = updated
        return updated
    }

    /**
     * Marks execution outcome unknown if app is suspended or crashes mid-write.
     */
    fun recordInterrupted(idempotencyKey: String, reason: String = "Process interrupted before verification"): ActionInstance {
        val current = getRequiredAction(idempotencyKey)
        if (current.state != ActionState.EXECUTING) {
            throw IllegalActionStateTransitionException(current.state, "recordInterrupted")
        }
        val now = clock()
        val updated = current.copy(
            state = ActionState.OUTCOME_UNKNOWN,
            updatedAtEpochMs = now,
            failureReason = reason,
            outcomeMessage = "Action execution outcome unknown. Reconciling on next launch..."
        )
        actionStore[idempotencyKey] = updated
        return updated
    }

    /**
     * Reversible Undo: Rollback a completed action.
     */
    fun undo(idempotencyKey: String, rollbackExecutor: (ActionInstance) -> Boolean): ActionInstance {
        val current = getRequiredAction(idempotencyKey)
        if (current.state != ActionState.VERIFIED_COMPLETED) {
            throw IllegalActionStateTransitionException(current.state, "undo")
        }
        val success = rollbackExecutor(current)
        if (!success) {
            throw IllegalStateException("Rollback executor failed for action $idempotencyKey.")
        }
        val now = clock()
        val updated = current.copy(
            state = ActionState.UNDONE,
            updatedAtEpochMs = now,
            outcomeMessage = "Action undone. Vault state restored."
        )
        actionStore[idempotencyKey] = updated
        return updated
    }

    private fun getRequiredAction(idempotencyKey: String): ActionInstance {
        return actionStore[idempotencyKey]
            ?: throw NoSuchElementException("No action with idempotency key '$idempotencyKey' exists.")
    }

    /**
     * Binding Rule 6: Success messages are templated from the verified commit, NEVER from LLM generation.
     */
    private fun templateVerifiedOutcome(proposal: ToolCallProposal, entityId: String): String {
        val title = proposal.arguments["title"] ?: proposal.arguments["newTitle"] ?: "item"
        return when (proposal.toolName) {
            "create_reminder" -> "Reminder set: \"$title\"."
            "update_reminder" -> "Reminder \"$title\" updated."
            "delete_reminder" -> "Reminder deleted from vault."
            "create_note" -> "Note \"$title\" encrypted and saved."
            "update_note" -> "Note \"$title\" updated in vault."
            "delete_note" -> "Note deleted from vault."
            "create_task" -> "Task created: \"$title\"."
            "update_task" -> "Task updated: \"$title\"."
            "complete_task" -> "Task marked completed."
            else -> "Action ${proposal.toolName} executed successfully (ID: $entityId)."
        }
    }
}
