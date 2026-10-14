package com.yaan.core.domain.model

import com.yaan.core.common.datetime.nowInstant
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class ReminderTier {
    NORMAL,
    PERSISTENT_ALARM,
    MISSION_CRITICAL
}

@Serializable
enum class AlarmAckState {
    PENDING,
    SNOOZED,
    ACKNOWLEDGED,
    DISMISSED
}

@Serializable
data class Reminder(
    val id: String,
    val title: String,
    val triggerTime: Instant,
    val tier: ReminderTier = ReminderTier.NORMAL,
    val isAlarmSound: Boolean = false,
    val geofenceRadiusMeters: Double? = null,
    val ackState: AlarmAckState = AlarmAckState.PENDING,
    val createdAt: Instant = nowInstant(),
)
