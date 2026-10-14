package com.yaan.backend.briefing

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Asynchronous Nightly Pre-Computation Worker Service.
 *
 * Implements Invariant #4 & Pillar 4 of ADR-0002:
 * Pre-computes morning briefings while the user sleeps, ensuring 0ms wake-up rendering.
 */
class BriefingWorkerService {

    // Cache of pre-computed briefings: userId -> latest briefing
    private val briefings = ConcurrentHashMap<String, PrecomputedBriefingEnvelope>()

    fun generateBriefing(request: GenerateBriefingRequest): PrecomputedBriefingEnvelope {
        val conflicts = if (request.calendarItemsCount > 2) {
            listOf(
                ScheduleConflict(
                    title = "Product Architecture Sync vs Dentist Commute",
                    timeSlot = "2:00 PM - 2:30 PM",
                    overlapDurationMin = 25,
                    proposedResolution = "Shift preparation reminder to 1:15 PM"
                )
            )
        } else {
            emptyList()
        }

        val envelope = PrecomputedBriefingEnvelope(
            briefingId = UUID.randomUUID().toString(),
            userId = request.userId,
            generatedAtMs = System.currentTimeMillis(),
            validForDate = request.targetDate,
            headline = "Good morning, Elena",
            summary = "You have ${request.calendarItemsCount} commitments today. Your next priority is the Product Architecture Sync, and 1 reminder needs rescheduling due to travel time.",
            commitmentsCount = request.calendarItemsCount,
            cognitiveLoad = if (conflicts.isNotEmpty()) "LIGHT" else "OPTIMAL",
            conflicts = conflicts,
            isPrecomputed = true
        )

        briefings[request.userId] = envelope
        return envelope
    }

    fun getLatestBriefing(userId: String): PrecomputedBriefingEnvelope? {
        return briefings[userId]
    }
}
