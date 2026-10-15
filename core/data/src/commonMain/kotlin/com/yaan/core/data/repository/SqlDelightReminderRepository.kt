package com.yaan.core.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.yaan.core.common.dispatcher.CoroutineDispatchers
import com.yaan.core.common.result.YaanError
import com.yaan.core.common.result.YaanResult
import com.yaan.core.data.db.ReminderEntity
import com.yaan.core.data.db.YaanDatabase
import com.yaan.core.domain.alarm.AlarmScheduler
import com.yaan.core.domain.alarm.NoOpAlarmScheduler
import com.yaan.core.domain.model.AlarmAckState
import com.yaan.core.domain.model.Reminder
import com.yaan.core.domain.model.ReminderTier
import com.yaan.core.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

class SqlDelightReminderRepository(
    private val database: YaanDatabase,
    private val dispatchers: CoroutineDispatchers,
    private val alarmScheduler: AlarmScheduler = NoOpAlarmScheduler(),
) : ReminderRepository {
    private val queries = database.yaanDatabaseQueries

    override fun observeReminders(): Flow<List<Reminder>> {
        return queries.selectAllReminders()
            .asFlow()
            .mapToList(dispatchers.io)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getReminders(): YaanResult<List<Reminder>> = withContext(dispatchers.io) {
        try {
            val list = queries.selectAllReminders().executeAsList().map { it.toDomain() }
            YaanResult.Success(list)
        } catch (e: Exception) {
            YaanResult.Failure(YaanError.StorageError("Failed to fetch reminders: ${e.message}", e))
        }
    }

    override suspend fun getReminderById(id: String): YaanResult<Reminder?> = withContext(dispatchers.io) {
        try {
            val entity = queries.selectReminderById(id).executeAsOneOrNull()
            YaanResult.Success(entity?.toDomain())
        } catch (e: Exception) {
            YaanResult.Failure(YaanError.StorageError("Failed to fetch reminder $id: ${e.message}", e))
        }
    }

    override suspend fun scheduleReminder(reminder: Reminder): YaanResult<Unit> = withContext(dispatchers.io) {
        try {
            queries.insertReminder(
                id = reminder.id,
                title = reminder.title,
                trigger_time = reminder.triggerTime.toEpochMilliseconds(),
                recurrence = "NONE",
                is_dismissed = reminder.ackState == AlarmAckState.DISMISSED,
                channel = if (reminder.isAlarmSound) "ALARM" else "NOTIFICATION",
                created_at = reminder.createdAt.toEpochMilliseconds()
            )
            alarmScheduler.schedule(reminder)
            YaanResult.Success(Unit)
        } catch (e: Exception) {
            YaanResult.Failure(YaanError.StorageError("Failed to schedule reminder ${reminder.id}: ${e.message}", e))
        }
    }

    override suspend fun updateAckState(id: String, ackState: AlarmAckState): YaanResult<Unit> = withContext(dispatchers.io) {
        try {
            if (ackState == AlarmAckState.DISMISSED) {
                queries.dismissReminder(id)
                alarmScheduler.cancel(id)
            }
            YaanResult.Success(Unit)
        } catch (e: Exception) {
            YaanResult.Failure(YaanError.StorageError("Failed to update reminder ack state $id: ${e.message}", e))
        }
    }

    override suspend fun deleteReminder(id: String): YaanResult<Unit> = withContext(dispatchers.io) {
        try {
            queries.deleteReminder(id)
            alarmScheduler.cancel(id)
            YaanResult.Success(Unit)
        } catch (e: Exception) {
            YaanResult.Failure(YaanError.StorageError("Failed to delete reminder $id: ${e.message}", e))
        }
    }

    private fun ReminderEntity.toDomain(): Reminder {
        return Reminder(
            id = id,
            title = title,
            triggerTime = Instant.fromEpochMilliseconds(trigger_time),
            tier = if (channel == "ALARM") ReminderTier.PERSISTENT_ALARM else ReminderTier.NORMAL,
            isAlarmSound = channel == "ALARM",
            ackState = if (is_dismissed) AlarmAckState.DISMISSED else AlarmAckState.PENDING,
            createdAt = Instant.fromEpochMilliseconds(created_at)
        )
    }
}
