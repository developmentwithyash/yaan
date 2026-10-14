package com.yaan.app.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class YaanDestinationTest {

    @Test
    fun testAllDestinationsDefined() {
        val destinations = YaanDestination.entries
        assertEquals(5, destinations.size)
        assertTrue(destinations.contains(YaanDestination.Home))
        assertTrue(destinations.contains(YaanDestination.Plan))
        assertTrue(destinations.contains(YaanDestination.AskYaan))
        assertTrue(destinations.contains(YaanDestination.Notes))
        assertTrue(destinations.contains(YaanDestination.You))
    }

    @Test
    fun testDestinationTitles() {
        assertEquals("Home", YaanDestination.Home.title)
        assertEquals("Plan", YaanDestination.Plan.title)
        assertEquals("Ask YAAN", YaanDestination.AskYaan.title)
        assertEquals("Notes", YaanDestination.Notes.title)
        assertEquals("You", YaanDestination.You.title)
    }
}
