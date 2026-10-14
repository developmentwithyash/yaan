package com.yaan.core.data.repository

import com.yaan.core.common.result.YaanError
import com.yaan.core.common.result.YaanResult
import com.yaan.core.domain.model.AlarmAckState
import com.yaan.core.domain.model.Reminder
import com.yaan.core.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryReminderRepository : ReminderRepository {
    private val mutex = Mutex()
    private val remindersFlow = MutableStateFlow<List<Reminder>>(emptyList())

    override fun observeReminders(): Flow<List<Reminder>> = remindersFlow.asStateFlow()

    override suspend fun getReminders(): YaanResult<List<Reminder>> = mutex.withLock {
        YaanResult.Success(remindersFlow.value)
    }

    override suspend fun getReminderById(id: String): YaanResult<Reminder?> = mutex.withLock {
        YaanResult.Success(remindersFlow.value.find { it.id == id })
    }

    override suspend fun scheduleReminder(reminder: Reminder): YaanResult<Unit> = mutex.withLock {
        val current = remindersFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == reminder.id }
        if (index >= 0) {
            current[index] = reminder
        } else {
            current.add(reminder)
        }
        remindersFlow.value = current
        YaanResult.Success(Unit)
    }

    override suspend fun updateAckState(id: String, ackState: AlarmAckState): YaanResult<Unit> = mutex.withLock {
        val current = remindersFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            current[index] = current[index].copy(ackState = ackState)
            remindersFlow.value = current
            YaanResult.Success(Unit)
        } else {
            YaanResult.Failure(YaanError.StorageError("Reminder not found: $id"))
        }
    }

    override suspend fun deleteReminder(id: String): YaanResult<Unit> = mutex.withLock {
        remindersFlow.value = remindersFlow.value.filterNot { it.id == id }
        YaanResult.Success(Unit)
    }
}
