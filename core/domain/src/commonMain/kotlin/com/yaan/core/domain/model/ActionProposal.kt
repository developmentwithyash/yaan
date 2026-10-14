package com.yaan.core.domain.model

import com.yaan.core.common.datetime.nowInstant
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class ActionType {
    RESCHEDULE_MEETING,
    DRAFT_EMAIL,
    PURGE_VAULT,
    EXPORT_ARCHIVE,
    CREATE_TASK,
}

@Serializable
data class ActionProposal(
    val id: String,
    val title: String,
    val summary: String,
    val actionType: ActionType,
    val requiresAffirmativeApproval: Boolean = true,
    val isExecuted: Boolean = false,
    val proposedAt: Instant = nowInstant(),
)
