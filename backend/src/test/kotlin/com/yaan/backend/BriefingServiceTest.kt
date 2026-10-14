package com.yaan.backend

import com.yaan.backend.briefing.BriefingWorkerService
import com.yaan.backend.briefing.GenerateBriefingRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BriefingServiceTest {

    @Test
    fun `test precomputed briefing generation with schedule conflicts`() {
        val service = BriefingWorkerService()
        val userId = "elena-exec"

        val request = GenerateBriefingRequest(
            userId = userId,
            targetDate = "2026-10-24",
            calendarItemsCount = 4
        )

        val briefing = service.generateBriefing(request)
        assertEquals(userId, briefing.userId)
        assertEquals(4, briefing.commitmentsCount)
        assertTrue(briefing.isPrecomputed)
        assertEquals("LIGHT", briefing.cognitiveLoad)
        assertEquals(1, briefing.conflicts.size)
        assertTrue(briefing.conflicts.first().title.contains("Dentist"))

        // Retrieve latest
        val cached = service.getLatestBriefing(userId)
        assertNotNull(cached)
        assertEquals(briefing.briefingId, cached.briefingId)
    }
}
