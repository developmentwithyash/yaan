package com.yaan.core.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.yaan.core.common.dispatcher.CoroutineDispatchers
import com.yaan.core.common.result.YaanResult
import com.yaan.core.data.db.YaanDatabase
import com.yaan.core.domain.alarm.AlarmScheduler
import com.yaan.core.domain.model.AlarmAckState
import com.yaan.core.domain.model.Reminder
import com.yaan.core.domain.model.ReminderTier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SqlDelightReminderSchedulerTest {

    private lateinit var database: YaanDatabase
    private val scheduledReminders = mutableListOf<Reminder>()
    private val cancelledReminderIds = mutableListOf<String>()

    private val testScheduler = object : AlarmScheduler {
        override fun schedule(reminder: Reminder) {
            scheduledReminders.add(reminder)
        }

        override fun cancel(reminderId: String) {
            cancelledReminderIds.add(reminderId)
        }
    }

    private val testDispatchers = object : CoroutineDispatchers {
        override val main = Dispatchers.Unconfined
        override val io = Dispatchers.Unconfined
        override val default = Dispatchers.Unconfined
        override val unconfined = Dispatchers.Unconfined
    }

    private lateinit var repository: SqlDelightReminderRepository

    @BeforeTest
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        YaanDatabase.Schema.create(driver)
        database = YaanDatabase(driver)
        scheduledReminders.clear()
        cancelledReminderIds.clear()

        repository = SqlDelightReminderRepository(
            database = database,
            dispatchers = testDispatchers,
            alarmScheduler = testScheduler
        )
    }

    @Test
    fun `scheduling reminder persists to SQLite and dispatches to AlarmScheduler`() = runTest {
        val reminder = Reminder(
            id = "rem_test_01",
            title = "Board Strategy Briefing",
            triggerTime = Instant.fromEpochMilliseconds(1730000000000L),
            tier = ReminderTier.PERSISTENT_ALARM,
            isAlarmSound = true,
            ackState = AlarmAckState.PENDING,
            createdAt = Instant.fromEpochMilliseconds(1729000000000L)
        )

        val scheduleResult = repository.scheduleReminder(reminder)
        assertTrue(scheduleResult is YaanResult.Success)

        // Verify SQLite persistence
        val fetched = repository.getReminderById("rem_test_01")
        assertTrue(fetched is YaanResult.Success)
        assertEquals("Board Strategy Briefing", fetched.data?.title)
        assertTrue(fetched.data?.isAlarmSound == true)

        // Verify AlarmScheduler invocation
        assertEquals(1, scheduledReminders.size)
        assertEquals("rem_test_01", scheduledReminders[0].id)
    }

    @Test
    fun `dismissing reminder updates status and cancels alarm`() = runTest {
        val reminder = Reminder(
            id = "rem_test_02",
            title = "Flight to Frankfurt",
            triggerTime = Instant.fromEpochMilliseconds(1730000000000L),
            tier = ReminderTier.NORMAL,
            isAlarmSound = false,
            ackState = AlarmAckState.PENDING,
            createdAt = Instant.fromEpochMilliseconds(1729000000000L)
        )

        repository.scheduleReminder(reminder)
        assertEquals(1, scheduledReminders.size)

        // Dismiss reminder
        val dismissResult = repository.updateAckState("rem_test_02", AlarmAckState.DISMISSED)
        assertTrue(dismissResult is YaanResult.Success)

        // Verify Alarm cancellation
        assertEquals(1, cancelledReminderIds.size)
        assertEquals("rem_test_02", cancelledReminderIds[0])
    }

    @Test
    fun `deleting reminder removes from SQLite and cancels alarm`() = runTest {
        val reminder = Reminder(
            id = "rem_test_03",
            title = "Quarterly Tax Review",
            triggerTime = Instant.fromEpochMilliseconds(1730000000000L),
            tier = ReminderTier.NORMAL,
            isAlarmSound = false,
            ackState = AlarmAckState.PENDING,
            createdAt = Instant.fromEpochMilliseconds(1729000000000L)
        )

        repository.scheduleReminder(reminder)
        val deleteResult = repository.deleteReminder("rem_test_03")
        assertTrue(deleteResult is YaanResult.Success)

        val fetched = repository.getReminderById("rem_test_03")
        assertTrue(fetched is YaanResult.Success)
        assertEquals(null, fetched.data)

        assertEquals(1, cancelledReminderIds.size)
        assertEquals("rem_test_03", cancelledReminderIds[0])
    }
}
