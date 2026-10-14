package com.yaan.feature.actions

import com.yaan.core.domain.model.ActionProposal

data class ActionsState(
    val proposals: List<ActionProposal> = emptyList(),
    val isExecuting: Boolean = false,
)

sealed interface ActionsIntent {
    data class ApproveAction(val id: String) : ActionsIntent
    data class ModifyAction(val id: String) : ActionsIntent
}
