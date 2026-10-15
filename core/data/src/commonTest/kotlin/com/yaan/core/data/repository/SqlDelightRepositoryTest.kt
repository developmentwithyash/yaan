package com.yaan.core.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.yaan.core.common.dispatcher.CoroutineDispatchers
import com.yaan.core.common.result.YaanResult
import com.yaan.core.data.db.VaultDatabaseInitializer
import com.yaan.core.data.db.YaanDatabase
import com.yaan.core.domain.model.AlarmAckState
import com.yaan.core.domain.model.Note
import com.yaan.core.domain.model.Task
import com.yaan.core.domain.model.TaskPriority
import com.yaan.core.domain.model.TaskStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SqlDelightRepositoryTest {

    private lateinit var database: YaanDatabase
    private lateinit var taskRepository: SqlDelightTaskRepository
    private lateinit var noteRepository: SqlDelightNoteRepository
    private lateinit var reminderRepository: SqlDelightReminderRepository
    private lateinit var briefingRepository: SqlDelightBriefingRepository
    private lateinit var memoryRepository: SqlDelightMemoryRepository

    private val testDispatchers = object : CoroutineDispatchers {
        override val main = Dispatchers.Unconfined
        override val io = Dispatchers.Unconfined
        override val default = Dispatchers.Unconfined
        override val unconfined = Dispatchers.Unconfined
    }

    @BeforeTest
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        YaanDatabase.Schema.create(driver)
        database = YaanDatabase(driver)
        VaultDatabaseInitializer.seedIfEmpty(database)

        taskRepository = SqlDelightTaskRepository(database, testDispatchers)
        noteRepository = SqlDelightNoteRepository(database, testDispatchers)
        reminderRepository = SqlDelightReminderRepository(database, testDispatchers)
        briefingRepository = SqlDelightBriefingRepository(database, testDispatchers)
        memoryRepository = SqlDelightMemoryRepository(database, testDispatchers)
    }

    @Test
    fun testDatabaseSeedingAndTaskCrud() = runTest {
        val initialTasksResult = taskRepository.getTasks()
        assertTrue(initialTasksResult is YaanResult.Success)
        val initialTasks = initialTasksResult.data
        assertEquals(3, initialTasks.size)

        // Toggle task completion
        val toggleResult = taskRepository.toggleTaskCompleted("task_01")
        assertTrue(toggleResult is YaanResult.Success)
        assertEquals(TaskStatus.PENDING, toggleResult.data.status)

        // Insert new task
        val newTask = Task(
            id = "task_custom_99",
            title = "Finalize Q4 Cloud Cost Hedge",
            priority = TaskPriority.URGENT
        )
        taskRepository.upsertTask(newTask)
        val fetched = taskRepository.getTaskById("task_custom_99")
        assertTrue(fetched is YaanResult.Success)
        assertEquals("Finalize Q4 Cloud Cost Hedge", fetched.data?.title)

        // Delete task
        taskRepository.deleteTask("task_custom_99")
        val deleted = taskRepository.getTaskById("task_custom_99")
        assertTrue(deleted is YaanResult.Success)
        assertEquals(null, deleted.data)
    }

    @Test
    fun testNoteSearchAndPersistence() = runTest {
        val notesResult = noteRepository.getNotes()
        assertTrue(notesResult is YaanResult.Success)
        assertEquals(3, notesResult.data.size)

        // Search notes for Kafka
        val searchResult = noteRepository.searchNotes("Kafka")
        assertTrue(searchResult is YaanResult.Success)
        assertEquals(1, searchResult.data.size)
        assertTrue(searchResult.data.first().title.contains("Root Cause Analysis"))

        // Add a note
        val note = Note(
            id = "note_new_01",
            title = "Board of Directors Presentation",
            contentCiphertext = "Series A traction highlights with 99.4% gross margin."
        )
        noteRepository.upsertNote(note)
        val updatedNotes = noteRepository.getNotes()
        assertTrue(updatedNotes is YaanResult.Success)
        assertEquals(4, updatedNotes.data.size)
    }

    @Test
    fun testReminderAndBriefingRepositories() = runTest {
        val remindersResult = reminderRepository.getReminders()
        assertTrue(remindersResult is YaanResult.Success)
        assertEquals(2, remindersResult.data.size)

        // Dismiss reminder
        reminderRepository.updateAckState("rem_01", AlarmAckState.DISMISSED)
        val updatedRem = reminderRepository.getReminderById("rem_01")
        assertTrue(updatedRem is YaanResult.Success)
        assertEquals(AlarmAckState.DISMISSED, updatedRem.data?.ackState)

        // Observe briefing
        val briefing = briefingRepository.observeLatestBriefing().first()
        assertNotNull(briefing)
        assertTrue(briefing.summaryMarkdown.contains("primary commitments"))
        assertEquals(1, briefing.conflicts.size)
        assertTrue(briefing.conflicts.first().description.contains("Dentist Appointment"))
    }
}
