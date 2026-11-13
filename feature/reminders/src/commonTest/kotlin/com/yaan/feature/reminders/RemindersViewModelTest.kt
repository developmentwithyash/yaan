package com.yaan.feature.reminders

import com.yaan.core.common.datetime.nowInstant
import com.yaan.core.common.dispatcher.TestCoroutineDispatchers
import com.yaan.core.common.result.YaanResult
import com.yaan.core.domain.model.AlarmAckState
import com.yaan.core.domain.model.Reminder
import com.yaan.core.domain.repository.ReminderRepository
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeReminderRepository : ReminderRepository {
    val reminders = mutableListOf<Reminder>()
    val deletedIds = mutableListOf<String>()

    override fun observeReminders(): Flow<List<Reminder>> = flowOf(reminders)

    override suspend fun getReminders(): YaanResult<List<Reminder>> = YaanResult.Success(reminders)

    override suspend fun getReminderById(id: String): YaanResult<Reminder?> =
        YaanResult.Success(reminders.find { it.id == id })

    override suspend fun scheduleReminder(reminder: Reminder): YaanResult<Unit> {
        val index = reminders.indexOfFirst { it.id == reminder.id }
        if (index >= 0) {
            reminders[index] = reminder
        } else {
            reminders.add(reminder)
        }
        return YaanResult.Success(Unit)
    }

    override suspend fun updateAckState(id: String, ackState: AlarmAckState): YaanResult<Unit> {
        val index = reminders.indexOfFirst { it.id == id }
        if (index >= 0) {
            reminders[index] = reminders[index].copy(ackState = ackState)
        }
        return YaanResult.Success(Unit)
    }

    override suspend fun deleteReminder(id: String): YaanResult<Unit> {
        deletedIds.add(id)
        reminders.removeAll { it.id == id }
        return YaanResult.Success(Unit)
    }
}

class RemindersViewModelTest {

    @Test
    fun testSnoozeReminderUpdatesTriggerTimeAndAckState() = runTest {
        val repository = FakeReminderRepository()
        val pastTime = nowInstant() - 1.hours
        val reminder = Reminder(
            id = "rem-123",
            title = "Review Architecture Proposal",
            triggerTime = pastTime,
            ackState = AlarmAckState.PENDING,
        )
        repository.scheduleReminder(reminder)

        val viewModel = RemindersViewModel(repository, TestCoroutineDispatchers())
        viewModel.onIntent(RemindersIntent.SnoozeReminder("rem-123"))

        val updated = repository.reminders.find { it.id == "rem-123" }
        assertEquals(AlarmAckState.SNOOZED, updated?.ackState)
        assertTrue(updated != null && updated.triggerTime > nowInstant())
    }

    @Test
    fun testAcknowledgeReminder() = runTest {
        val repository = FakeReminderRepository()
        val reminder = Reminder(
            id = "rem-456",
            title = "Team Standup",
            triggerTime = nowInstant(),
            ackState = AlarmAckState.PENDING,
        )
        repository.scheduleReminder(reminder)

        val viewModel = RemindersViewModel(repository, TestCoroutineDispatchers())
        viewModel.onIntent(RemindersIntent.AcknowledgeReminder("rem-456"))

        val updated = repository.reminders.find { it.id == "rem-456" }
        assertEquals(AlarmAckState.ACKNOWLEDGED, updated?.ackState)
    }

    @Test
    fun testDeleteReminder() = runTest {
        val repository = FakeReminderRepository()
        val reminder = Reminder(
            id = "rem-789",
            title = "Obsolete Alert",
            triggerTime = nowInstant(),
        )
        repository.scheduleReminder(reminder)

        val viewModel = RemindersViewModel(repository, TestCoroutineDispatchers())
        viewModel.onIntent(RemindersIntent.DeleteReminder("rem-789"))

        assertTrue(repository.deletedIds.contains("rem-789"))
        assertEquals(0, repository.reminders.size)
    }
}
