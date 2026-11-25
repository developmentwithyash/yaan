package com.yaan.app.ui.gestures

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SwipeActionCoordinatorTest {

    @Test
    fun testSubThresholdSwipeProducesNone() {
        var completeCalled = false
        var deleteCalled = false
        val coordinator = SwipeActionCoordinator(
            thresholdPx = 100f,
            onComplete = { completeCalled = true },
            onDelete = { deleteCalled = true }
        )

        val resultSmallRight = coordinator.evaluateSwipe(50f)
        assertEquals(SwipeActionResult.NONE, resultSmallRight)
        assertEquals(SwipeDirection.START_TO_END, coordinator.determineDirection(50f))

        val resultSmallLeft = coordinator.evaluateSwipe(-50f)
        assertEquals(SwipeActionResult.NONE, resultSmallLeft)
        assertEquals(SwipeDirection.END_TO_START, coordinator.determineDirection(-50f))

        val actionResult = coordinator.executeSwipeAction(30f)
        assertEquals(SwipeActionResult.NONE, actionResult)
        assertFalse(completeCalled)
        assertFalse(deleteCalled)
    }

    @Test
    fun testRightSwipeBeyondThresholdCompletes() {
        var completeCalled = false
        var deleteCalled = false
        val coordinator = SwipeActionCoordinator(
            thresholdPx = 100f,
            onComplete = { completeCalled = true },
            onDelete = { deleteCalled = true }
        )

        val eval = coordinator.evaluateSwipe(120f)
        assertEquals(SwipeActionResult.COMPLETE, eval)

        val result = coordinator.executeSwipeAction(150f)
        assertEquals(SwipeActionResult.COMPLETE, result)
        assertTrue(completeCalled)
        assertFalse(deleteCalled)
    }

    @Test
    fun testLeftSwipeBeyondThresholdDeletes() {
        var completeCalled = false
        var deleteCalled = false
        val coordinator = SwipeActionCoordinator(
            thresholdPx = 100f,
            onComplete = { completeCalled = true },
            onDelete = { deleteCalled = true }
        )

        val eval = coordinator.evaluateSwipe(-120f)
        assertEquals(SwipeActionResult.DELETE, eval)

        val result = coordinator.executeSwipeAction(-150f)
        assertEquals(SwipeActionResult.DELETE, result)
        assertFalse(completeCalled)
        assertTrue(deleteCalled)
    }

    @Test
    fun testZeroOffsetProducesNone() {
        val coordinator = SwipeActionCoordinator()
        assertEquals(SwipeDirection.NONE, coordinator.determineDirection(0f))
        assertEquals(SwipeActionResult.NONE, coordinator.evaluateSwipe(0f))
    }
}
