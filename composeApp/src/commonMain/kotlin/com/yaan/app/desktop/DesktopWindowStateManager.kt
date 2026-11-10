package com.yaan.app.desktop

/**
 * Interface abstraction for desktop window state persistence.
 */
interface DesktopPreferencesStore {
    fun getInt(key: String, def: Int): Int
    fun putInt(key: String, value: Int)
}

/**
 * Data model for persisted window bounds and screen coordinates.
 */
data class SavedWindowState(
    val widthDp: Int,
    val heightDp: Int,
    val positionXDp: Int? = null,
    val positionYDp: Int? = null
)

/**
 * Manages desktop window size and position persistence with enforced minimum constraints.
 * Satisfies Issue #31:
 * - Enforces minimum bounds: Width >= 380dp, Height >= 600dp.
 * - Defaults: Width = 440dp, Height = 880dp.
 * - Clamps any loaded or saved dimensions to prevent unusable window sizes.
 */
class DesktopWindowStateManager(
    private val store: DesktopPreferencesStore
) {
    companion object {
        const val KEY_WIDTH = "window_width_dp"
        const val KEY_HEIGHT = "window_height_dp"
        const val KEY_POS_X = "window_pos_x_dp"
        const val KEY_POS_Y = "window_pos_y_dp"

        const val MIN_WIDTH_DP = 380
        const val MIN_HEIGHT_DP = 600
        const val DEFAULT_WIDTH_DP = 440
        const val DEFAULT_HEIGHT_DP = 880

        const val NO_POSITION = -999999
    }

    fun loadWindowState(): SavedWindowState {
        val rawWidth = store.getInt(KEY_WIDTH, DEFAULT_WIDTH_DP)
        val rawHeight = store.getInt(KEY_HEIGHT, DEFAULT_HEIGHT_DP)
        val rawX = store.getInt(KEY_POS_X, NO_POSITION)
        val rawY = store.getInt(KEY_POS_Y, NO_POSITION)

        val clampedWidth = maxOf(MIN_WIDTH_DP, rawWidth)
        val clampedHeight = maxOf(MIN_HEIGHT_DP, rawHeight)

        val posX = if (rawX != NO_POSITION) rawX else null
        val posY = if (rawY != NO_POSITION) rawY else null

        return SavedWindowState(
            widthDp = clampedWidth,
            heightDp = clampedHeight,
            positionXDp = posX,
            positionYDp = posY
        )
    }

    fun saveWindowState(
        widthDp: Int,
        heightDp: Int,
        positionXDp: Int? = null,
        positionYDp: Int? = null
    ) {
        val clampedWidth = maxOf(MIN_WIDTH_DP, widthDp)
        val clampedHeight = maxOf(MIN_HEIGHT_DP, heightDp)

        store.putInt(KEY_WIDTH, clampedWidth)
        store.putInt(KEY_HEIGHT, clampedHeight)

        if (positionXDp != null) {
            store.putInt(KEY_POS_X, positionXDp)
        }
        if (positionYDp != null) {
            store.putInt(KEY_POS_Y, positionYDp)
        }
    }
}
