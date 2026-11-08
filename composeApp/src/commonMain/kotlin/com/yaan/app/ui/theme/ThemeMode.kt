package com.yaan.app.ui.theme

/**
 * Represents the user's explicit theme preference for YAAN.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    /**
     * Resolves whether dark theme styling should be rendered given the current system theme state.
     */
    fun isDark(systemInDarkTheme: Boolean): Boolean = when (this) {
        SYSTEM -> systemInDarkTheme
        LIGHT -> false
        DARK -> true
    }

    /**
     * Cycles to the next theme mode in sequence: SYSTEM -> LIGHT -> DARK -> SYSTEM.
     */
    fun next(): ThemeMode = when (this) {
        SYSTEM -> LIGHT
        LIGHT -> DARK
        DARK -> SYSTEM
    }
}
