package com.yaan.core.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.yaan.core.common.dispatcher.CoroutineDispatchers
import com.yaan.core.common.result.YaanResult
import com.yaan.core.data.db.YaanDatabase
import com.yaan.core.domain.model.Note
import com.yaan.core.domain.model.VaultTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SqlDelightNotePaginationTest {

    private val testDispatchers = object : CoroutineDispatchers {
        override val main = Dispatchers.Unconfined
        override val io = Dispatchers.Unconfined
        override val default = Dispatchers.Unconfined
        override val unconfined = Dispatchers.Unconfined
    }

    private fun createTestDb(): YaanDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        YaanDatabase.Schema.create(driver)
        return YaanDatabase(driver)
    }

    @Test
    fun `getNotesPaged returns correct slice with limit and offset`() = runTest {
        val db = createTestDb()
        val repository = SqlDelightNoteRepository(
            database = db,
            dispatchers = testDispatchers
        )

        // Insert 10 notes with increasing timestamps
        for (i in 1..10) {
            val note = Note(
                id = "note_$i",
                title = "Note #$i",
                contentCiphertext = "Content of note $i",
                tags = listOf(VaultTag("Test")),
                isPinned = (i == 3), // note_3 is pinned
                createdAt = Instant.fromEpochMilliseconds(i * 1000L),
                updatedAt = Instant.fromEpochMilliseconds(i * 1000L)
            )
            val res = repository.upsertNote(note)
            assertTrue(res is YaanResult.Success)
        }

        // Pinned note (note_3) should come first, then others descending by createdAt:
        // Expected order: note_3 (pinned), note_10, note_9, note_8, note_7, note_6, note_5, note_4, note_2, note_1

        // Page 1: limit 3, offset 0 -> note_3, note_10, note_9
        val page1 = repository.getNotesPaged(limit = 3, offset = 0)
        assertTrue(page1 is YaanResult.Success)
        val p1Notes = (page1 as YaanResult.Success).data
        assertEquals(3, p1Notes.size)
        assertEquals("note_3", p1Notes[0].id)
        assertEquals("note_10", p1Notes[1].id)
        assertEquals("note_9", p1Notes[2].id)

        // Page 2: limit 3, offset 3 -> note_8, note_7, note_6
        val page2 = repository.getNotesPaged(limit = 3, offset = 3)
        assertTrue(page2 is YaanResult.Success)
        val p2Notes = (page2 as YaanResult.Success).data
        assertEquals(3, p2Notes.size)
        assertEquals("note_8", p2Notes[0].id)
        assertEquals("note_7", p2Notes[1].id)
        assertEquals("note_6", p2Notes[2].id)

        // Offset beyond dataset -> empty
        val pageEmpty = repository.getNotesPaged(limit = 10, offset = 20)
        assertTrue(pageEmpty is YaanResult.Success)
        assertTrue((pageEmpty as YaanResult.Success).data.isEmpty())
    }

    @Test
    fun `inMemoryNoteRepository also supports pagination correctly`() = runTest {
        val repository = InMemoryNoteRepository()

        for (i in 1..5) {
            val note = Note(
                id = "mem_note_$i",
                title = "Mem Note #$i",
                contentCiphertext = "Content $i",
                tags = listOf(VaultTag("Mem")),
                isPinned = false,
                createdAt = Instant.fromEpochMilliseconds(i * 1000L),
                updatedAt = Instant.fromEpochMilliseconds(i * 1000L)
            )
            repository.upsertNote(note)
        }

        val page = repository.getNotesPaged(limit = 2, offset = 1)
        assertTrue(page is YaanResult.Success)
        val notes = (page as YaanResult.Success).data
        assertEquals(2, notes.size)
        assertEquals("mem_note_4", notes[0].id)
        assertEquals("mem_note_3", notes[1].id)
    }
}
