package com.yaan.core.domain.tool

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ToolRegistryTest {

    @Test
    fun testAllTwelveRequiredToolsExistInRegistry() {
        val requiredTools = listOf(
            "create_reminder",
            "update_reminder",
            "delete_reminder",
            "list_reminders",
            "create_note",
            "update_note",
            "delete_note",
            "search_notes",
            "create_task",
            "update_task",
            "complete_task",
            "get_preferences"
        )

        for (tool in requiredTools) {
            assertTrue(ToolRegistry.isToolPermitted(tool), "Tool '$tool' must be permitted in ToolRegistry")
            val def = ToolRegistry.getTool(tool)
            assertNotNull(def, "ToolDefinition for '$tool' must not be null")
            assertEquals(ToolExecutionAuthority.CLIENT, def.authority, "All tools must have CLIENT execution authority")
        }
    }

    @Test
    fun testUnpermittedToolIsRejected() {
        assertFalse(ToolRegistry.isToolPermitted("drop_database"))
        assertFalse(ToolRegistry.isToolPermitted("exec_shell"))

        val proposal = ToolCallProposal(
            idempotencyKey = "bad_1",
            toolName = "exec_shell",
            arguments = emptyMap()
        )
        val result = ToolRegistry.validate(proposal)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("not permitted") })
    }

    @Test
    fun testDestructiveToolsAlwaysRequireConfirmation() {
        val deleteReminder = ToolCallProposal(
            idempotencyKey = "del_rem_1",
            toolName = "delete_reminder",
            arguments = mapOf("reminderId" to "rem_123"),
            isExplicitUserRequest = true
        )
        val delRemResult = ToolRegistry.validate(deleteReminder)
        assertTrue(delRemResult.isValid)
        assertTrue(delRemResult.requiresConfirmation, "delete_reminder must ALWAYS require affirmative confirmation")

        val deleteNote = ToolCallProposal(
            idempotencyKey = "del_note_1",
            toolName = "delete_note",
            arguments = mapOf("noteId" to "note_123"),
            isExplicitUserRequest = true
        )
        val delNoteResult = ToolRegistry.validate(deleteNote)
        assertTrue(delNoteResult.isValid)
        assertTrue(delNoteResult.requiresConfirmation, "delete_note must ALWAYS require affirmative confirmation")
    }

    @Test
    fun testReadOnlyToolsAutoApprove() {
        val search = ToolCallProposal(
            idempotencyKey = "search_1",
            toolName = "search_notes",
            arguments = mapOf("query" to "quarterly targets")
        )
        val searchResult = ToolRegistry.validate(search)
        assertTrue(searchResult.isValid)
        assertFalse(searchResult.requiresConfirmation, "search_notes should auto-approve without confirmation")

        val listReminders = ToolCallProposal(
            idempotencyKey = "list_1",
            toolName = "list_reminders",
            arguments = emptyMap()
        )
        val listResult = ToolRegistry.validate(listReminders)
        assertTrue(listResult.isValid)
        assertFalse(listResult.requiresConfirmation, "list_reminders should auto-approve without confirmation")
    }

    @Test
    fun testMissingRequiredParametersFailValidation() {
        val createNoteWithoutContent = ToolCallProposal(
            idempotencyKey = "note_empty",
            toolName = "create_note",
            arguments = mapOf("title" to "My Note") // Missing content
        )
        val result = ToolRegistry.validate(createNoteWithoutContent)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Missing required parameter 'content'") })
    }

    @Test
    fun testPastTimeSchedulingValidation() {
        val pastEpochMs = 1000L
        val currentEpochMs = 1792022400000L

        val pastProposal = ToolCallProposal(
            idempotencyKey = "past_rem",
            toolName = "create_reminder",
            arguments = mapOf(
                "title" to "Dentist",
                "triggerTimeEpochMs" to pastEpochMs.toString()
            )
        )
        val pastResult = ToolRegistry.validate(pastProposal, currentEpochMs)
        assertFalse(pastResult.isValid)
        assertTrue(pastResult.errors.any { it.contains("past") })

        // With explicit allowPast flag
        val pastExplicit = pastProposal.copy(
            arguments = pastProposal.arguments + ("allowPast" to "true")
        )
        val explicitResult = ToolRegistry.validate(pastExplicit, currentEpochMs)
        assertTrue(explicitResult.isValid)
    }
}
