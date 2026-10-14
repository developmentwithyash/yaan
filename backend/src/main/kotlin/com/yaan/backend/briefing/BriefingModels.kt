package com.yaan.backend.briefing

import kotlinx.serialization.Serializable

@Serializable
data class ScheduleConflict(
    val title: String,
    val timeSlot: String,
    val overlapDurationMin: Int,
    val proposedResolution: String
)

@Serializable
data class PrecomputedBriefingEnvelope(
    val briefingId: String,
    val userId: String,
    val generatedAtMs: Long,
    val validForDate: String,
    val headline: String,
    val summary: String,
    val commitmentsCount: Int,
    val cognitiveLoad: String, // "LIGHT", "MODERATE", "HEAVY"
    val conflicts: List<ScheduleConflict> = emptyList(),
    val isPrecomputed: Boolean = true
)

@Serializable
data class GenerateBriefingRequest(
    val userId: String,
    val targetDate: String,
    val calendarItemsCount: Int = 3,
    val unaddressedAlertsCount: Int = 0
)
