package com.yaan.core.domain.repository

import com.yaan.core.common.result.YaanResult
import com.yaan.core.domain.model.AlarmAckState
import com.yaan.core.domain.model.Reminder
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun observeReminders(): Flow<List<Reminder>>
    suspend fun getReminders(): YaanResult<List<Reminder>>
    suspend fun getReminderById(id: String): YaanResult<Reminder?>
    suspend fun scheduleReminder(reminder: Reminder): YaanResult<Unit>
    suspend fun updateAckState(id: String, ackState: AlarmAckState): YaanResult<Unit>
    suspend fun deleteReminder(id: String): YaanResult<Unit>
}
