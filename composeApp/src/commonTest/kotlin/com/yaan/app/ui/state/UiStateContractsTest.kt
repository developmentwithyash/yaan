package com.yaan.app.ui.state

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UiStateContractsTest {

    @Test
    fun testHomeUiStateTransitions() {
        val initialState = HomeUiState()
        assertEquals(3, initialState.tasks.size)
        assertTrue(initialState.tasks[0].completed)
        assertFalse(initialState.tasks[1].completed)

        // Toggle task completion
        val updatedTasks = initialState.tasks.map { task ->
            if (task.id == "2") task.copy(completed = true) else task
        }
        val toggledState = initialState.copy(tasks = updatedTasks)
        assertTrue(toggledState.tasks.first { it.id == "2" }.completed)

        // Conflict adjustment state and reversible undo
        assertFalse(initialState.conflictAdjusted)
        val adjustedState = initialState.copy(conflictAdjusted = true)
        assertTrue(adjustedState.conflictAdjusted)
        val undoneAdjustedState = adjustedState.copy(conflictAdjusted = false)
        assertFalse(undoneAdjustedState.conflictAdjusted)

        // Briefing dismissal
        val dismissedBriefingState = initialState.copy(showBriefing = false)
        assertFalse(dismissedBriefingState.showBriefing)
    }

    @Test
    fun testNotesUiStateFilteringAndAudio() {
        val initialState = NotesUiState()
        assertEquals("All Notes", initialState.selectedFilter)
        assertFalse(initialState.isPlayingAudio)
        assertEquals(3, initialState.notes.size)

        // Filter transition
        val filteredState = initialState.copy(selectedFilter = "Voice Memos")
        assertEquals("Voice Memos", filteredState.selectedFilter)

        // Audio playback toggle
        val playingState = initialState.copy(isPlayingAudio = true)
        assertTrue(playingState.isPlayingAudio)

        // Search query
        val searchedState = initialState.copy(searchQuery = "Architecture")
        assertEquals("Architecture", searchedState.searchQuery)
    }

    @Test
    fun testAskYaanUiStateModeAndActionProposal() {
        val initialState = AskYaanUiState()
        assertEquals("Chat", initialState.activeMode)
        assertFalse(initialState.thoughtExpanded)
        assertNotNull(initialState.proposedAction)
        assertFalse(initialState.proposedAction!!.isConfirmed)

        // Switch to Voice
        val voiceState = initialState.copy(activeMode = "Voice")
        assertEquals("Voice", voiceState.activeMode)

        // Toggle Thought Accordion
        val expandedState = initialState.copy(thoughtExpanded = true)
        assertTrue(expandedState.thoughtExpanded)

        // Confirm action proposal
        val confirmedState = initialState.copy(
            proposedAction = initialState.proposedAction!!.copy(isConfirmed = true)
        )
        assertTrue(confirmedState.proposedAction!!.isConfirmed)

        // Dismiss action proposal (F-004)
        assertFalse(initialState.proposedAction!!.isDismissed)
        val dismissedState = initialState.copy(
            proposedAction = initialState.proposedAction!!.copy(isDismissed = true)
        )
        assertTrue(dismissedState.proposedAction!!.isDismissed)

        // Undo action proposal (F-004)
        assertFalse(initialState.proposedAction!!.isUndone)
        val undoneState = initialState.copy(
            proposedAction = initialState.proposedAction!!.copy(isUndone = true, isConfirmed = false)
        )
        assertTrue(undoneState.proposedAction!!.isUndone)
        assertFalse(undoneState.proposedAction!!.isConfirmed)
    }

    @Test
    fun testPlanUiStateCalendarAndRoutine() {
        val initialState = PlanUiState()
        assertEquals("Today", initialState.selectedTab)
        assertEquals(24, initialState.selectedDay)
        assertFalse(initialState.isDueNextDone)
        assertFalse(initialState.isRoutineRunning)

        // Tab selection
        val upcomingState = initialState.copy(selectedTab = "Upcoming")
        assertEquals("Upcoming", upcomingState.selectedTab)

        // Day selection
        val dayState = initialState.copy(selectedDay = 25)
        assertEquals(25, dayState.selectedDay)

        // Due next completion
        val dueDoneState = initialState.copy(isDueNextDone = true)
        assertTrue(dueDoneState.isDueNextDone)

        // Routine execution
        val routineRunningState = initialState.copy(isRoutineRunning = true)
        assertTrue(routineRunningState.isRoutineRunning)

        // Notification permission warning dismissal (F-003)
        assertFalse(initialState.isPermissionWarningDismissed)
        val dismissedWarningState = initialState.copy(isPermissionWarningDismissed = true)
        assertTrue(dismissedWarningState.isPermissionWarningDismissed)
    }

    @Test
    fun testYouUiStateSettingsAndArming() {
        val initialState = YouUiState(isDarkTheme = false)
        assertFalse(initialState.isDarkTheme)
        assertFalse(initialState.snapshotArmed)
        assertFalse(initialState.isPurgeConfirmed)

        // Theme toggle
        val darkState = initialState.copy(isDarkTheme = true)
        assertTrue(darkState.isDarkTheme)

        // Arm snapshot
        val armedState = initialState.copy(snapshotArmed = true)
        assertTrue(armedState.snapshotArmed)

        // Confirm purge
        val purgeState = initialState.copy(isPurgeConfirmed = true)
        assertTrue(purgeState.isPurgeConfirmed)
    }
}
