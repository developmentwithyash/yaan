package com.yaan.core.data.crdt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VectorClockTest {

    @Test
    fun testIncrementAndCompare() {
        val clockA = VectorClock().increment("nodeA")
        val clockB = VectorClock().increment("nodeB")

        // Concurrent clocks
        assertEquals(ClockComparison.CONCURRENT, clockA.compare(clockB))

        // Merged and causally following
        val merged = clockA.merge(clockB)
        assertEquals(ClockComparison.AFTER, merged.compare(clockA))
        assertEquals(ClockComparison.AFTER, merged.compare(clockB))
        assertEquals(ClockComparison.BEFORE, clockA.compare(merged))
    }

    @Test
    fun testMvrRegisterConflictResolution() {
        var registerA = MvrRegister<String>().set("Version A", "nodeA")
        var registerB = MvrRegister<String>().set("Version B", "nodeB")

        // Concurrent divergent writes
        val merged = registerA.merge(registerB)
        assertTrue(merged.isConflict)
        assertEquals(2, merged.values.size)
        assertTrue(merged.values.contains("Version A"))
        assertTrue(merged.values.contains("Version B"))

        // Causally superseding write on nodeA
        val resolved = merged.set("Resolved Final", "nodeA")
        assertFalse(resolved.isConflict)
        assertEquals(listOf("Resolved Final"), resolved.values)
    }
}
