package com.yaan.feature.briefing

import com.yaan.core.domain.model.Briefing

data class BriefingState(
    val briefing: Briefing? = null,
    val isLoading: Boolean = false,
    val isStale: Boolean = false,
)

sealed interface BriefingIntent {
    data object Refresh : BriefingIntent
    data class AdjustConflict(val conflictId: String) : BriefingIntent
}
