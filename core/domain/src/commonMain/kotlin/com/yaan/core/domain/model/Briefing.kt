package com.yaan.core.domain.model

import com.yaan.core.common.datetime.nowInstant
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class BriefingCommitment(
    val id: String,
    val title: String,
    val startTime: Instant,
    val endTime: Instant,
    val locationOrUrl: String? = null,
    val attendees: List<String> = emptyList(),
)

@Serializable
data class ScheduleConflict(
    val id: String,
    val description: String,
    val proposedResolution: String,
    val targetTime: Instant,
)

@Serializable
data class Briefing(
    val id: String,
    val targetDate: LocalDate,
    val summaryMarkdown: String,
    val cognitiveLoadScore: Int,
    val commitments: List<BriefingCommitment> = emptyList(),
    val conflicts: List<ScheduleConflict> = emptyList(),
    val preparedAt: Instant = nowInstant(),
    val isOfflineStale: Boolean = false,
)
