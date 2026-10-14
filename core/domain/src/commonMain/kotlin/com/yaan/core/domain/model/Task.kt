package com.yaan.core.domain.model

import com.yaan.core.common.datetime.nowInstant
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}

@Serializable
enum class TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    ARCHIVED
}

@Serializable
enum class AutoReschedulePolicy {
    NEVER,
    NEXT_MORNING,
    NEXT_WEEK,
    AUTO_GARDENING
}

@Serializable
data class Task(
    val id: String,
    val title: String,
    val notes: String = "",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.PENDING,
    val dueAt: Instant? = null,
    val completedAt: Instant? = null,
    val autoReschedulePolicy: AutoReschedulePolicy = AutoReschedulePolicy.NEXT_MORNING,
    val parentTaskId: String? = null,
    val createdAt: Instant = nowInstant(),
)
