package com.yaan.app.ui.gestures

/**
 * Direction of a horizontal gesture swipe.
 */
enum class SwipeDirection {
    NONE,
    START_TO_END, // Swiping right (affirmative / complete)
    END_TO_START  // Swiping left (destructive / delete)
}

/**
 * Resulting outcome of a completed swipe gesture.
 */
enum class SwipeActionResult {
    NONE,
    COMPLETE,
    DELETE
}

/**
 * Gesture coordinator evaluating displacement offsets, thresholds, and affirmative/destructive actions
 * for mobile touch interactions on tasks and reminders.
 */
class SwipeActionCoordinator(
    val thresholdPx: Float = DEFAULT_THRESHOLD_PX,
    private val onComplete: () -> Unit = {},
    private val onDelete: () -> Unit = {}
) {

    /**
     * Identifies the current drag direction from pixel displacement.
     */
    fun determineDirection(dragOffsetPx: Float): SwipeDirection = when {
        dragOffsetPx > 0f -> SwipeDirection.START_TO_END
        dragOffsetPx < 0f -> SwipeDirection.END_TO_START
        else -> SwipeDirection.NONE
    }

    /**
     * Evaluates whether the current displacement exceeds the action trigger threshold.
     */
    fun evaluateSwipe(dragOffsetPx: Float): SwipeActionResult = when {
        dragOffsetPx >= thresholdPx -> SwipeActionResult.COMPLETE
        dragOffsetPx <= -thresholdPx -> SwipeActionResult.DELETE
        else -> SwipeActionResult.NONE
    }

    /**
     * Executes the appropriate action callback if the threshold was crossed.
     */
    fun executeSwipeAction(dragOffsetPx: Float): SwipeActionResult {
        val result = evaluateSwipe(dragOffsetPx)
        when (result) {
            SwipeActionResult.COMPLETE -> onComplete()
            SwipeActionResult.DELETE -> onDelete()
            SwipeActionResult.NONE -> { /* no-op */ }
        }
        return result
    }

    companion object {
        const val DEFAULT_THRESHOLD_PX = 120f
    }
}
