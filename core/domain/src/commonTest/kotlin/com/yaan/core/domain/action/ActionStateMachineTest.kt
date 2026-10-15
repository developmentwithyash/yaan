package com.yaan.core.domain.action

import com.yaan.core.domain.tool.ToolCallProposal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ActionStateMachineTest {

    private val fixedEpochMs = 1792022400000L
    private val stateMachine = ActionStateMachine(clock = { fixedEpochMs })

    @Test
    fun testConsequentialActionTransitionsToAwaitingConfirmationThenExecutesAndVerifies() {
        val proposal = ToolCallProposal(
            idempotencyKey = "act_del_1",
            toolName = "delete_reminder",
            arguments = mapOf("reminderId" to "rem_999"),
            isExplicitUserRequest = true
        )

        // 1. Propose
        val staged = stateMachine.propose(proposal, fixedEpochMs)
        assertEquals(ActionState.AWAITING_CONFIRMATION, staged.state)

        // 2. User confirms
        val executing = stateMachine.confirm("act_del_1")
        assertEquals(ActionState.EXECUTING, executing.state)

        // 3. Verified database commit
        val verified = stateMachine.recordSuccess(
            idempotencyKey = "act_del_1",
            executedEntityId = "rem_999",
            verifiedPayloadJson = "{\"deleted\":true}"
        )
        assertEquals(ActionState.VERIFIED_COMPLETED, verified.state)
        assertEquals("Reminder deleted from vault.", verified.outcomeMessage)

        // 4. Undo rollback
        var rollbackCalled = false
        val undone = stateMachine.undo("act_del_1") {
            rollbackCalled = true
            true
        }
        assertEquals(ActionState.UNDONE, undone.state)
        assertTrue(rollbackCalled)
    }

    @Test
    fun testNonConsequentialActionAutoApprovesAndExecutes() {
        val proposal = ToolCallProposal(
            idempotencyKey = "act_read_1",
            toolName = "search_notes",
            arguments = mapOf("query" to "strategy")
        )

        val staged = stateMachine.propose(proposal, fixedEpochMs)
        assertEquals(ActionState.AUTO_APPROVED, staged.state)

        val executing = stateMachine.startExecution("act_read_1")
        assertEquals(ActionState.EXECUTING, executing.state)

        val verified = stateMachine.recordSuccess(
            idempotencyKey = "act_read_1",
            executedEntityId = "query_1",
            verifiedPayloadJson = "[]"
        )
        assertEquals(ActionState.VERIFIED_COMPLETED, verified.state)
    }

    @Test
    fun testIllegalTransitionsThrowTypedException() {
        val proposal = ToolCallProposal(
            idempotencyKey = "act_illegal_1",
            toolName = "delete_note",
            arguments = mapOf("noteId" to "note_555")
        )

        val staged = stateMachine.propose(proposal, fixedEpochMs)
        assertEquals(ActionState.AWAITING_CONFIRMATION, staged.state)

        // Cannot recordSuccess directly from AWAITING_CONFIRMATION (bypassing EXECUTING)
        assertFailsWith<IllegalActionStateTransitionException> {
            stateMachine.recordSuccess("act_illegal_1", "note_555", "{}")
        }

        // Cannot startExecution on an action awaiting confirmation
        assertFailsWith<IllegalActionStateTransitionException> {
            stateMachine.startExecution("act_illegal_1")
        }

        // Confirm moves to EXECUTING
        stateMachine.confirm("act_illegal_1")

        // Cannot confirm again while EXECUTING
        assertFailsWith<IllegalActionStateTransitionException> {
            stateMachine.confirm("act_illegal_1")
        }
    }

    @Test
    fun testIdempotencyReplayPreservesStateWithoutDuplication() {
        val proposal = ToolCallProposal(
            idempotencyKey = "idempotent_key_1",
            toolName = "create_note",
            arguments = mapOf("title" to "Quick Idea", "content" to "Remember this")
        )

        val first = stateMachine.propose(proposal, fixedEpochMs)
        val second = stateMachine.propose(proposal, fixedEpochMs + 5000L)

        assertEquals(first.idempotencyKey, second.idempotencyKey)
        assertEquals(first.createdAtEpochMs, second.createdAtEpochMs)
        assertEquals(first.state, second.state)
    }

    @Test
    fun testOutcomeUnknownReconciliation() {
        val proposal = ToolCallProposal(
            idempotencyKey = "crash_recovery_1",
            toolName = "create_task",
            arguments = mapOf("title" to "Finish audit")
        )

        stateMachine.propose(proposal, fixedEpochMs)
        stateMachine.startExecution("crash_recovery_1")

        // Process killed mid-write
        val interrupted = stateMachine.recordInterrupted("crash_recovery_1", "App killed")
        assertEquals(ActionState.OUTCOME_UNKNOWN, interrupted.state)

        // On next boot: verified by scanning database
        val reconciled = stateMachine.recordSuccess("crash_recovery_1", "task_fin_1", "{\"persisted\":true}")
        assertEquals(ActionState.VERIFIED_COMPLETED, reconciled.state)
        assertEquals("Task created: \"Finish audit\".", reconciled.outcomeMessage)
    }
}
